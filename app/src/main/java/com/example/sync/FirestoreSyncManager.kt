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

                val currentTier = leagueManager.getCurrentTier(userId)
                val weeklyXp = leagueManager.getWeeklyXp(userId)
                val weekId = leagueManager.getCurrentWeekId()
                val displayName = profile.displayName.ifBlank { firebaseUser.displayName ?: "Học viên Kiki" }
                val avatarUri = profile.avatarUri ?: firebaseUser.photoUrl?.toString() ?: ""
                val email = firebaseUser.email ?: ""

                val userDoc = hashMapOf<String, Any>(
                    "uid" to firebaseUser.uid,
                    "email" to email,
                    "displayName" to displayName,
                    "avatarUri" to avatarUri,
                    "totalXp" to profile.totalXp,
                    "weeklyXp" to weeklyXp,
                    "tier" to currentTier.name,
                    "lastActiveWeek" to weekId,
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

                // 2. Cập nhật Bảng xếp hạng toàn cầu "leaderboard" (đồng bộ 100% dữ liệu)
                firestore.collection("leaderboard")
                    .document(firebaseUser.uid)
                    .set(userDoc, SetOptions.merge())

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

                val cloudWeeklyXp = snap.getLong("weeklyXp")?.toInt() ?: 0
                val cloudTierStr = snap.getString("tier") ?: snap.getString("leagueTier")
                if (!cloudTierStr.isNullOrBlank()) {
                    leagueManager.setCurrentTier(LeagueTier.fromString(cloudTierStr), userId)
                }
                if (cloudWeeklyXp > 0) {
                    leagueManager.setWeeklyXp(cloudWeeklyXp, userId)
                }

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
            val localWeeklyXp = leagueManager.getWeeklyXp(currentUserId)
            val localTier = leagueManager.getCurrentTier(currentUserId)

            // 1. Tải toàn bộ người dùng từ CẢ 2 collection "users" và "leaderboard" trên Firestore
            // Đảm bảo 100% tài khoản đã từng đăng nhập trên Firebase đều sẽ hiển thị trên bảng xếp hạng
            val usersSnap = try {
                firestore.collection("users")
                    .limit(100)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Không thể truy vấn Firestore users: ${e.message}")
                null
            }

            val leaderboardSnap = try {
                firestore.collection("leaderboard")
                    .limit(100)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Không thể truy vấn Firestore leaderboard: ${e.message}")
                null
            }

            // 2. Hợp nhất tất cả các tài khoản theo UID để không sót bất kỳ ai
            data class UnifiedUser(
                val uid: String,
                val name: String,
                val avatar: String,
                val totalXp: Int,
                val weeklyXp: Int,
                val tier: LeagueTier,
                val streak: Int,
                val email: String
            )

            val mergedUsers = mutableMapOf<String, UnifiedUser>()

            fun processDocument(doc: com.google.firebase.firestore.DocumentSnapshot) {
                val uid = doc.getString("uid")?.takeIf { it.isNotBlank() } ?: doc.id
                val rawName = (doc.getString("displayName") ?: doc.getString("name"))?.trim()
                val email = doc.getString("email")?.trim() ?: ""
                val name = rawName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").takeIf { it.isNotBlank() }
                    ?: "Học viên Kiki"
                val avatar = doc.getString("avatarUri")
                    ?: doc.getString("photoUrl")
                    ?: doc.getString("avatarUrl")
                    ?: ""
                val totalXp = (doc.getLong("totalXp") ?: doc.getLong("xp"))?.toInt() ?: 0
                val rawWeeklyXp = doc.getLong("weeklyXp")?.toInt() ?: 0
                val docWeek = doc.getString("lastActiveWeek") ?: ""
                // Nếu cùng tuần thì dùng weeklyXp, nếu tài khoản chưa thi đấu tuần này thì điểm tuần = 0
                val weeklyXp = if (docWeek.isBlank() || docWeek == currentWeek) rawWeeklyXp else 0
                val streak = (doc.getLong("currentStreak") ?: doc.getLong("streak"))?.toInt() ?: 0
                val tierStr = doc.getString("tier") ?: doc.getString("leagueTier")
                val tier = if (!tierStr.isNullOrBlank()) LeagueTier.fromString(tierStr) else LeagueTier.BRONZE

                val existing = mergedUsers[uid]
                if (existing != null) {
                    mergedUsers[uid] = existing.copy(
                        name = if (existing.name == "Học viên Kiki" && name != "Học viên Kiki") name else existing.name,
                        avatar = existing.avatar.ifBlank { avatar },
                        totalXp = maxOf(existing.totalXp, totalXp),
                        weeklyXp = maxOf(existing.weeklyXp, weeklyXp),
                        tier = if (existing.tier == LeagueTier.BRONZE && tier != LeagueTier.BRONZE) tier else existing.tier,
                        streak = maxOf(existing.streak, streak)
                    )
                } else {
                    mergedUsers[uid] = UnifiedUser(
                        uid = uid,
                        name = name,
                        avatar = avatar,
                        totalXp = totalXp,
                        weeklyXp = weeklyXp,
                        tier = tier,
                        streak = streak,
                        email = email
                    )
                }
            }

            usersSnap?.documents?.forEach { processDocument(it) }
            leaderboardSnap?.documents?.forEach { processDocument(it) }

            // 3. Bổ sung thêm các tài khoản đã đăng nhập cục bộ trên thiết bị từ Room SQLite (nếu chưa có trên Firebase)
            try {
                val localProfiles = repository.getLeaderboard()
                for (lp in localProfiles) {
                    val matchingUid = if (lp.userId == currentUserId) currentUid else "local_user_${lp.userId}"
                    if (!mergedUsers.containsKey(matchingUid) && !mergedUsers.values.any { it.email.isNotBlank() && it.email == lp.displayName }) {
                        mergedUsers[matchingUid] = UnifiedUser(
                            uid = matchingUid,
                            name = lp.displayName.ifBlank { "Học viên Kiki" },
                            avatar = lp.avatarUri ?: "",
                            totalXp = lp.totalXp,
                            weeklyXp = if (lp.userId == currentUserId) localWeeklyXp else lp.totalXp,
                            tier = if (lp.userId == currentUserId) localTier else LeagueTier.BRONZE,
                            streak = lp.currentStreak,
                            email = ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi nạp local profiles: ${e.message}")
            }

            // 4. Lọc người chơi theo targetTier và tính điểm thi đấu
            val realPlayersInTier = mutableListOf<RankPlayer>()
            var isLocalUserIncluded = false

            for (user in mergedUsers.values) {
                val isCurrentUser = currentUid.isNotBlank() && user.uid == currentUid

                // Điểm hiển thị: ưu tiên weeklyXp, nếu chưa có weeklyXp thì dùng totalXp để luôn có điểm tranh tài
                val displayScore = if (isCurrentUser) {
                    maxOf(localWeeklyXp, localProfile?.totalXp ?: 0, user.weeklyXp, user.totalXp)
                } else {
                    if (user.weeklyXp > 0) user.weeklyXp else user.totalXp
                }

                val effectiveTier = if (isCurrentUser) localTier else user.tier

                if (isCurrentUser) {
                    isLocalUserIncluded = true
                    if (effectiveTier == targetTier) {
                        realPlayersInTier.add(
                            RankPlayer(
                                rank = 0,
                                name = localProfile?.displayName?.ifBlank { user.name } ?: user.name,
                                title = targetTier.subtitle,
                                score = displayScore,
                                avatarUrl = localProfile?.avatarUri ?: user.avatar,
                                isCurrentUser = true,
                                streak = maxOf(localProfile?.currentStreak ?: 0, user.streak),
                                tier = targetTier,
                                uid = user.uid
                            )
                        )
                    }
                } else if (effectiveTier == targetTier) {
                    realPlayersInTier.add(
                        RankPlayer(
                            rank = 0,
                            name = user.name,
                            title = targetTier.subtitle,
                            score = displayScore,
                            avatarUrl = user.avatar,
                            isCurrentUser = false,
                            streak = user.streak,
                            tier = targetTier,
                            uid = user.uid
                        )
                    )
                }
            }

            // Nếu người dùng hiện tại đang ở targetTier nhưng chưa được thêm vào
            if (!isLocalUserIncluded && localTier == targetTier) {
                val myName = localProfile?.displayName?.ifBlank { "Bạn" } ?: "Bạn"
                realPlayersInTier.add(
                    RankPlayer(
                        rank = 0,
                        name = myName,
                        title = targetTier.subtitle,
                        score = maxOf(localWeeklyXp, localProfile?.totalXp ?: 0),
                        avatarUrl = localProfile?.avatarUri ?: "",
                        isCurrentUser = true,
                        streak = localProfile?.currentStreak ?: 0,
                        tier = targetTier,
                        uid = currentUid
                    )
                )
            }

            // 5. Sắp xếp danh sách người chơi thật theo điểm số giảm dần
            val finalRankedList = realPlayersInTier
                .sortedWith(compareByDescending<RankPlayer> { it.score }.thenBy { it.name })
                .mapIndexed { index, player ->
                    val rank = index + 1
                    val zone = leagueManager.determineZone(rank, targetTier)
                    player.copy(rank = rank, zone = zone)
                }

            // Lưu lại vị trí của user nếu user đang thi đấu ở giải này
            val myRank = finalRankedList.find { it.isCurrentUser }?.rank
            if (myRank != null && localTier == targetTier) {
                leagueManager.recordCurrentRank(myRank, currentUserId)
            }

            finalRankedList
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi fetchRealtimeTierLeaderboard: ${e.message}", e)
            emptyList()
        }
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

