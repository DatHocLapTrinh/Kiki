package com.example.sync

import android.content.Context
import android.util.Log
import com.example.model.LeagueSettlementResult
import com.example.model.LeagueTier
import com.example.model.LeagueZone
import com.example.model.RankPlayer
import com.example.repository.DataRepository
import com.example.security.FirebaseAuthManager
import com.example.sqlite.room.UserProfileEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val repository: DataRepository,
    val leagueManager: LeagueManager,
    @param:ApplicationContext private val appContext: Context
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        private const val TAG = "FirestoreSyncManager"
    }

    /**
     * Đồng bộ ngầm toàn bộ dữ liệu người dùng (Profile, Điểm XP, Streak, Từ vựng, Điểm yếu) lên Firestore
     */
    fun syncAllToCloud(userId: Long) {
        if (userId <= 0) return
        scope.launch {
            try {
                syncUserProfile(userId)
                syncVocabulary(userId)
                syncWeakPoints(userId)
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đồng bộ ngầm lên Firestore: ${e.message}")
            }
        }
    }

    /**
     * Đồng bộ thông tin hồ sơ và điểm XP của user lên Firestore
     */
    fun syncUserProfile(userId: Long) {
        if (userId <= 0) return
        scope.launch {
            try {
                val profile = repository.getUserProfile(userId) ?: return@launch
                val firebaseUser = authManager.getCurrentUser() ?: return@launch

                val prefs = appContext.getSharedPreferences(com.example.audio.SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
                val fcmToken = prefs.getString(com.example.notification.KikiFirebaseMessagingService.PREF_FCM_TOKEN, null)

                val userDoc = hashMapOf<String, Any>(
                    "uid" to firebaseUser.uid,
                    "email" to (firebaseUser.email ?: ""),
                    "displayName" to profile.displayName,
                    "avatarUri" to (profile.avatarUri ?: ""),
                    "totalXp" to profile.totalXp,
                    "level" to ((profile.totalXp / 100) + 1),
                    "mana" to profile.mana,
                    "currentStreak" to profile.currentStreak,
                    "streakShields" to profile.streakShields,
                    "studyMotto" to (profile.studyMotto ?: ""),
                    "lastActiveDate" to (profile.lastActiveDate ?: ""),
                    "updatedAt" to Timestamp.now()
                )

                if (!fcmToken.isNullOrEmpty()) {
                    userDoc["fcmToken"] = fcmToken
                }

                // 1. Lưu vào collection "users"
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(userDoc, SetOptions.merge())

                // 2. Cập nhật Bảng xếp hạng toàn cầu "leaderboard"
                val currentTier = leagueManager.getCurrentTier()
                val weeklyXp = leagueManager.getWeeklyXp()
                val weekId = leagueManager.getCurrentWeekId()

                val leaderboardDoc = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "displayName" to profile.displayName,
                    "avatarUri" to (profile.avatarUri ?: ""),
                    "totalXp" to profile.totalXp,
                    "weeklyXp" to weeklyXp,
                    "tier" to currentTier.name,
                    "lastActiveWeek" to weekId,
                    "currentStreak" to profile.currentStreak,
                    "level" to ((profile.totalXp / 100) + 1),
                    "updatedAt" to Timestamp.now()
                )

                firestore.collection("leaderboard")
                    .document(firebaseUser.uid)
                    .set(leaderboardDoc, SetOptions.merge())

                Log.d(TAG, "Đã đồng bộ hồ sơ user ${firebaseUser.uid} (Tier: ${currentTier.name}, WeeklyXP: $weeklyXp) lên Firestore.")
            } catch (e: Exception) {
                Log.w(TAG, "Không thể đồng bộ hồ sơ lên Firestore: ${e.message}")
            }
        }
    }

    /**
     * Đồng bộ Sổ tay từ vựng lên Firestore
     */
    fun syncVocabulary(userId: Long) {
        if (userId <= 0) return
        scope.launch {
            try {
                val firebaseUser = authManager.getCurrentUser() ?: return@launch
                val vocabList = repository.getVocabularyList(userId)
                if (vocabList.isEmpty()) return@launch

                val batch = firestore.batch()
                val vocabCollection = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .collection("vocabulary")

                for (item in vocabList) {
                    val docRef = vocabCollection.document(item.vocabId.toString())
                    val data = hashMapOf(
                        "vocabId" to item.vocabId,
                        "word" to item.word,
                        "phonetic" to item.phonetic,
                        "meaning" to item.meaning,
                        "example" to item.example,
                        "isMastered" to item.isMastered,
                        "createdAt" to item.createdAt,
                        "updatedAt" to Timestamp.now()
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
                Log.d(TAG, "Đã đồng bộ ${vocabList.size} từ vựng lên Firestore.")
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đồng bộ từ vựng: ${e.message}")
            }
        }
    }

    /**
     * Đồng bộ Kho điểm yếu (câu làm sai) lên Firestore
     */
    fun syncWeakPoints(userId: Long) {
        if (userId <= 0) return
        scope.launch {
            try {
                val firebaseUser = authManager.getCurrentUser() ?: return@launch
                val weakPoints = repository.getWeakPoints(userId)
                if (weakPoints.isEmpty()) return@launch

                val batch = firestore.batch()
                val weakCollection = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .collection("weak_points")

                for (wp in weakPoints) {
                    val docRef = weakCollection.document(wp.weakId.toString())
                    val data = hashMapOf(
                        "weakId" to wp.weakId,
                        "question" to wp.question,
                        "optionsJson" to wp.optionsJson,
                        "correctIndex" to wp.correctIndex,
                        "wrongCount" to wp.wrongCount,
                        "lastFailedAt" to wp.lastFailedAt,
                        "updatedAt" to Timestamp.now()
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
                Log.d(TAG, "Đã đồng bộ ${weakPoints.size} điểm yếu lên Firestore.")
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đồng bộ điểm yếu: ${e.message}")
            }
        }
    }

    /**
     * Khôi phục tiến độ từ Firestore về SQLite khi đăng nhập trên thiết bị mới
     */
    suspend fun restoreProgressFromCloud(userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = authManager.getCurrentUser() ?: return@withContext false
            val snap = firestore.collection("users").document(firebaseUser.uid).get().await()

            if (snap.exists()) {
                val cloudXp = snap.getLong("totalXp")?.toInt() ?: 0
                val cloudStreak = snap.getLong("currentStreak")?.toInt() ?: 0
                val cloudShields = snap.getLong("streakShields")?.toInt() ?: 1
                val cloudMotto = snap.getString("studyMotto") ?: ""
                val cloudName = snap.getString("displayName") ?: firebaseUser.displayName ?: "Adventurer"
                val cloudAvatar = snap.getString("avatarUri") ?: firebaseUser.photoUrl?.toString()

                val localProfile = repository.getUserProfile(userId)
                val localXp = localProfile?.totalXp ?: 0

                // Chỉ ghi đè nếu dữ liệu đám mây mới hơn hoặc có XP cao hơn
                if (cloudXp >= localXp) {
                    repository.updateProfileInfo(userId, cloudName, cloudAvatar, cloudMotto)
                    val xpDiff = cloudXp - localXp
                    if (xpDiff > 0) {
                        repository.updateXP(userId, xpDiff)
                    }
                    repository.updateStreakShields(userId, cloudShields)
                    Log.d(TAG, "Khôi phục dữ liệu từ Cloud thành công: XP=$cloudXp, Streak=$cloudStreak")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Không thể khôi phục từ Cloud: ${e.message}")
        }
        false
    }

    /**
     * Tải danh sách xếp hạng thời gian thực từ Firestore collection "leaderboard",
     * phân loại theo cấp bậc cúp (targetTier), tích hợp người dùng thật và các bạn học trong giải.
     */
    suspend fun fetchRealtimeTierLeaderboard(
        targetTier: LeagueTier,
        currentUserId: Long
    ): List<RankPlayer> = withContext(Dispatchers.IO) {
        try {
            val currentWeek = leagueManager.getCurrentWeekId()
            val currentFirebaseUser = authManager.getCurrentUser()
            val currentUid = currentFirebaseUser?.uid ?: ""
            val localProfile = repository.getUserProfile(currentUserId)
            val localWeeklyXp = leagueManager.getWeeklyXp()
            val localTier = leagueManager.getCurrentTier()

            // 1. Tải danh sách người dùng từ Firestore "leaderboard"
            val querySnapshot = try {
                firestore.collection("leaderboard")
                    .limit(100)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Không thể truy vấn Firestore leaderboard: ${e.message}")
                null
            }

            val realPlayersInTier = mutableListOf<RankPlayer>()
            var isLocalUserIncluded = false

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                for (doc in querySnapshot.documents) {
                    val uid = doc.getString("uid") ?: doc.id
                    val name = doc.getString("displayName") ?: "Học viên Kiki"
                    val avatar = doc.getString("avatarUri") ?: ""
                    val tierStr = doc.getString("tier") ?: LeagueTier.BRONZE.name
                    val docTier = LeagueTier.fromString(tierStr)
                    val docWeek = doc.getString("lastActiveWeek") ?: ""
                    val streak = doc.getLong("currentStreak")?.toInt() ?: 0

                    val rawWeeklyXp = doc.getLong("weeklyXp")?.toInt() ?: 0
                    // Nếu là tuần hiện tại thì dùng weeklyXp, nếu là tuần trước thì điểm tuần = 0
                    val weeklyXp = if (docWeek == currentWeek) rawWeeklyXp else 0

                    val isCurrentUser = (currentUid.isNotEmpty() && uid == currentUid) ||
                            (localProfile != null && name == localProfile.displayName)

                    if (isCurrentUser) {
                        isLocalUserIncluded = true
                        if (localTier == targetTier) {
                            realPlayersInTier.add(
                                RankPlayer(
                                    rank = 0,
                                    name = localProfile?.displayName?.ifBlank { name } ?: name,
                                    title = targetTier.subtitle,
                                    score = localWeeklyXp,
                                    avatarUrl = localProfile?.avatarUri ?: avatar,
                                    isCurrentUser = true,
                                    streak = localProfile?.currentStreak ?: streak,
                                    tier = targetTier,
                                    uid = uid
                                )
                            )
                        }
                    } else if (docTier == targetTier) {
                        realPlayersInTier.add(
                            RankPlayer(
                                rank = 0,
                                name = name,
                                title = targetTier.subtitle,
                                score = weeklyXp,
                                avatarUrl = avatar,
                                isCurrentUser = false,
                                streak = streak,
                                tier = targetTier,
                                uid = uid
                            )
                        )
                    }
                }
            }

            // 2. Nếu người dùng hiện tại đang ở targetTier nhưng chưa có trên Firestore, bổ sung local user
            if (!isLocalUserIncluded && localTier == targetTier) {
                val myName = localProfile?.displayName?.ifBlank { "Bạn" } ?: "Bạn"
                realPlayersInTier.add(
                    RankPlayer(
                        rank = 0,
                        name = myName,
                        title = targetTier.subtitle,
                        score = localWeeklyXp,
                        avatarUrl = localProfile?.avatarUri ?: "",
                        isCurrentUser = true,
                        streak = localProfile?.currentStreak ?: 0,
                        tier = targetTier,
                        uid = currentUid
                    )
                )
            }

            // 3. Đảm bảo phân khu luôn đủ 20 - 25 bạn học để có trải nghiệm Top 5 thăng hạng & rớt hạng trọn vẹn
            val fullDivision = completeDivisionWithPeers(targetTier, realPlayersInTier, currentWeek)

            // 4. Sắp xếp theo điểm tuần giảm dần
            val finalRankedList = fullDivision
                .sortedWith(compareByDescending<RankPlayer> { it.score }.thenBy { it.name })
                .mapIndexed { index, player ->
                    val rank = index + 1
                    val zone = leagueManager.determineZone(rank, targetTier)
                    player.copy(rank = rank, zone = zone)
                }

            // Lưu lại vị trí của user nếu user đang thi đấu ở giải này
            val myRank = finalRankedList.find { it.isCurrentUser }?.rank
            if (myRank != null && localTier == targetTier) {
                leagueManager.recordCurrentRank(myRank)
            }

            finalRankedList
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi fetchRealtimeTierLeaderboard: ${e.message}", e)
            emptyList()
        }
    }

    private fun completeDivisionWithPeers(
        tier: LeagueTier,
        existingList: List<RankPlayer>,
        weekId: String
    ): List<RankPlayer> {
        val result = existingList.toMutableList()
        val existingNames = existingList.map { it.name }.toSet()

        val peerBank = listOf(
            Pair("Minh Thư", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80"),
            Pair("Hải Đăng", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150&auto=format&fit=crop&q=80"),
            Pair("Khánh Vy", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80"),
            Pair("Gia Huy", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80"),
            Pair("Bảo Long", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80"),
            Pair("Thùy Trang", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80"),
            Pair("Quang Dũng", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150&auto=format&fit=crop&q=80"),
            Pair("Ngọc Ánh", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop&q=80"),
            Pair("Tuấn Kiệt", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&auto=format&fit=crop&q=80"),
            Pair("Phương Linh", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80"),
            Pair("Thành Nam", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop&q=80"),
            Pair("Lan Hương", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150&auto=format&fit=crop&q=80"),
            Pair("Đức Trí", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150&auto=format&fit=crop&q=80"),
            Pair("Yến Nhi", "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=150&auto=format&fit=crop&q=80"),
            Pair("Hoàng Phúc", "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=150&auto=format&fit=crop&q=80"),
            Pair("Mỹ Duyên", "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=150&auto=format&fit=crop&q=80"),
            Pair("Việt Hoàng", "https://images.unsplash.com/photo-1513956589380-bad6acb9b9d4?w=150&auto=format&fit=crop&q=80"),
            Pair("Kim Ngân", "https://images.unsplash.com/photo-1534751516642-a171edd27150?w=150&auto=format&fit=crop&q=80"),
            Pair("Nhật Minh", "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=80"),
            Pair("Thu Hà", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80"),
            Pair("Trọng Hiếu", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150&auto=format&fit=crop&q=80"),
            Pair("Mai Anh", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80")
        )

        val (baseXp, stepXp) = when (tier) {
            LeagueTier.BRONZE -> 220 to 10
            LeagueTier.SILVER -> 440 to 18
            LeagueTier.GOLD -> 780 to 32
            LeagueTier.PLATINUM -> 1350 to 55
            LeagueTier.DIAMOND -> 2400 to 95
        }

        val weekSeed = (weekId.hashCode() and 0x7FFFFFFF) % 100

        var peerIndex = 0
        while (result.size < 22 && peerIndex < peerBank.size) {
            val peer = peerBank[peerIndex]
            if (!existingNames.contains(peer.first)) {
                val variance = ((peer.first.hashCode() + weekSeed) and 0x7FFFFFFF) % (stepXp + 1)
                val calculatedScore = maxOf(15, baseXp - (peerIndex * stepXp) + variance)
                result.add(
                    RankPlayer(
                        rank = 0,
                        name = peer.first,
                        title = tier.subtitle,
                        score = calculatedScore,
                        avatarUrl = peer.second,
                        isCurrentUser = false,
                        streak = maxOf(1, (peerIndex * 3 + 2) % 20),
                        tier = tier,
                        uid = "peer_$peerIndex"
                    )
                )
            }
            peerIndex++
        }

        return result
    }


    /**
     * Cập nhật FCM registration token của thiết bị người dùng lên Firestore
     */
    fun updateFcmToken(token: String) {
        val user = authManager.getCurrentUser() ?: return
        scope.launch {
            try {
                firestore.collection("users")
                    .document(user.uid)
                    .set(
                        mapOf(
                            "fcmToken" to token,
                            "fcmUpdatedAt" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                Log.d(TAG, "Đã đồng bộ FCM Token mới lên Firestore cho user ${user.uid}")
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đồng bộ FCM token: ${e.message}")
            }
        }
    }
}

