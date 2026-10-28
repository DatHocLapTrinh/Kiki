package com.example.repository

import android.content.Context
import com.example.model.ChapterGeneratedResponse
import com.example.model.MatchingPair
import com.example.model.QAItem
import com.example.model.QuestItem
import com.example.model.QuestionType
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

    val vocabularyPairsBank = listOf(
        // Căn bản cho người mới bắt đầu (Beginner / Zero-knowledge A0-A1)
        MatchingPair(1, "hello", "xin chào"),
        MatchingPair(2, "thank you", "cảm ơn"),
        MatchingPair(3, "goodbye", "tạm biệt"),
        MatchingPair(4, "please", "làm ơn / xin vui lòng"),
        MatchingPair(5, "sorry", "xin lỗi"),
        MatchingPair(6, "friend", "bạn bè"),
        MatchingPair(7, "water", "nước"),
        MatchingPair(8, "coffee", "cà phê"),
        MatchingPair(9, "morning", "buổi sáng"),
        MatchingPair(10, "name", "tên"),
        MatchingPair(11, "yes", "vâng / có"),
        MatchingPair(12, "no", "không"),
        // Thông dụng trung cấp & nâng cao
        MatchingPair(13, "give up", "từ bỏ"),
        MatchingPair(14, "turn off", "tắt (thiết bị)"),
        MatchingPair(15, "look after", "chăm sóc"),
        MatchingPair(16, "call off", "hủy bỏ"),
        MatchingPair(17, "run into", "tình cờ gặp"),
        MatchingPair(18, "carry out", "tiến hành"),
        MatchingPair(19, "break down", "hỏng hóc"),
        MatchingPair(20, "come across", "bắt gặp"),
        MatchingPair(21, "look forward to", "trông đợi"),
        MatchingPair(22, "figure out", "tìm ra / hiểu ra"),
        MatchingPair(23, "put off", "trì hoãn"),
        MatchingPair(24, "get along", "hòa thuận"),
        MatchingPair(25, "fluency", "sự lưu loát"),
        MatchingPair(26, "enthusiastic", "nhiệt tình"),
        MatchingPair(27, "essential", "thiết yếu"),
        MatchingPair(28, "brilliant", "tài giỏi"),
        MatchingPair(29, "opportunity", "cơ hội"),
        MatchingPair(30, "challenge", "thử thách"),
        MatchingPair(31, "confidence", "sự tự tin"),
        MatchingPair(32, "perseverance", "sự kiên trì"),
        MatchingPair(33, "accurate", "chính xác"),
        MatchingPair(34, "efficient", "hiệu quả"),
        MatchingPair(35, "creative", "sáng tạo"),
        MatchingPair(36, "inspiration", "nguồn cảm hứng"),
        MatchingPair(37, "determination", "lòng quyết tâm")
    )

    suspend fun ensureSeeded() {
        seedJob.join()
    }

    private suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        try {
            // 1. Purge legacy sample chapters and questions from old questions.json (e.g. English Fundamentals)
            val existing = dao.getActiveChapters()
            val hasLegacySample = existing.any {
                it.contains("English Fundamentals", ignoreCase = true) ||
                it.contains("Intermediate Conversations", ignoreCase = true)
            }
            if (hasLegacySample) {
                dao.clearQuestions()
                dao.clearChapters()
            }

            val remainingChapters = dao.getActiveChapters()
            if (remainingChapters.isEmpty()) {
                // Initialize default scientific stage shells with ZERO static questions
                val defaultStages = listOf(
                    Pair("Chặng 1: Chào Hỏi & Làm Quen Cơ Bản (Hello & Daily Greetings)", "Bắt đầu từ số 0 với các từ chào hỏi kinh điển Hello, Hi, Thank you, My name is."),
                    Pair("Chặng 2: Giới Thiệu Bản Thân & Hỏi Thăm (Personal Identity & How Are You)", "Tự tin giới thiệu bản thân và hỏi thăm bạn bè: How are you, I am fine."),
                    Pair("Chặng 3: Nhu Cầu & Đồ Vật Thiết Yếu (Everyday Essentials & Requests)", "Giao tiếp cơ bản khi xin phép, cảm ơn và gọi nước, đồ ăn hàng ngày."),
                    Pair("Chặng 4: Số Đếm, Thời Gian & Mua Sắm Cơ Bản (Numbers, Time & Basic Requests)", "Làm quen số đếm 1-10, hỏi giờ và hỏi giá đơn giản."),
                    Pair("Chặng 5: Đàm Thoại Ngắn Phản Xạ Đời Sống (Short Daily Dialogue & Mastery)", "Phản xạ giao tiếp tự nhiên với các hội thoại ngắn đời thường 2-3 câu.")
                )
                defaultStages.forEachIndexed { cIndex, (chapterTitle, chapterDesc) ->
                    val chapter = ChapterEntity(
                        title = chapterTitle,
                        description = chapterDesc,
                        level = "Beginner",
                        subject = "English",
                        orderNo = cIndex,
                        isActive = 1
                    )
                    dao.insertChapter(chapter)
                }
                // ABSOLUTELY ZERO static questions are seeded! All question slots are dynamically generated by Gemini 2.5 Flash on demand!
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

    suspend fun getChapterTitleById(chapterId: Long): String? = withContext(Dispatchers.IO) {
        ensureSeeded()
        dao.getChapterTitleById(chapterId)
    }

    suspend fun clearQuestionsByChapter(chapterId: Long): Int = withContext(Dispatchers.IO) {
        dao.clearQuestionsByChapter(chapterId)
    }

    suspend fun clearAllQuestions() = withContext(Dispatchers.IO) {
        dao.clearQuestions()
    }

    suspend fun getQuestionsByChapter(chapterId: Long): List<QuestItem> = withContext(Dispatchers.IO) {
        ensureSeeded()
        val questions = dao.getQuestionsByChapter(chapterId)
        val validQuestions = questions.filterNot {
            it.questionText.contains("______") ||
            it.questionText.contains("in her crib", ignoreCase = true) ||
            it.questionText.contains("by bus every morning", ignoreCase = true)
        }
        if (validQuestions.isEmpty()) return@withContext emptyList()

        val hasExplicitTypes = validQuestions.any {
            it.questionType.equals("SENTENCE_BUILDER", ignoreCase = true) ||
            it.questionType.equals("LISTENING_CHALLENGE", ignoreCase = true) ||
            it.questionType.equals("LISTENING", ignoreCase = true) ||
            it.questionType.equals("MATCHING_PAIRS", ignoreCase = true) ||
            it.questionType.equals("SPEAKING_CHALLENGE", ignoreCase = true) ||
            it.questionType.equals("SPEAKING", ignoreCase = true)
        }

        validQuestions.mapIndexedNotNull { index, q ->
            try {
                val array = JSONArray(q.optionsJson)
                val options = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    options.add(array.getString(i))
                }
                val correctIdx = q.correctAnswer.toIntOrNull() ?: 0
                val hasBlanks = q.questionText.contains(Regex("_{2,}"))

                // Determine intended question archetype:
                // If the chapter has explicitly authored types (e.g. from AI 3C generator), respect them.
                // If not (e.g. legacy questions.json), dynamically interleave across the 5 Golden Multimodal Archetypes!
                val effectiveType = if (hasExplicitTypes) {
                    when {
                        q.questionType.equals("SPEAKING_CHALLENGE", ignoreCase = true) || q.questionType.equals("SPEAKING", ignoreCase = true) -> QuestionType.SPEAKING_CHALLENGE
                        q.questionType.equals("MATCHING_PAIRS", ignoreCase = true) -> QuestionType.MATCHING_PAIRS
                        q.questionType.equals("LISTENING_CHALLENGE", ignoreCase = true) || q.questionType.equals("LISTENING", ignoreCase = true) -> QuestionType.LISTENING_CHALLENGE
                        q.questionType.equals("SENTENCE_BUILDER", ignoreCase = true) -> QuestionType.SENTENCE_BUILDER
                        else -> QuestionType.MULTIPLE_CHOICE
                    }
                } else {
                    // The Golden 5-Step Multimodal Cycle:
                    // 0: Multiple Choice / Cloze (Visual & Semantic recognition)
                    // 1: Sentence Builder (Active syntax reconstruction)
                    // 2: Listening Challenge (Phonological decoding)
                    // 3: Matching Pairs (Speed association & dopamine booster)
                    // 4: Speaking Challenge (Oral output & pronunciation mastery)
                    when (index % 5) {
                        0 -> QuestionType.MULTIPLE_CHOICE
                        1 -> QuestionType.SENTENCE_BUILDER
                        2 -> QuestionType.LISTENING_CHALLENGE
                        3 -> QuestionType.MATCHING_PAIRS
                        4 -> QuestionType.SPEAKING_CHALLENGE
                        else -> QuestionType.MULTIPLE_CHOICE
                    }
                }

                // Helper: Reconstruct complete sentence from prompt + correct option
                val correctWord = options.getOrNull(correctIdx) ?: ""
                val completeSentence = when {
                    q.correctAnswer.contains(" ") -> q.correctAnswer.trim()
                    options.size == 1 && options[0].contains(" ") -> options[0].trim()
                    hasBlanks -> q.questionText.replace(Regex("_{2,}"), correctWord).replace(Regex("\\s+"), " ").trim()
                    options.size >= 3 && !options.any { it.contains(" ") } -> options.joinToString(" ")
                    else -> (options.firstOrNull() ?: q.questionText).trim()
                }

                when (effectiveType) {
                    QuestionType.SPEAKING_CHALLENGE -> {
                        val targetSpeakSentence = completeSentence.ifEmpty { q.questionText }
                        if (targetSpeakSentence.isNotBlank()) {
                            QuestItem(
                                question = "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
                                options = listOf(targetSpeakSentence),
                                correctIndex = 0,
                                type = QuestionType.SPEAKING_CHALLENGE,
                                speakingSentence = targetSpeakSentence
                            )
                        } else {
                            QuestItem(
                                question = q.questionText,
                                options = options,
                                correctIndex = correctIdx,
                                type = QuestionType.MULTIPLE_CHOICE
                            )
                        }
                    }

                    QuestionType.MATCHING_PAIRS -> {
                        val hasKeyVal = options.any { it.contains("=") }
                        val selectedPairs = if (hasKeyVal) {
                            options.mapIndexedNotNull { optIdx, opt ->
                                val parts = opt.split("=", limit = 2)
                                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                    MatchingPair(optIdx + 1, parts[0].trim(), parts[1].trim())
                                } else null
                            }.takeIf { it.size >= 2 } ?: vocabularyPairsBank.shuffled().take(4)
                        } else {
                            vocabularyPairsBank.shuffled().take(4)
                        }
                        val pairSummary = selectedPairs.joinToString(", ") { "${it.english} = ${it.vietnamese}" }
                        QuestItem(
                            question = "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng 🧩",
                            options = listOf(pairSummary),
                            correctIndex = 0,
                            type = QuestionType.MATCHING_PAIRS,
                            matchingPairs = selectedPairs
                        )
                    }

                    QuestionType.LISTENING_CHALLENGE -> {
                        val sentenceToListen = completeSentence.ifEmpty { q.questionText }
                        val rawTokens = sentenceToListen.split(Regex("\\s+"))
                            .map { it.trim().trim('.', ',', '!', '?', ';', ':', '"') }
                            .filter { it.isNotEmpty() }

                        if (rawTokens.size >= 2) {
                            QuestItem(
                                question = "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
                                options = options,
                                correctIndex = correctIdx,
                                type = QuestionType.LISTENING_CHALLENGE,
                                sentenceTokens = rawTokens.shuffled(),
                                correctSentence = sentenceToListen,
                                speakingSentence = sentenceToListen
                            )
                        } else {
                            QuestItem(
                                question = q.questionText,
                                options = options,
                                correctIndex = correctIdx,
                                type = QuestionType.MULTIPLE_CHOICE
                            )
                        }
                    }

                    QuestionType.SENTENCE_BUILDER -> {
                        val promptText = when {
                            hasBlanks -> "Sắp xếp các từ sau thành câu hoàn chỉnh 🧩"
                            q.questionText.startsWith("Sắp xếp", ignoreCase = true) -> q.questionText
                            q.questionText.startsWith("Dịch", ignoreCase = true) -> q.questionText
                            else -> "Sắp xếp các từ sau thành câu hoàn chỉnh 🧩"
                        }

                        val availableTokens = if (options.size >= 4 && !options.any { it.contains(" ") } && !hasBlanks) {
                            options.shuffled()
                        } else {
                            val rawTokens = completeSentence.split(Regex("\\s+"))
                                .map { it.trim().trim('.', ',', '!', '?', ';', ':', '"') }
                                .filter { it.isNotEmpty() }

                            val distractorOptions = options.filterIndexed { i, _ -> i != correctIdx }
                            val distractors = mutableListOf<String>()
                            for (d in distractorOptions) {
                                val cleanD = d.trim().trim('.', ',', '!', '?', ';', ':')
                                if (cleanD.isNotEmpty() && !rawTokens.any { it.equals(cleanD, ignoreCase = true) }) {
                                    distractors.add(cleanD)
                                    if (distractors.size >= 2) break
                                }
                            }
                            (rawTokens + distractors).shuffled()
                        }

                        if (availableTokens.size >= 2) {
                            QuestItem(
                                question = promptText,
                                options = options,
                                correctIndex = correctIdx,
                                type = QuestionType.SENTENCE_BUILDER,
                                sentenceTokens = availableTokens,
                                correctSentence = completeSentence
                            )
                        } else {
                            QuestItem(
                                question = q.questionText,
                                options = options,
                                correctIndex = correctIdx,
                                type = QuestionType.MULTIPLE_CHOICE
                            )
                        }
                    }

                    else -> {
                        // QuestionType.MULTIPLE_CHOICE (Câu khởi động: Điền khuyết / Trắc nghiệm)
                        QuestItem(
                            question = q.questionText,
                            options = options,
                            correctIndex = correctIdx,
                            type = QuestionType.MULTIPLE_CHOICE
                        )
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveAiGeneratedChapter(
        response: ChapterGeneratedResponse,
        isPriorityFirst: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val currentMaxOrder = dao.getMaxChapterOrderNo() ?: 0
        val targetOrderNo = if (isPriorityFirst) 0 else (response.orderNo ?: (currentMaxOrder + 1)).coerceAtLeast(currentMaxOrder + 1)
        val chapter = ChapterEntity(
            title = response.title.trim(),
            description = response.description,
            level = response.level ?: "Beginner",
            subject = response.subject ?: "English",
            orderNo = targetOrderNo,
            isActive = 1
        )
        val chapterId = dao.insertChapter(chapter)
        if (chapterId != -1L) {
            response.questions.forEach { qDto ->
                val question = QuestionEntity(
                    chapterId = chapterId,
                    questionText = qDto.questionText,
                    questionType = qDto.questionType ?: "MULTIPLE_CHOICE",
                    optionsJson = qDto.optionsJson,
                    correctAnswer = qDto.correctAnswer,
                    explanation = qDto.explanation,
                    points = qDto.points,
                    difficulty = qDto.difficulty ?: "MEDIUM"
                )
                dao.insertQuestion(question)
            }
        }
        chapterId
    }

    suspend fun resetWithAiMicrolearningRoadmap(
        level: String,
        stages: List<Pair<String, String>>,
        firstChapterUnits: ChapterGeneratedResponse?
    ): Long = withContext(Dispatchers.IO) {
        dao.clearQuestions()
        dao.clearChapters()

        var firstChapterId = -1L
        stages.forEachIndexed { index, (title, description) ->
            val chapter = ChapterEntity(
                title = title.trim(),
                description = description,
                level = level,
                subject = "English",
                orderNo = index,
                isActive = 1
            )
            val chapterId = dao.insertChapter(chapter)
            if (index == 0) {
                firstChapterId = chapterId
                firstChapterUnits?.questions?.forEach { qDto ->
                    val question = QuestionEntity(
                        chapterId = chapterId,
                        questionText = qDto.questionText,
                        questionType = qDto.questionType ?: "MULTIPLE_CHOICE",
                        optionsJson = qDto.optionsJson,
                        correctAnswer = qDto.correctAnswer,
                        explanation = qDto.explanation,
                        points = qDto.points,
                        difficulty = qDto.difficulty ?: "MEDIUM"
                    )
                    dao.insertQuestion(question)
                }
            }
        }
        firstChapterId
    }

    suspend fun insertQuestion(question: QuestionEntity): Long = withContext(Dispatchers.IO) {
        dao.insertQuestion(question)
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

    suspend fun getOrCreateSocialUser(
        email: String,
        displayName: String?,
        photoUrl: String?
    ): Long = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        var userId = dao.getUserIdByEmail(normalizedEmail) ?: -1L
        if (userId == -1L) {
            val user = UserEntity(
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash("social_auth_${System.currentTimeMillis()}"),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            )
            userId = dao.insertUser(user)
            if (userId != -1L) {
                val profile = UserProfileEntity(
                    userId = userId,
                    displayName = displayName?.takeIf { it.isNotBlank() } ?: "Kiki Scholar",
                    avatarUri = photoUrl,
                    studyMotto = "Chinh phục tri thức tiếng Anh cùng Kiki!"
                )
                dao.insertUserProfile(profile)
            }
        } else {
            val existing = dao.getUserProfile(userId)
            if (existing != null) {
                val updatedName = displayName?.takeIf { it.isNotBlank() } ?: existing.displayName
                val updatedAvatar = photoUrl?.takeIf { it.isNotBlank() } ?: existing.avatarUri
                dao.updateProfileInfo(userId, updatedName, updatedAvatar, existing.studyMotto)
            }
        }
        userId
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

    suspend fun processVocabularySm2Review(
        vocabId: Long,
        quality: Int,
        currentEntity: VocabularyEntity
    ): com.example.util.Sm2State = withContext(Dispatchers.IO) {
        val currentState = com.example.util.Sm2State(
            repetition = currentEntity.repetitionLevel,
            intervalDays = currentEntity.intervalDays,
            easinessFactor = currentEntity.easinessFactor,
            nextReviewDate = currentEntity.nextReviewDate,
            isMastered = currentEntity.isMastered
        )
        val newState = com.example.util.Sm2Algorithm.calculateNextReview(currentState, quality)
        dao.updateVocabularySm2(
            vocabId = vocabId,
            repetition = newState.repetition,
            interval = newState.intervalDays,
            easiness = newState.easinessFactor,
            nextReview = newState.nextReviewDate,
            isMastered = newState.isMastered
        )
        newState
    }

    suspend fun getDueVocabularyList(userId: Long): List<VocabularyEntity> = withContext(Dispatchers.IO) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        dao.getDueVocabularyList(userId, todayStr)
    }

    suspend fun deleteVocabulary(vocabId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.deleteVocabulary(vocabId) > 0
    }

    suspend fun isWordBookmarked(userId: Long, word: String): Boolean = withContext(Dispatchers.IO) {
        dao.isWordBookmarked(userId, word) > 0
    }

    suspend fun updateStreakShields(userId: Long, shields: Int): Boolean = withContext(Dispatchers.IO) {
        dao.updateStreakShields(userId, shields) > 0
    }

    suspend fun recordWeakPoint(userId: Long, question: String, options: List<String>, correctIndex: Int): Boolean = withContext(Dispatchers.IO) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val existing = dao.getWeakPointByQuestion(userId, question)
        if (existing != null) {
            dao.incrementWeakPointCount(existing.weakId, now) > 0
        } else {
            val jsonArr = org.json.JSONArray(options)
            val wp = WeakPointEntity(
                userId = userId,
                question = question,
                optionsJson = jsonArr.toString(),
                correctIndex = correctIndex,
                wrongCount = 1,
                lastFailedAt = now
            )
            dao.insertWeakPoint(wp) > 0
        }
    }

    suspend fun getWeakPoints(userId: Long): List<WeakPointEntity> = withContext(Dispatchers.IO) {
        dao.getWeakPoints(userId)
    }

    suspend fun deleteWeakPoint(weakId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.deleteWeakPoint(weakId) > 0
    }

    suspend fun clearWeakPoints(userId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.clearWeakPoints(userId) > 0
    }

    suspend fun getMasteredVocabularyCount(userId: Long): Int = withContext(Dispatchers.IO) {
        dao.getMasteredVocabularyCount(userId)
    }

    suspend fun getUnlockedBadgeIds(userId: Long): Set<String> = withContext(Dispatchers.IO) {
        val json = dao.getBadgesJson(userId)
        if (json.isNullOrBlank()) {
            emptySet()
        } else {
            try {
                val arr = org.json.JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }
    }

    suspend fun unlockBadge(userId: Long, badgeId: String): Boolean = withContext(Dispatchers.IO) {
        val currentBadges = getUnlockedBadgeIds(userId).toMutableSet()
        if (currentBadges.add(badgeId)) {
            val jsonArray = org.json.JSONArray(currentBadges)
            dao.updateBadgesJson(userId, jsonArray.toString()) > 0
        } else {
            false
        }
    }

    // --- Research Telemetry Logging (Scientific Spaced Repetition) ---
    suspend fun saveTelemetry(entry: ResearchTelemetryEntity): Long = withContext(Dispatchers.IO) {
        dao.insertTelemetry(entry)
    }

    suspend fun saveTelemetryBatch(entries: List<ResearchTelemetryEntity>): List<Long> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) emptyList() else dao.insertTelemetryList(entries)
    }

    suspend fun getAllTelemetry(userId: Long): List<ResearchTelemetryEntity> = withContext(Dispatchers.IO) {
        dao.getAllTelemetry(userId)
    }

    suspend fun getTelemetryCount(userId: Long): Int = withContext(Dispatchers.IO) {
        dao.getTelemetryCount(userId)
    }

    suspend fun clearTelemetry(userId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.clearTelemetry(userId) > 0
    }
}



