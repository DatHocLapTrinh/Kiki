package com.example.repository

import android.content.Context
import com.example.model.QAItem
import com.example.model.QuestItem
import com.example.security.PasswordHasher
import com.example.sqlite.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val dao: AppDao,
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val seedJob = scope.launch {
        seedInitialData()
    }

    suspend fun ensureSeeded() {
        seedJob.join()
    }

    private suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getActiveChapters()
            val hasEnglish = existing.any { it.contains("English", ignoreCase = true) || it.contains("Vocabulary", ignoreCase = true) }
            if (!hasEnglish || existing.isEmpty()) {
                dao.clearQuestions()
                dao.clearChapters()

                val jsonString = context.assets.open("questions.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val levels = arrayOf("Beginner", "Intermediate", "Advanced", "Middle School", "High School", "University / College")

                for (level in levels) {
                    val subjectKeys = root.keys()
                    while (subjectKeys.hasNext()) {
                        val subject = subjectKeys.next()
                        val chaptersObj = root.getJSONObject(subject)
                        var cIndex = 1

                        val chapterKeys = chaptersObj.keys()
                        while (chapterKeys.hasNext()) {
                            val chapterTitle = chapterKeys.next()
                            val questionsArr = chaptersObj.getJSONArray(chapterTitle)

                            val chapter = ChapterEntity(
                                title = chapterTitle,
                                level = level,
                                subject = subject,
                                orderNo = cIndex++,
                                isActive = 1
                            )
                            val chapterId = dao.insertChapter(chapter)

                            for (i in 0 until questionsArr.length()) {
                                val qObj = questionsArr.getJSONObject(i)
                                val qText = qObj.getString("question")
                                val optionsJson = qObj.getJSONArray("options").toString()
                                val correctIdx = qObj.getString("correctAnswer")

                                val question = QuestionEntity(
                                    chapterId = chapterId,
                                    questionText = qText,
                                    optionsJson = optionsJson,
                                    correctAnswer = correctIdx
                                )
                                dao.insertQuestion(question)
                            }
                        }
                    }
                }
            }

            val lb = dao.getLeaderboard()
            if (lb.size < 10) {
                val names = arrayOf("Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jamie", "Skyler", "Cameron", "Quinn")
                val xps = intArrayOf(15200, 14500, 13100, 12500, 11000, 9500, 8000, 7500, 6000, 5000)
                for (i in names.indices) {
                    val dummy = UserEntity(
                        email = "dummy$i@test.com",
                        passwordHash = PasswordHasher.hash("kikiDummyPass123!")
                    )
                    val id = dao.insertUser(dummy)
                    if (id != -1L) {
                        val profile = UserProfileEntity(
                            userId = id,
                            displayName = names[i],
                            totalXp = xps[i]
                        )
                        dao.insertUserProfile(profile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getChaptersByPreference(level: String?, subject: String?): List<String> = withContext(Dispatchers.IO) {
        ensureSeeded()
        val targetSubject = if (subject.isNullOrBlank() || subject != "English") "English" else subject
        val targetLevel = level ?: "Beginner"
        val results = dao.getChaptersByPreference(targetLevel, targetSubject)
        if (results.isNotEmpty()) {
            results.distinct()
        } else {
            dao.getActiveChapters().distinct()
        }
    }

    suspend fun getChapterIdByTitle(title: String): Long = withContext(Dispatchers.IO) {
        ensureSeeded()
        dao.getChapterIdByTitle(title) ?: -1L
    }

    suspend fun getQuestionsByChapter(chapterId: Long): List<QuestItem> = withContext(Dispatchers.IO) {
        val questions = dao.getQuestionsByChapter(chapterId)
        questions.mapNotNull { q ->
            try {
                val array = JSONArray(q.optionsJson)
                val options = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    options.add(array.getString(i))
                }
                QuestItem(
                    question = q.questionText,
                    options = options,
                    correctIndex = q.correctAnswer.toIntOrNull() ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun registerUser(name: String, email: String, rawPassword: String): Long = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val existingId = dao.getUserIdByEmail(normalizedEmail)
        if (existingId != null) return@withContext -2L

        val hashedPassword = PasswordHasher.hash(rawPassword)
        val user = UserEntity(
            email = normalizedEmail,
            passwordHash = hashedPassword,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )
        val userId = dao.insertUser(user)

        if (userId != -1L) {
            val profile = UserProfileEntity(
                userId = userId,
                displayName = name
            )
            dao.insertUserProfile(profile)
        }
        userId
    }

    suspend fun login(email: String, rawPassword: String): UserEntity? = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val user = dao.getUserByEmail(normalizedEmail) ?: return@withContext null

        if (PasswordHasher.verify(rawPassword, user.passwordHash)) {
            return@withContext user
        }

        // Tương thích ngược: nếu tài khoản cũ chưa băm mật khẩu
        if (user.passwordHash == rawPassword) {
            // Tự động nâng cấp sang mã băm bảo mật
            user.passwordHash = PasswordHasher.hash(rawPassword)
            dao.insertUser(user)
            return@withContext user
        }

        null
    }

    suspend fun getUserIdByEmail(email: String): Long = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        dao.getUserIdByEmail(normalizedEmail) ?: -1L
    }

    suspend fun getUserProfile(userId: Long): UserProfileEntity? = withContext(Dispatchers.IO) {
        dao.getUserProfile(userId)
    }

    suspend fun getActiveChapters(): List<String> = withContext(Dispatchers.IO) {
        ensureSeeded()
        dao.getActiveChapters()
    }

    suspend fun saveAIQuestion(userId: Long, question: String, imageUri: String?, aiAnswer: String) = withContext(Dispatchers.IO) {
        val ai = AIQuestionEntity(
            userId = userId,
            questionText = question,
            imageUri = imageUri,
            aiAnswer = aiAnswer,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )
        dao.insertAIQuestion(ai)
    }

    suspend fun saveQuizAttempt(userId: Long, chapterId: Long, total: Int, correct: Int) = withContext(Dispatchers.IO) {
        val attempt = QuizAttemptEntity(
            userId = userId,
            chapterId = chapterId,
            totalQuestions = total,
            correctAnswers = correct,
            score = if (total > 0) (correct * 100) / total else 0,
            status = "COMPLETED",
            completedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )
        dao.insertQuizAttempt(attempt)
    }

    suspend fun getAIHistory(userId: Long): List<QAItem> = withContext(Dispatchers.IO) {
        val list = dao.getAIHistory(userId)
        list.map { ai ->
            QAItem(
                id = ai.aiQuestionId.toInt(),
                question = ai.questionText,
                imageUri = ai.imageUri,
                answer = ai.aiAnswer,
                timestamp = 0
            )
        }
    }

    suspend fun deleteAIQuestion(aiQuestionId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.deleteAIQuestion(aiQuestionId) > 0
    }

    suspend fun clearAIHistory(userId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.clearAIHistory(userId) > 0
    }

    suspend fun updateXP(userId: Long, xpGain: Int) = withContext(Dispatchers.IO) {
        dao.updateXP(userId, xpGain)
    }

    suspend fun updateMana(userId: Long, manaGain: Int) = withContext(Dispatchers.IO) {
        dao.updateMana(userId, manaGain)
    }

    suspend fun getOrCreateDailyTasks(userId: Long, taskDate: String): List<DailyTaskEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getDailyTasks(userId, taskDate)
        if (existing.isNotEmpty()) return@withContext existing

        insertDailyTask(userId, taskDate, "LESSON_COMPLETE", 1, 50)
        insertDailyTask(userId, taskDate, "PERFECT_SCORE", 1, 100)
        insertDailyTask(userId, taskDate, "ANSWER_QUESTIONS", 10, 50)
        dao.getDailyTasks(userId, taskDate)
    }

    private suspend fun insertDailyTask(userId: Long, taskDate: String, taskType: String, targetValue: Int, rewardXp: Int) {
        val task = DailyTaskEntity(
            userId = userId,
            taskDate = taskDate,
            taskType = taskType,
            title = taskType,
            targetValue = targetValue,
            currentValue = 0,
            rewardXp = rewardXp,
            status = "IN_PROGRESS"
        )
        dao.insertDailyTask(task)
    }

    suspend fun updateDailyTaskProgress(userId: Long, taskDate: String, taskType: String, increment: Int) = withContext(Dispatchers.IO) {
        val tasks = dao.getDailyTasks(userId, taskDate)
        val target = tasks.firstOrNull { it.taskType == taskType }
        if (target != null) {
            dao.updateTaskProgress(target.taskId, increment)
        }
    }

    suspend fun getDailyTask(taskId: Long): DailyTaskEntity? = withContext(Dispatchers.IO) {
        dao.getDailyTask(taskId)
    }

    suspend fun claimDailyTask(taskId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.claimDailyTask(taskId) > 0
    }

    private fun chestKey(userId: Long, taskDate: String) = "chest_${userId}_$taskDate"

    fun isDailyChestClaimed(userId: Long, taskDate: String): Boolean {
        return context.getSharedPreferences("StudyMentorDailyState", Context.MODE_PRIVATE)
            .getBoolean(chestKey(userId, taskDate), false)
    }

    @Synchronized
    fun claimDailyChest(userId: Long, taskDate: String): Boolean {
        val prefs = context.getSharedPreferences("StudyMentorDailyState", Context.MODE_PRIVATE)
        val key = chestKey(userId, taskDate)
        if (prefs.getBoolean(key, false)) return false
        return prefs.edit().putBoolean(key, true).commit()
    }

    private fun mapNodeKey(userId: Long, chapterTitle: String) = "node_${userId}_$chapterTitle"

    fun getCurrentLessonNode(userId: Long, chapterTitle: String): Int {
        return context.getSharedPreferences("StudyMentorMapProgress", Context.MODE_PRIVATE)
            .getInt(mapNodeKey(userId, chapterTitle), 1)
    }

    fun saveCurrentLessonNode(userId: Long, chapterTitle: String, node: Int) {
        val safeNode = node.coerceIn(1, 6)
        context.getSharedPreferences("StudyMentorMapProgress", Context.MODE_PRIVATE)
            .edit()
            .putInt(mapNodeKey(userId, chapterTitle), safeNode)
            .apply()
    }

    suspend fun getLeaderboard(): List<UserProfileEntity> = withContext(Dispatchers.IO) {
        dao.getLeaderboard()
    }

    suspend fun updateStreak(userId: Long, streak: Int, lastActiveDate: String): Boolean = withContext(Dispatchers.IO) {
        dao.updateStreak(userId, streak, lastActiveDate) > 0
    }

    suspend fun updateProfileInfo(userId: Long, displayName: String, avatarUri: String?, motto: String?): Boolean = withContext(Dispatchers.IO) {
        dao.updateProfileInfo(userId, displayName, avatarUri, motto) > 0
    }

    suspend fun insertVocabulary(userId: Long, word: String, phonetic: String, meaning: String, example: String): Long = withContext(Dispatchers.IO) {
        val vocab = VocabularyEntity(
            userId = userId,
            word = word,
            phonetic = phonetic,
            meaning = meaning,
            example = example,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        dao.insertVocabulary(vocab)
    }

    suspend fun getVocabularyList(userId: Long): List<VocabularyEntity> = withContext(Dispatchers.IO) {
        dao.getVocabularyList(userId)
    }

    suspend fun toggleVocabularyMastered(vocabId: Long, isMastered: Boolean): Boolean = withContext(Dispatchers.IO) {
        dao.updateVocabularyMastered(vocabId, isMastered) > 0
    }

    suspend fun deleteVocabulary(vocabId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.deleteVocabulary(vocabId) > 0
    }

    suspend fun isWordBookmarked(userId: Long, word: String): Boolean = withContext(Dispatchers.IO) {
        dao.isWordBookmarked(userId, word) > 0
    }
}

