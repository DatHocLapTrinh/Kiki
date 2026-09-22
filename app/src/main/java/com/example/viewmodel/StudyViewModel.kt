package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.model.GroqModels
import com.example.model.QAItem
import com.example.model.QuestItem
import com.example.network.GroqApiService
import com.example.repository.DataRepository
import com.example.sqlite.room.DailyTaskEntity
import com.example.sqlite.room.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StudyViewModel @Inject constructor(
    val repository: DataRepository,
    private val groqApiService: GroqApiService,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("StudyMentorAiSettings", Context.MODE_PRIVATE)

    // --- State ---
    private val _history = MutableLiveData<List<QAItem>>(emptyList())
    val history: LiveData<List<QAItem>> = _history

    private val _isEnglish = MutableLiveData(false)
    val isEnglish: LiveData<Boolean> = _isEnglish
    fun setEnglish(isEnglish: Boolean) { _isEnglish.value = isEnglish }

    private val _batterySaver = MutableLiveData(prefs.getBoolean("battery_saver_mode", false))
    val batterySaver: LiveData<Boolean> = _batterySaver
    fun setBatterySaver(enabled: Boolean) {
        prefs.edit().putBoolean("battery_saver_mode", enabled).apply()
        _batterySaver.value = enabled
    }

    private val _userApiKey = MutableLiveData(prefs.getString("groq_custom_api_key", "") ?: "")
    val userApiKey: LiveData<String> = _userApiKey
    fun setCustomApiKey(key: String) {
        prefs.edit().putString("groq_custom_api_key", key.trim()).apply()
        _userApiKey.value = key.trim()
    }

    fun getEffectiveApiKey(): String {
        val customKey = _userApiKey.value?.trim()
        if (!customKey.isNullOrBlank()) return customKey
        // Fallback to BuildConfig if available
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "your_api_key_here") buildKey else ""
        } catch (e: Exception) {
            ""
        }
    }

    private val _userName = MutableLiveData("Adventurer")
    val userName: LiveData<String> = _userName

    private val _currentUserId = MutableLiveData(-1L)
    val currentUserId: LiveData<Long> = _currentUserId

    private val _isGenerating = MutableLiveData(false)
    val isGenerating: LiveData<Boolean> get() = _isGenerating
    @JvmName("getIsGeneratingFunction")
    fun isGenerating(): LiveData<Boolean> = _isGenerating

    private val _xp = MutableLiveData(0)
    val xp: LiveData<Int> = _xp

    private val _level = MutableLiveData(1)
    val level: LiveData<Int> = _level

    private val _rankTitle = MutableLiveData("Bronze Novice")
    val rankTitle: LiveData<String> = _rankTitle

    fun calculateRankTitle(xp: Int): String {
        return when {
            xp >= 15000 -> "Grandmaster"
            xp >= 13000 -> "Crystal Knight"
            xp >= 11000 -> "Gold Wizard"
            xp >= 9000 -> "Silver Apprentice"
            else -> "Bronze Novice"
        }
    }

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak
    fun setStreak(streak: Int) { _streak.value = streak }

    private val _mana = MutableLiveData(20)
    val mana: LiveData<Int> = _mana
    fun setMana(mana: Int) { _mana.value = mana }

    private val _currentNode = MutableLiveData(1)
    val currentNode: LiveData<Int> = _currentNode
    fun setCurrentNode(node: Int) { _currentNode.value = node }

    private val _leaderboard = MutableLiveData<List<UserProfileEntity>>(emptyList())
    val leaderboard: LiveData<List<UserProfileEntity>> = _leaderboard

    private val _dailyTasks = MutableLiveData<List<DailyTaskEntity>>(emptyList())
    val dailyTasks: LiveData<List<DailyTaskEntity>> = _dailyTasks

    private val _dailyChestOpened = MutableLiveData(false)
    val isDailyChestOpened: LiveData<Boolean> get() = _dailyChestOpened
    @JvmName("getIsDailyChestOpenedFunction")
    fun isDailyChestOpened(): LiveData<Boolean> = _dailyChestOpened

    fun fetchLeaderboard() {
        viewModelScope.launch {
            val lb = repository.getLeaderboard()
            _leaderboard.value = lb
        }
    }

    private val _selectedLevel = MutableLiveData("Beginner")
    val selectedLevel: LiveData<String> = _selectedLevel

    private val _selectedSubject = MutableLiveData("English")
    val selectedSubject: LiveData<String> = _selectedSubject

    fun setPreferences(level: String, subject: String) {
        _selectedLevel.value = level
        _selectedSubject.value = if (subject.isBlank() || subject != "English") "English" else subject
    }

    private val _availableChapters = MutableLiveData<List<String>>(emptyList())
    val availableChapters: LiveData<List<String>> = _availableChapters

    private val _chapterProgress = MutableLiveData<List<Int>>(emptyList())
    val chapterProgress: LiveData<List<Int>> = _chapterProgress

    private val _chaptersLoading = MutableLiveData(false)
    val chaptersLoading: LiveData<Boolean> = _chaptersLoading

    private val _chaptersError = MutableLiveData<String?>(null)
    val chaptersError: LiveData<String?> = _chaptersError

    fun refreshChapters() {
        _chaptersLoading.value = true
        _chaptersError.value = null
        viewModelScope.launch {
            try {
                val data = repository.getChaptersByPreference(_selectedLevel.value, _selectedSubject.value)
                _availableChapters.value = data

                val userId = _currentUserId.value
                val progress = data.map { title ->
                    if (userId == null || userId == -1L) 1
                    else repository.getCurrentLessonNode(userId, title)
                }
                _chapterProgress.value = progress
            } catch (e: Exception) {
                _chaptersError.value = "Không thể tải bản đồ học. Vui lòng thử lại."
            } finally {
                _chaptersLoading.value = false
            }
        }
    }

    private val _currentChapterTitle = MutableLiveData("English Fundamentals")
    val currentChapterTitle: LiveData<String> = _currentChapterTitle

    private val _currentChapterIndex = MutableLiveData(1)
    val currentChapterIndex: LiveData<Int> = _currentChapterIndex

    fun selectChapterByTitle(title: String, index: Int) {
        viewModelScope.launch {
            val id = repository.getChapterIdByTitle(title)
            _currentChapterTitle.value = title
            _currentChapterIndex.value = index
            val userId = _currentUserId.value
            val savedNode = if (userId == null || userId == -1L) 1 else repository.getCurrentLessonNode(userId, title)
            _currentNode.value = savedNode
            if (id != -1L) loadQuestions(id, savedNode)
        }
    }

    private val _questions = MutableLiveData<List<QuestItem>>(emptyList())
    val questions: LiveData<List<QuestItem>> = _questions

    private val _lastQuestResults = MutableLiveData<List<QuestItem>>(emptyList())
    val lastQuestResults: LiveData<List<QuestItem>> = _lastQuestResults

    private var allChapterQuestions: List<QuestItem> = emptyList()

    fun loadQuestions(chapterId: Long, lessonIndex: Int = 1) {
        viewModelScope.launch {
            allChapterQuestions = repository.getQuestionsByChapter(chapterId)
            startLesson(lessonIndex)
        }
    }

    fun startLesson(lessonIndex: Int) {
        val startIndex = (lessonIndex - 1) * 10
        val endIndex = (startIndex + 10).coerceAtMost(allChapterQuestions.size)
        if (startIndex in 0 until allChapterQuestions.size) {
            _questions.value = allChapterQuestions.subList(startIndex, endIndex)
        } else {
            _questions.value = emptyList()
        }
    }

    fun completeCurrentLesson() {
        val userId = _currentUserId.value ?: return
        val chapterTitle = _currentChapterTitle.value ?: return
        val node = _currentNode.value ?: return
        if (userId == -1L) return

        val nextNode = (node + 1).coerceAtMost(6)
        _currentNode.value = nextNode
        repository.saveCurrentLessonNode(userId, chapterTitle, nextNode)
        refreshChapters()
    }

    private val _questAnalysis = MutableLiveData<String?>(null)
    val questAnalysis: LiveData<String?> = _questAnalysis

    fun analyzeQuestResults(context: Context, results: List<QuestItem>) {
        _lastQuestResults.value = results
        _questAnalysis.value = null

        val correctCount = results.count { it.selectedIndex == it.correctIndex }
        if (correctCount > 0) {
            addXp(correctCount * 20)
        }
        val totalCount = results.size

        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                val chapterId = repository.getChapterIdByTitle(_currentChapterTitle.value ?: "")
                if (chapterId != -1L) {
                    repository.saveQuizAttempt(userId, chapterId, totalCount, correctCount)
                }
                recordDailyTaskProgress(userId, "LESSON_COMPLETE", 1)
                recordDailyTaskProgress(userId, "ANSWER_QUESTIONS", results.size)
                if (results.isNotEmpty() && correctCount == results.size) {
                    recordDailyTaskProgress(userId, "PERFECT_SCORE", 1)
                }
                refreshDailyTasks()
            }

            val prompt = buildString {
                append("Here are the student's exercise results:\n")
                for (item in results) {
                    val selected = item.options.getOrNull(item.selectedIndex) ?: "Skipped"
                    val correct = item.options.getOrNull(item.correctIndex) ?: ""
                    append("Question: ").append(item.question)
                        .append("\nStudent chose: ").append(selected)
                        .append("\nCorrect answer: ").append(correct).append("\n")
                }
                append("\nAct as an AI tutor, analyze weaknesses based on incorrect answers, explain briefly, and propose a study plan. DO NOT use markdown format, asterisks, hashes, or any special formatting symbols. Use plain text only.")
            }

            val analysis = fetchAnswerFromGroq(context, prompt, null)
            _questAnalysis.value = analysis
        }
    }

    fun onLoginSuccess(email: String) {
        viewModelScope.launch {
            val userId = repository.getUserIdByEmail(email)
            if (userId != -1L) {
                _currentUserId.value = userId
                _history.value = emptyList()
                val profile = repository.getUserProfile(userId)
                if (profile != null) {
                    _userName.value = profile.displayName
                    _xp.value = profile.totalXp
                    _level.value = (profile.totalXp / 100) + 1
                    _rankTitle.value = calculateRankTitle(profile.totalXp)
                    _mana.value = profile.mana
                    _streak.value = profile.currentStreak
                }
                fetchLeaderboard()
                refreshDailyTasks()
            }
        }
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun refreshDailyTasks() {
        val userId = _currentUserId.value
        if (userId == null || userId == -1L) {
            _dailyTasks.value = emptyList()
            _dailyChestOpened.value = false
            return
        }

        viewModelScope.launch {
            val date = today()
            val tasks = repository.getOrCreateDailyTasks(userId, date)
            _dailyTasks.value = tasks
            _dailyChestOpened.value = repository.isDailyChestClaimed(userId, date)
        }
    }

    private suspend fun recordDailyTaskProgress(userId: Long, taskType: String, increment: Int) {
        val date = today()
        repository.getOrCreateDailyTasks(userId, date)
        repository.updateDailyTaskProgress(userId, date, taskType, increment)
    }

    fun claimDailyTask(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getDailyTask(taskId)
            if (task != null && repository.claimDailyTask(taskId)) {
                addXp(task.rewardXp)
                refreshDailyTasks()
            }
        }
    }

    fun claimDailyChest() {
        val userId = _currentUserId.value
        if (userId == null || userId == -1L) return

        viewModelScope.launch {
            val date = today()
            val tasks = repository.getOrCreateDailyTasks(userId, date)
            val allClaimed = tasks.isNotEmpty() && tasks.all { it.status == "CLAIMED" }
            if (allClaimed && repository.claimDailyChest(userId, date)) {
                addXp(500)
                _dailyChestOpened.value = true
                refreshDailyTasks()
            }
        }
    }

    fun refreshHistory(userId: Long) {
        viewModelScope.launch {
            val data = repository.getAIHistory(userId)
            _history.value = data
        }
    }

    fun askQuestion(context: Context, question: String, imageUri: Uri?, onSuccess: (() -> Unit)? = null) {
        if (question.isBlank() && imageUri == null) return
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                val answer = fetchAnswerFromGroq(context, question, imageUri)
                val userId = _currentUserId.value
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, question, imageUri?.toString(), answer)
                    val data = repository.getAIHistory(userId)
                    _history.value = data
                    repository.updateXP(userId, 20)
                    val newXp = (_xp.value ?: 0) + 20
                    _xp.value = newXp
                    _level.value = (newXp / 100) + 1
                    _rankTitle.value = calculateRankTitle(newXp)
                } else {
                    val current = _history.value ?: emptyList()
                    val updated = mutableListOf(QAItem(0, question, imageUri?.toString(), answer))
                    updated.addAll(current)
                    _history.value = updated
                }
            } finally {
                _isGenerating.value = false
                onSuccess?.invoke()
            }
        }
    }

    fun askFollowUpQuestion(context: Context, currentQuestion: String, followUpType: String) {
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                var prompt = ""
                var title = ""
                when (followUpType) {
                    "EXPLAIN" -> {
                        prompt = "Based on the question: '$currentQuestion'. Please explain it in more detail, step by step. Use '[STEP] ' prefix for each step. Reply in the same language as the question."
                        title = "Giải thích thêm: $currentQuestion"
                    }
                    "SIMILAR" -> {
                        prompt = "Based on the question: '$currentQuestion'. Give me a similar practice exercise with a hint. Use '[STEP] ' prefix for each step. Reply in the same language as the question."
                        title = "Bài tập tương tự: $currentQuestion"
                    }
                }
                val answer = fetchAnswerFromGroq(context, prompt, null)
                val userId = _currentUserId.value
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, title, null, answer)
                    val data = repository.getAIHistory(userId)
                    _history.value = data
                    repository.updateXP(userId, 10)
                } else {
                    val current = _history.value ?: emptyList()
                    val updated = mutableListOf(QAItem(0, title, null, answer))
                    updated.addAll(current)
                    _history.value = updated
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deepAnalysis(context: Context, question: String) {
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                val prompt = "Perform a deep academic analysis of this topic: '$question'. Cover: 1) Core concept, 2) Why it works (theory), 3) Common mistakes, 4) Real-world applications. Use '[STEP] ' prefix for each section. Reply in the same language as the question."
                val title = "[Phân tích sâu] $question"
                val answer = fetchAnswerFromGroq(context, prompt, null)
                val userId = _currentUserId.value
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, title, null, answer)
                    val data = repository.getAIHistory(userId)
                    _history.value = data
                } else {
                    val current = _history.value ?: emptyList()
                    val updated = mutableListOf(QAItem(0, title, null, answer))
                    updated.addAll(current)
                    _history.value = updated
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private val _quizQuestions = MutableLiveData<List<String>>(emptyList())
    val quizQuestions: LiveData<List<String>> = _quizQuestions

    private val _quizLoading = MutableLiveData(false)
    val isQuizLoading: LiveData<Boolean> get() = _quizLoading
    @JvmName("getIsQuizLoadingFunction")
    fun isQuizLoading(): LiveData<Boolean> = _quizLoading

    fun generateQuickQuiz(context: Context, question: String) {
        _quizLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Create exactly 3 multiple choice questions to test understanding of: '$question'.\n" +
                        "Format each question EXACTLY like this (no extra text):\n" +
                        "Q: [question text]\n" +
                        "A: [option A]\n" +
                        "B: [option B]\n" +
                        "C: [option C]\n" +
                        "D: [option D]\n" +
                        "ANS: [correct letter A/B/C/D]\n" +
                        "---\n" +
                        "Reply in the same language as the topic."
                val raw = fetchAnswerFromGroq(context, prompt, null)
                val blocks = raw.split("---").map { it.trim() }.filter { it.isNotEmpty() }
                _quizQuestions.value = blocks
            } finally {
                _quizLoading.value = false
            }
        }
    }

    fun addXp(amount: Int) {
        val newXp = (_xp.value ?: 0) + amount
        _xp.value = newXp
        _level.value = (newXp / 100) + 1
        _rankTitle.value = calculateRankTitle(newXp)

        val userId = _currentUserId.value
        if (userId != null && userId != -1L) {
            viewModelScope.launch {
                repository.updateXP(userId, amount)
                fetchLeaderboard()
            }
        }
    }

    fun logout() {
        _currentUserId.value = -1L
        _userName.value = "Adventurer"
        _history.value = emptyList()
        _dailyTasks.value = emptyList()
        _dailyChestOpened.value = false
    }

    private suspend fun fetchAnswerFromGroq(context: Context, prompt: String, uri: Uri?): String = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveApiKey()
            if (apiKey.isBlank()) {
                return@withContext "⚠️ Bạn chưa cài đặt Groq API Key.\n\nVui lòng mở biểu tượng Menu (Cài đặt) và dán API Key của Groq để kích hoạt gia sư AI."
            }

            val actualPrompt = if (prompt.isBlank()) "Hãy phân tích chi tiết và giải thích bài tập trong hình ảnh này từng bước một." else prompt

            val messages = mutableListOf<GroqModels.ChatMessage>()
            messages.add(
                GroqModels.createTextMessage(
                    role = "system",
                    text = "You are Kiki, an elite AI English Learning Mentor and Socratic Language Coach. " +
                           "Guide the student to master English grammar, vocabulary, pronunciation nuances, and natural conversational phrasing. " +
                           "Use concise, encouraging, and clear explanations. Use '[STEP] ' prefix when explaining complex rules or sentence breakdowns. " +
                           "Provide bilingual Vietnamese-English explanations when helpful for clarity."
                )
            )

            val base64Image = if (uri != null) uriToBase64(context, uri) else null

            val (modelToUse, userMessage) = if (base64Image != null) {
                GroqModels.MODEL_VISION to GroqModels.createVisionMessage("user", actualPrompt, base64Image)
            } else {
                GroqModels.MODEL_TEXT to GroqModels.createTextMessage("user", actualPrompt)
            }

            messages.add(userMessage)

            val request = GroqModels.ChatRequest(
                model = modelToUse,
                messages = messages,
                temperature = 0.7,
                maxTokens = 1024
            )

            val response = groqApiService.generateChatCompletion("Bearer $apiKey", request)
            if (response.isSuccessful) {
                val body = response.body()
                val choice = body?.choices?.firstOrNull()
                if (choice != null) {
                    return@withContext choice.message.content
                }
            }

            val errorDetail = response.errorBody()?.string() ?: "Code ${response.code()}"
            "Lỗi kết nối Groq API (${response.code()}): $errorDetail"
        } catch (e: Exception) {
            e.printStackTrace()
            "Lỗi hệ thống/mạng: ${e.message}"
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
