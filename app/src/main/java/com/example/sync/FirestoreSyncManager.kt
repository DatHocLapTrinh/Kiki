package com.example.sync

import android.content.Context
import android.util.Log
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
                val leaderboardDoc = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "displayName" to profile.displayName,
                    "avatarUri" to (profile.avatarUri ?: ""),
                    "totalXp" to profile.totalXp,
                    "level" to ((profile.totalXp / 100) + 1),
                    "updatedAt" to Timestamp.now()
                )

                firestore.collection("leaderboard")
                    .document(firebaseUser.uid)
                    .set(leaderboardDoc, SetOptions.merge())

                Log.d(TAG, "Đã đồng bộ hồ sơ user ${firebaseUser.uid} lên Firestore thành công.")
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

