package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.json.JSONArray
import com.example.model.AchievementBadge
import com.example.model.AchievementRegistry
import com.example.model.BadgeId
import com.example.model.GeminiModels
import com.example.model.GroqModels
import com.example.model.QAItem
import com.example.model.QuestItem
import com.example.model.QuestionType
import com.example.network.GeminiApiService
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.audio.TextToSpeechManager
import com.example.audio.SoundEffectManager
import com.example.audio.SpeechRecognitionManager
import com.example.model.GenerateChapterRequest
import com.example.network.KikiBackendApiService
import com.example.security.FirebaseAuthManager
import com.example.security.GoogleAuthResult
import com.example.sync.FirestoreSyncManager
import com.example.sqlite.room.VocabularyEntity
import com.example.sqlite.room.WeakPointEntity
import com.example.model.ChapterUnlockEvent
import com.example.model.ChapterGeneratedResponse
import com.example.model.QuestionItemDto
import com.example.model.MentorChatRequest
import com.example.model.VocabularyCardDto
import com.example.sqlite.room.ResearchTelemetryEntity
import com.example.util.Sm2Algorithm
import com.example.util.Sm2State
import com.example.util.ResearchTelemetryExporter
import java.util.UUID
import kotlinx.coroutines.delay
import javax.inject.Inject
import com.example.model.LearnerPersonaProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@HiltViewModel
class StudyViewModel @Inject constructor(
    val repository: DataRepository,
    private val groqApiService: GroqApiService,
    private val geminiApiService: GeminiApiService,
    private val backendApiService: KikiBackendApiService,
    val ttsManager: TextToSpeechManager,
    val soundEffectManager: SoundEffectManager,
    val speechRecognitionManager: SpeechRecognitionManager,
    val authManager: FirebaseAuthManager,
    val firestoreSyncManager: FirestoreSyncManager,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)

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

    // --- Universal Multi-Provider AI Architecture ---
    companion object {
        const val PROVIDER_AUTO = "AUTO"
        const val PROVIDER_GEMINI = "GEMINI"
        const val PROVIDER_GROQ = "GROQ"
        const val PROVIDER_CLAUDE = "CLAUDE"
        const val PROVIDER_OPENAI = "OPENAI"
        const val DEFAULT_GEMINI_MODEL = GeminiModels.MODEL_GEMINI_2_5_FLASH

        fun detectAiProvider(key: String): String {
            val clean = key.trim()
            return when {
                clean.startsWith("AQ.") || clean.startsWith("AIza") -> PROVIDER_GEMINI
                clean.startsWith("gsk_") -> PROVIDER_GROQ
                clean.startsWith("sk-ant-") -> PROVIDER_CLAUDE
                clean.startsWith("sk-") -> PROVIDER_OPENAI
                else -> PROVIDER_GEMINI
            }
        }

        fun getProviderDisplayName(provider: String): String {
            return when (provider) {
                PROVIDER_GEMINI -> "🧠 Gemini 2.5 Flash Native Dialog"
                PROVIDER_GROQ -> "⚡ Groq (Llama 3.3 70B)"
                PROVIDER_CLAUDE -> "🎭 Claude 3.5 Sonnet"
                PROVIDER_OPENAI -> "🤖 OpenAI GPT-4o"
                PROVIDER_AUTO -> "🧠 Gemini 2.5 Flash (Tự Động)"
                else -> "🧠 Gemini 2.5 Flash Mentor"
            }
        }

        const val AI_MENTOR_SYSTEM_PROMPT =
            "You are Kiki, an elite AI English Mentor. Your response MUST be concise, direct, and delivered in a SINGLE answer without any conversational introductions, greetings, preamble, or outro (do NOT say 'Chào bạn', 'Dưới đây là...', 'Here is the breakdown...'). Never use markdown tables (| col |). Never use '[STEP]' prefixes.\n\n" +
            "When explaining any English word, vocabulary, idiom, or phrase, you MUST strictly follow this exact structured format:\n\n" +
            "[Word / Phrase] /IPA Phonetics/ (Part of speech)\n" +
            "• Nghĩa: [Vietnamese meaning, concise and precise]\n" +
            "• Ví dụ: [Natural English sentence] ([Vietnamese translation])\n" +
            "• Từ đồng nghĩa: [3-5 synonyms separated by commas]\n" +
            "• Từ trái nghĩa: [2-4 antonyms separated by commas]\n\n" +
            "If the question is about grammar, provide a concise explanation with formula, 2 bilingual examples, and a quick tip using clean • bullet points. Keep the entire response within 1 single concise message."

        fun sanitizeAiMentorReply(raw: String): String {
            if (raw.isBlank()) return ""
            var cleaned = raw
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                .replace(Regex("\\*(.+?)\\*"), "$1")
                .replace(Regex("(?m)^#{1,6}\\s*"), "")
                .replace("[STEP]", "")
                .replace(Regex("(?m)^\\s*[-=_]{3,}\\s*$"), "")

            val lines = cleaned.lines().mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    ""
                } else if (trimmed.matches(Regex("^\\|[-| :]+\\|$"))) {
                    null
                } else if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                    val cells = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (cells.isNotEmpty() && !cells.any { it.contains("English Sentence", ignoreCase = true) || it.contains("Translation", ignoreCase = true) }) {
                        "• " + cells.joinToString(" - ")
                    } else {
                        null
                    }
                } else {
                    line
                }
            }
            cleaned = lines.joinToString("\n")
            cleaned = cleaned.replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
            cleaned = cleaned.replace(
                Regex("(?i)^(chào bạn[!,.]?|hello[!,.]?|hi there[!,.]?|sure[!,.]?|dưới đây là[^\n]*\n?|here is[^\n]*\n?|sau đây là[^\n]*\n?|tất nhiên rồi[^\n]*\n?)\\s*"),
                ""
            )
            cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")
            return cleaned.trim()
        }
    }

    private val genericHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _universalAiKey = MutableLiveData(
        prefs.getString("gemini_custom_api_key", "")
            ?.trim()?.takeIf { !it.startsWith("sk-") && !it.startsWith("gsk_") }
            ?: prefs.getString("kiki_universal_ai_api_key", "")
            ?.trim()?.takeIf { !it.startsWith("sk-") && !it.startsWith("gsk_") }
            ?: ""
    )
    val universalAiKey: LiveData<String> = _universalAiKey

    fun setUniversalAiApiKey(key: String) {
        val clean = key.trim()
        prefs.edit()
            .putString("kiki_universal_ai_api_key", clean)
            .putString("groq_custom_api_key", clean)
            .putString("gemini_custom_api_key", clean)
            .apply()
        _universalAiKey.value = clean
        _userApiKey.value = clean
        _geminiApiKey.value = clean
    }

    private val _userApiKey = MutableLiveData(prefs.getString("groq_custom_api_key", "") ?: "")
    val userApiKey: LiveData<String> = _userApiKey
    fun setCustomApiKey(key: String) {
        setUniversalAiApiKey(key)
    }

    private val _geminiApiKey = MutableLiveData(
        prefs.getString("gemini_custom_api_key", "")
            ?.trim()?.takeIf { !it.startsWith("sk-") && !it.startsWith("gsk_") }
            ?: ""
    )
    val geminiApiKey: LiveData<String> = _geminiApiKey
    fun setGeminiApiKey(key: String) {
        setUniversalAiApiKey(key)
    }

    private val _aiProvider = MutableLiveData(PROVIDER_GEMINI)
    val aiProvider: LiveData<String> = _aiProvider
    fun setAiProvider(provider: String) {
        prefs.edit().putString("ai_provider", provider).apply()
        _aiProvider.value = provider
    }

    private val _geminiModel = MutableLiveData(prefs.getString("gemini_selected_model", DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL)
    val geminiModel: LiveData<String> = _geminiModel
    fun setGeminiModel(model: String) {
        prefs.edit().putString("gemini_selected_model", model).apply()
        _geminiModel.value = model
    }

    private val _audioDialogAutoTts = MutableLiveData(prefs.getBoolean("audio_dialog_auto_tts", true))
    val audioDialogAutoTts: LiveData<Boolean> = _audioDialogAutoTts
    fun setAudioDialogAutoTts(enabled: Boolean) {
        prefs.edit().putBoolean("audio_dialog_auto_tts", enabled).apply()
        _audioDialogAutoTts.value = enabled
    }

    private val aiQueryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun getEffectiveGeminiApiKey(): String {
        // 1. Luôn ưu tiên tuyệt đối biến môi trường GEMINI_API_KEY từ file .env / BuildConfig
        val envGemini = try {
            val f = BuildConfig::class.java.getField("GEMINI_API_KEY")
            f.get(null) as? String ?: ""
        } catch (_: Exception) { "" }
        if (envGemini.isNotBlank() && !envGemini.contains("your_") && !envGemini.contains("here")) {
            return envGemini.trim()
        }

        // 2. Custom key người dùng dán (chỉ chấp nhận key Gemini hợp lệ, bỏ qua key OpenAI sk- hoặc Groq gsk_)
        val customGemini = _geminiApiKey.value?.trim()
        if (!customGemini.isNullOrBlank() && !customGemini.startsWith("sk-") && !customGemini.startsWith("gsk_")) {
            return customGemini
        }

        val custom = _universalAiKey.value?.trim()
        if (!custom.isNullOrBlank() && !custom.startsWith("sk-") && !custom.startsWith("gsk_")) {
            return custom
        }

        // 3. Fallback sang biến AI_API_KEY nếu không phải OpenAI/Groq
        val envUniversal = try {
            val f = BuildConfig::class.java.getField("AI_API_KEY")
            f.get(null) as? String ?: ""
        } catch (_: Exception) { "" }
        if (envUniversal.isNotBlank() && !envUniversal.contains("your_") && !envUniversal.contains("here") && !envUniversal.startsWith("sk-") && !envUniversal.startsWith("gsk_")) {
            return envUniversal.trim()
        }

        return ""
    }

    fun getEffectiveUniversalApiKey(): String {
        return getEffectiveGeminiApiKey()
    }

    fun getEffectiveActiveProvider(): String {
        return PROVIDER_GEMINI
    }

    val activeProviderDisplayTitle: LiveData<String> = MediatorLiveData<String>().apply {
        val update = {
            value = getProviderDisplayName(PROVIDER_GEMINI)
        }
        addSource(_aiProvider) { update() }
        addSource(_universalAiKey) { update() }
        addSource(_userApiKey) { update() }
        addSource(_geminiApiKey) { update() }
        update()
    }

    fun getEffectiveApiKey(): String {
        return getEffectiveGeminiApiKey()
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

    private val _streakShields = MutableLiveData(1)
    val streakShields: LiveData<Int> = _streakShields

    private val _shieldUsedNotice = MutableLiveData(false)
    val shieldUsedNotice: LiveData<Boolean> = _shieldUsedNotice
    fun dismissShieldNotice() { _shieldUsedNotice.value = false }

    private val _weakPoints = MutableLiveData<List<WeakPointEntity>>(emptyList())
    val weakPoints: LiveData<List<WeakPointEntity>> = _weakPoints

    private val _avatarUri = MutableLiveData<String?>(null)
    val avatarUri: LiveData<String?> = _avatarUri

    private val _studyMotto = MutableLiveData<String>("")
    val studyMotto: LiveData<String> = _studyMotto

    private val _vocabularyList = MutableLiveData<List<VocabularyEntity>>(emptyList())
    val vocabularyList: LiveData<List<VocabularyEntity>> = _vocabularyList

    private val _mana = MutableLiveData(20)
    val mana: LiveData<Int> = _mana
    fun setMana(mana: Int) { _mana.value = mana }

    private val _unlockedBadgeIds = MutableLiveData<Set<String>>(emptySet())
    val unlockedBadgeIds: LiveData<Set<String>> = _unlockedBadgeIds

    private val _curedWeakPointsCount = MutableLiveData(0)
    val curedWeakPointsCount: LiveData<Int> = _curedWeakPointsCount

    private val _masteredVocabCount = MutableLiveData(0)
    val masteredVocabCount: LiveData<Int> = _masteredVocabCount

    private val _newlyUnlockedBadge = MutableLiveData<AchievementBadge?>(null)
    val newlyUnlockedBadge: LiveData<AchievementBadge?> = _newlyUnlockedBadge
    fun dismissNewlyUnlockedBadge() { _newlyUnlockedBadge.value = null }

    fun refreshBadges(userId: Long = _currentUserId.value ?: -1L) {
        if (userId == -1L) return
        viewModelScope.launch {
            val badges = repository.getUnlockedBadgeIds(userId)
            _unlockedBadgeIds.value = badges
            val cured = prefs.getInt("cured_weak_points_count_$userId", 0)
            _curedWeakPointsCount.value = cured
            val mastered = repository.getMasteredVocabularyCount(userId)
            _masteredVocabCount.value = mastered
        }
    }

    fun checkAndUnlockBadge(badgeId: BadgeId) {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) return
        viewModelScope.launch {
            val current = _unlockedBadgeIds.value ?: emptySet()
            if (!current.contains(badgeId.id)) {
                val success = repository.unlockBadge(userId, badgeId.id)
                if (success) {
                    val updated = current + badgeId.id
                    _unlockedBadgeIds.value = updated
                    firestoreSyncManager.syncUserProfile(userId)
                    val all = AchievementRegistry.getAllBadges(
                        unlockedIds = updated,
                        streak = _streak.value ?: 0,
                        curedWeakPoints = _curedWeakPointsCount.value ?: 0,
                        masteredVocabCount = _masteredVocabCount.value ?: 0
                    )
                    val b = all.firstOrNull { it.id == badgeId.id }
                    if (b != null) {
                        _newlyUnlockedBadge.value = b
                    }
                }
            }
        }
    }

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

    private val _learnerPersonaProfile = MutableLiveData<LearnerPersonaProfile?>(null)
    val learnerPersonaProfile: LiveData<LearnerPersonaProfile?> = _learnerPersonaProfile

    fun saveLearnerPersonaProfile(profile: LearnerPersonaProfile) {
        val userId = _currentUserId.value?.takeIf { it > 0 } ?: profile.userId
        val updatedProfile = profile.copy(userId = userId)
        _learnerPersonaProfile.postValue(updatedProfile)

        // 1. SharedPreferences persistence (Moshi)
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(LearnerPersonaProfile::class.java)
            val json = adapter.toJson(updatedProfile)
            prefs.edit().putString("learner_persona_$userId", json).apply()
        } catch (_: Exception) {}

        // 2. Set learning preferences
        setPreferences(updatedProfile.evaluatedCefr, "English")

        // 3. Save baseline weak points into Room so SM-2 and adaptive chapter generators pick them up immediately
        viewModelScope.launch {
            try {
                for (weakTag in updatedProfile.baselineWeakPoints) {
                    val prompt = when (weakTag) {
                        "Present_Simple_Agreement" -> "Chủ ngữ số ít 'She' chia động từ ở thì Hiện tại đơn"
                        "Conditional_Type_2" -> "Câu điều kiện loại 2 (If + S + V2/ed, S + would + V)"
                        "Negative_Inversion" -> "Đảo ngữ câu khẳng định với trạng từ phủ định (Scarcely had he...)"
                        "Basic_Collocation" -> "Cụm kết hợp từ tự nhiên: have dinner, make an effort"
                        "Phrasal_Verbs" -> "Cụm động từ thông dụng: drop by, look forward to"
                        "Academic_Collocation" -> "Cụm từ học thuật chuẩn xác: groundbreaking breakthrough"
                        "Acoustic_Everyday_Greeting" -> "Nhận diện ngữ âm câu chào hỏi cơ bản"
                        "Acoustic_Connected_Speech" -> "Nhận diện hiện tượng nuốt âm & nối âm (Connected Speech)"
                        "Acoustic_Academic_Cadence" -> "Nghe hiểu câu học thuật tốc độ tự nhiên bản xứ"
                        "Spoken_Greeting_Fluency" -> "Luyện phát âm câu giao tiếp hàng ngày"
                        "Spoken_Polite_Inquiry" -> "Luyện ngữ điệu câu hỏi lịch sự"
                        "Spoken_Rhythm_Connected" -> "Luyện nhịp điệu trọng âm câu phức"
                        else -> "Kỹ năng cần củng cố: $weakTag"
                    }
                    repository.recordWeakPoint(
                        userId = userId,
                        question = prompt,
                        options = listOf("Luyện phản xạ âm thanh", "Ôn tập cấu trúc câu", "Ứng dụng trong ngữ cảnh"),
                        correctIndex = 0
                    )
                }
                refreshWeakPoints()
            } catch (_: Exception) {}

            // 4. Record baseline diagnostic telemetry entry
            try {
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val telemetry = ResearchTelemetryEntity(
                    sessionId = "cat_placement_${System.currentTimeMillis()}",
                    userId = userId,
                    timestamp = nowStr,
                    sloTopic = "CAT_DIAGNOSTIC_PLACEMENT",
                    challengeType = "ADAPTIVE_TEST",
                    itemIdentifier = "Theta=${updatedProfile.thetaAbility};CEFR=${updatedProfile.evaluatedCefr};Goal=${updatedProfile.goal};Pace=${updatedProfile.dailyTargetMinutes}m",
                    responseLatencyMs = 0L,
                    isCorrect = true,
                    usedHint = false,
                    isDistracted = false,
                    objectiveQuality = 5,
                    easinessFactor = updatedProfile.initialEasinessFactor,
                    intervalDays = 1,
                    retentionProbability = 1.0f
                )
                repository.saveTelemetry(telemetry)
            } catch (_: Exception) {}
        }
    }

    fun loadLearnerPersonaProfile(userId: Long? = null): LearnerPersonaProfile? {
        val targetId = userId ?: _currentUserId.value?.takeIf { it > 0 } ?: return null
        val json = prefs.getString("learner_persona_$targetId", null) ?: return null
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            moshi.adapter(LearnerPersonaProfile::class.java).fromJson(json)?.also {
                _learnerPersonaProfile.postValue(it)
            }
        } catch (_: Exception) { null }
    }

    fun setPreferences(level: String, subject: String) {
        _selectedLevel.value = level
        _selectedSubject.value = if (subject.isBlank() || subject != "English") "English" else subject
        val userId = _currentUserId.value
        if (userId != null && userId != -1L) {
            prefs.edit().putString("learning_level_$userId", level).apply()
        }
    }

    fun updateLearningLevel(level: String) {
        _selectedLevel.value = level
        val userId = _currentUserId.value
        if (userId != null && userId != -1L) {
            prefs.edit().putString("learning_level_$userId", level).apply()
        }
        refreshChapters()
    }

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    fun refreshAll() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                refreshChapters()
                fetchLeaderboard()
                refreshDailyTasks()
                val userId = _currentUserId.value
                if (userId != null && userId != -1L) {
                    refreshHistory(userId)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
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
        val safeLesson = lessonIndex.coerceIn(1, 5)
        val startIndex = (safeLesson - 1) * 10
        if (allChapterQuestions.isNotEmpty()) {
            val safeStart = if (startIndex in 0 until allChapterQuestions.size) startIndex else 0
            val endIndex = (safeStart + 10).coerceAtMost(allChapterQuestions.size)
            _questions.value = allChapterQuestions.subList(safeStart, endIndex)
        } else {
            // Asynchronous fallback: đảm bảo nạp câu hỏi an toàn không bị kẹt spinner
            viewModelScope.launch {
                val currentTitle = _currentChapterTitle.value
                val chapterId = if (!currentTitle.isNullOrBlank()) {
                    repository.getChapterIdByTitle(currentTitle)
                } else -1L

                if (chapterId != -1L) {
                    allChapterQuestions = repository.getQuestionsByChapter(chapterId)
                } else {
                    val defaultChapters = repository.getChaptersByPreference("Beginner", "English")
                    val defaultTitle = defaultChapters.firstOrNull() ?: "English Fundamentals"
                    val defId = repository.getChapterIdByTitle(defaultTitle)
                    if (defId != -1L) {
                        allChapterQuestions = repository.getQuestionsByChapter(defId)
                    }
                }

                if (allChapterQuestions.isNotEmpty()) {
                    val safeStart = if (startIndex in 0 until allChapterQuestions.size) startIndex else 0
                    val endIndex = (safeStart + 10).coerceAtMost(allChapterQuestions.size)
                    _questions.value = allChapterQuestions.subList(safeStart, endIndex)
                } else {
                    _questions.value = emptyList()
                }
            }
        }
    }

    private val _chapterUnlockEvent = MutableLiveData<ChapterUnlockEvent?>(null)
    val chapterUnlockEvent: LiveData<ChapterUnlockEvent?> = _chapterUnlockEvent

    fun clearChapterUnlockEvent() {
        _chapterUnlockEvent.value = null
    }

    private val _isGeneratingNextChapter = MutableLiveData(false)
    val isGeneratingNextChapter: LiveData<Boolean> = _isGeneratingNextChapter

    fun completeCurrentLesson() {
        val userId = _currentUserId.value ?: return
        val chapterTitle = _currentChapterTitle.value ?: return
        val node = _currentNode.value ?: return
        if (userId == -1L) return

        val isFreshChapterCompletion = (node < 6 && node + 1 >= 6)
        val nextNode = (node + 1).coerceAtMost(6)
        _currentNode.value = nextNode
        repository.saveCurrentLessonNode(userId, chapterTitle, nextNode)
        refreshChapters()

        if (isFreshChapterCompletion) {
            handleChapterCompletion(userId, chapterTitle)
        }
    }

    private val _questAnalysis = MutableLiveData<String?>(null)
    val questAnalysis: LiveData<String?> = _questAnalysis

    fun analyzeQuestResults(context: Context, results: List<QuestItem>) {
        _lastQuestResults.value = results

        val correctCount = results.count { it.selectedIndex == it.correctIndex }
        if (correctCount > 0) {
            addXp(correctCount * 20)
        }
        val totalCount = results.size
        val accuracy = if (totalCount > 0) (correctCount * 100) / totalCount else 0
        val isEng = _isEnglish.value ?: false

        // Generate instant, insightful baseline analysis immediately so UI NEVER gets stuck loading
        val instantAnalysis = if (correctCount == totalCount && totalCount > 0) {
            if (isEng) {
                "• Outstanding performance: $correctCount/$totalCount (100%)!\n• Key Strength: Excellent mastery of sentence patterns and vocabulary.\n• Recommendation: Advance to the next stage to unlock advanced topics."
            } else {
                "• Kết quả xuất sắc: $correctCount/$totalCount (100%)!\n• Điểm mạnh: Nắm bắt rất chuẩn cấu trúc ngữ pháp và từ vựng của bài học.\n• Lời khuyên: Hãy tiếp tục hành trình để mở rộng thêm kiến thức mới."
            }
        } else if (totalCount > 0) {
            val mistakes = results.filter { it.selectedIndex != it.correctIndex }
            if (isEng) {
                "• Lesson Score: $correctCount/$totalCount ($accuracy% accuracy).\n• Focus Area: Review the ${mistakes.size} incorrect questions in the review list.\n• Tip: Practice listening and speaking challenges repeatedly to build natural intuition."
            } else {
                "• Kết quả bài học: $correctCount/$totalCount câu đúng ($accuracy% độ chính xác).\n• Trọng tâm ôn tập: Chú ý ${mistakes.size} câu làm sai trong danh sách chi tiết bên dưới.\n• Mẹo ghi nhớ: Nhấn nút loa nghe phát âm và bấm dấu trang để lưu từ vựng khó vào Sổ tay."
            }
        } else {
            if (isEng) "• Complete lessons to receive personalized AI mentor insights."
            else "• Hoàn thành bài học để nhận phân tích chi tiết từ Cố vấn AI."
        }
        _questAnalysis.value = instantAnalysis

        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                val chapterId = repository.getChapterIdByTitle(_currentChapterTitle.value ?: "")
                if (chapterId != -1L) {
                    repository.saveQuizAttempt(userId, chapterId, totalCount, correctCount)
                }

                // Ghi nhận dữ liệu thực nghiệm thụ động (Passive Research Telemetry)
                val sessionTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val sessionId = "SES_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "_" + UUID.randomUUID().toString().take(6)
                val currentChapter = _currentChapterTitle.value ?: "Microlearning Unit"

                val telemetryList = results.map { item ->
                    val isItemCorrect = item.selectedIndex == item.correctIndex
                    val latency = if (item.responseLatencyMs > 0) item.responseLatencyMs else 2500L
                    val isDistracted = item.isDistracted || (latency > Sm2Algorithm.DISTRACTION_LATENCY_THRESHOLD_MS)
                    val q = if (item.objectiveQuality > 0) item.objectiveQuality
                            else Sm2Algorithm.calculateObjectiveQuality(
                                isCorrect = isItemCorrect,
                                responseLatencyMs = latency,
                                usedHint = item.usedHint,
                                isDistracted = isDistracted
                            )
                    val pseudoNextState = Sm2Algorithm.calculateNextReview(
                        Sm2State(repetition = 0, intervalDays = 1, easinessFactor = 2.5f),
                        q
                    )
                    val retentionR = Sm2Algorithm.calculateRetentionProbability(0.0, pseudoNextState.intervalDays.toDouble()).toFloat()

                    ResearchTelemetryEntity(
                        sessionId = sessionId,
                        userId = userId,
                        timestamp = sessionTimestamp,
                        sloTopic = if (item.sloTopic.isNotBlank()) item.sloTopic else currentChapter,
                        challengeType = item.type.name,
                        itemIdentifier = item.question.take(100),
                        responseLatencyMs = latency,
                        isCorrect = isItemCorrect,
                        usedHint = item.usedHint,
                        isDistracted = isDistracted,
                        objectiveQuality = q,
                        easinessFactor = pseudoNextState.easinessFactor,
                        intervalDays = pseudoNextState.intervalDays,
                        retentionProbability = retentionR
                    )
                }
                repository.saveTelemetryBatch(telemetryList)

                // Kích hoạt cơ chế nạp trước dự đoán (Predictive Prefetching) ở chế độ nền
                prefetchNextMicrolearningUnit(userId, currentChapter)

                recordDailyTaskProgress(userId, "LESSON_COMPLETE", 1)
                recordDailyTaskProgress(userId, "ANSWER_QUESTIONS", results.size)
                if (results.isNotEmpty() && correctCount == results.size) {
                    recordDailyTaskProgress(userId, "PERFECT_SCORE", 1)
                }

                // Tự động gom các câu làm sai vào Kho Điểm Yếu (Smart Mistake Bank) với 4 phương án cân bằng
                for (item in results) {
                    if (item.selectedIndex != item.correctIndex) {
                        recordAdaptedWeakPoint(userId, item)
                    }
                }
                refreshWeakPoints()
                refreshDailyTasks()
                checkAndUpdateStreak(userId)
                firestoreSyncManager.syncUserProfile(userId)
                firestoreSyncManager.syncWeakPoints(userId)

                // Kiểm tra mở khóa huy hiệu thành tựu
                checkAndUnlockBadge(BadgeId.FIRST_STEP)
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (hour >= 22 || hour < 4) {
                    checkAndUnlockBadge(BadgeId.NIGHT_OWL)
                }
                if (results.any { it.type == QuestionType.SPEAKING_CHALLENGE && (it.pronunciationScore ?: 0) >= 85 }) {
                    checkAndUnlockBadge(BadgeId.SPEAKING_ACE)
                }
            }

            // Build analysis prompt for deeper AI insight
            val mistakeSummary = buildString {
                val mistakes = results.filter { it.selectedIndex != it.correctIndex }
                if (mistakes.isEmpty()) {
                    append("Student answered all ${results.size} questions correctly. Score: ${results.size}/${results.size}.")
                } else {
                    append("Student score: $correctCount/${results.size}.\nMistakes made:\n")
                    mistakes.take(5).forEach { item ->
                        val selected = item.options.getOrNull(item.selectedIndex) ?: "Skipped"
                        val correct = item.options.getOrNull(item.correctIndex) ?: item.correctSentence.ifEmpty { "" }
                        append("- Q: ${item.question.take(60)}\n")
                        append("  Student: $selected | Correct: $correct\n")
                    }
                }
            }

            // Helper to clean raw text
            fun sanitizeReply(text: String): String {
                return text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                    .replace(Regex("\\*(.+?)\\*"), "$1")
                    .replace(Regex("(?m)^#{1,6}\\s*"), "")
                    .replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
                    .trim()
            }

            // Try backend mentorChat API first (faster and well-formatted) with 3.5s timeout
            try {
                val token = authManager.getIdToken() ?: "mock_dev_token_kiki_explorer"
                val analysisMessage = "Analyze these lesson results and give encouraging feedback with study tips: $mistakeSummary"
                val request = MentorChatRequest(
                    message = analysisMessage,
                    userLevel = _selectedLevel.value ?: "Intermediate",
                    currentChapter = _currentChapterTitle.value ?: "English Fundamentals",
                    recentWeakPoints = emptyList()
                )
                val response = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(3500) {
                        backendApiService.mentorChat("Bearer $token", request)
                    }
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.reply.isNotBlank()) {
                        _questAnalysis.value = sanitizeReply(body.reply)
                        return@launch
                    }
                }
            } catch (_: Exception) {}

            // Fallback: use direct Groq API with 4.5s timeout
            try {
                val prompt = buildString {
                    append(mistakeSummary)
                    append("\n\nAct as a supportive English tutor. Analyze the mistakes above, briefly explain what went wrong, "
                        + "and suggest what to review next. Format your response using bullet points starting with • character. "
                        + "Do NOT use ** asterisks or # hash symbols. Plain text only with • bullets.")
                }
                val analysis = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(4500) {
                        fetchAnswerFromAI(context, prompt, null)
                    }
                }
                if (!analysis.isNullOrBlank() && !analysis.startsWith("⚠️") && !analysis.startsWith("Lỗi kết nối")) {
                    _questAnalysis.value = sanitizeReply(analysis)
                }
            } catch (_: Exception) {
                // Keep instantAnalysis already set
            }
        }
    }

    private val _isAuthLoading = MutableLiveData(false)
    val isAuthLoading: LiveData<Boolean> = _isAuthLoading

    fun signInWithGoogle(
        activityContext: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isAuthLoading.value = true
        viewModelScope.launch {
            try {
                when (val result = authManager.signInWithGoogle(activityContext)) {
                    is GoogleAuthResult.Success -> {
                        val userId = repository.getOrCreateSocialUser(
                            email = result.email,
                            displayName = result.displayName,
                            photoUrl = result.photoUrl
                        )
                        if (userId != -1L) {
                            loadUserData(result.email)
                            _isAuthLoading.value = false
                            onSuccess()
                        } else {
                            _isAuthLoading.value = false
                            onError("Không thể đồng bộ dữ liệu tài khoản Google.")
                        }
                    }
                    is GoogleAuthResult.Cancelled -> {
                        _isAuthLoading.value = false
                    }
                    is GoogleAuthResult.Error -> {
                        _isAuthLoading.value = false
                        onError(result.message)
                    }
                }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                onError(e.localizedMessage ?: "Lỗi xác thực Google.")
            }
        }
    }

    fun signInAsGuest(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isAuthLoading.value = true
        viewModelScope.launch {
            try {
                when (val result = authManager.signInAnonymously()) {
                    is GoogleAuthResult.Success -> {
                        val userId = repository.getOrCreateSocialUser(
                            email = result.email,
                            displayName = result.displayName ?: "Kiki Explorer",
                            photoUrl = result.photoUrl
                        )
                        if (userId != -1L) {
                            loadUserData(result.email)
                            _isAuthLoading.value = false
                            onSuccess()
                        } else {
                            _isAuthLoading.value = false
                            onError("Không thể tạo phiên khám phá khách.")
                        }
                    }
                    is GoogleAuthResult.Cancelled -> {
                        _isAuthLoading.value = false
                    }
                    is GoogleAuthResult.Error -> {
                        _isAuthLoading.value = false
                        onError(result.message)
                    }
                }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                onError(e.localizedMessage ?: "Lỗi tạo phiên khách.")
            }
        }
    }

    fun getLastLoggedInEmail(): String? {
        return prefs.getString("last_logged_in_email", null)
    }

    fun isUserOnboardingCompleted(userId: Long): Boolean {
        if (userId <= 0) return false
        // 1. Kiểm tra cờ trong SharedPreferences
        if (prefs.getBoolean("onboarding_completed_$userId", false)) return true
        // 2. Kiểm tra nếu đã có cấp độ học được lưu
        val savedLevel = prefs.getString("learning_level_$userId", null)
        if (!savedLevel.isNullOrBlank()) {
            prefs.edit().putBoolean("onboarding_completed_$userId", true).apply()
            return true
        }
        // 3. Kiểm tra nếu đã có điểm XP (> 0 tức là đã từng học/làm bài)
        if ((_xp.value ?: 0) > 0) {
            prefs.edit().putBoolean("onboarding_completed_$userId", true).apply()
            return true
        }
        return false
    }

    fun completeOnboarding(userId: Long, level: String) {
        setPreferences(level, "English")
        if (userId > 0) {
            prefs.edit().putBoolean("onboarding_completed_$userId", true).apply()
            firestoreSyncManager.syncUserProfile(userId)
        }
    }

    suspend fun loadUserData(email: String): Long {
        val userId = repository.getUserIdByEmail(email)
        if (userId != -1L) {
            prefs.edit().putString("last_logged_in_email", email).apply()

            // Khôi phục dữ liệu từ Cloud nếu có bản sao mới hơn
            firestoreSyncManager.restoreProgressFromCloud(userId)

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
                _streakShields.value = profile.streakShields
                _avatarUri.value = profile.avatarUri
                _studyMotto.value = profile.studyMotto ?: ""
            }
            val savedLevel = prefs.getString("learning_level_$userId", _selectedLevel.value ?: "Beginner") ?: "Beginner"
            _selectedLevel.value = savedLevel

            // Nếu user đã có XP hoặc đã lưu level, tự động đánh dấu đã hoàn thành onboarding
            if ((profile?.totalXp ?: 0) > 0 || !prefs.getString("learning_level_$userId", null).isNullOrBlank()) {
                prefs.edit().putBoolean("onboarding_completed_$userId", true).apply()
            }

            refreshChapters()
            fetchLeaderboard()
            refreshDailyTasks()
            refreshHistory(userId)
            checkAndUpdateStreak(userId)
            refreshVocabulary()
            refreshWeakPoints()
            refreshBadges(userId)
            loadLearnerPersonaProfile(userId)

            // Tự động đồng bộ toàn bộ dữ liệu lên Cloud Firestore ngầm
            firestoreSyncManager.syncAllToCloud(userId)
        }
        return userId
    }

    fun onLoginSuccess(email: String) {
        viewModelScope.launch {
            loadUserData(email)
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
                soundEffectManager.playFanfare()
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

    fun askQuestion(
        context: Context,
        question: String,
        imageUri: Uri?,
        isAudioDialog: Boolean = false,
        onSuccess: (() -> Unit)? = null
    ) {
        if (question.isBlank() && imageUri == null) return
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                var answer: String
                var grammarTip: String? = null
                var vocabularies: List<VocabularyCardDto> = emptyList()

                // Augment vocab/lookup questions to strictly request the concise 1-card format
                val cleanQuestion = question.trim()
                val isLookup = cleanQuestion.startsWith("🔍") ||
                        cleanQuestion.contains("Tra từ", ignoreCase = true) ||
                        cleanQuestion.contains("Tra thành ngữ", ignoreCase = true) ||
                        cleanQuestion.contains("Look up", ignoreCase = true) ||
                        cleanQuestion.contains("nghĩa là gì", ignoreCase = true) ||
                        (cleanQuestion.split(Regex("\\s+")).size <= 3 && !cleanQuestion.endsWith("?") && !cleanQuestion.contains("tại sao", ignoreCase = true) && !cleanQuestion.contains("why", ignoreCase = true))

                val promptForAi = if (isLookup && imageUri == null) {
                    val targetWord = cleanQuestion
                        .replace(Regex("^🔍\\s*(Tra từ|Tra thành ngữ|Look up word|Look up idiom):?\\s*"), "")
                        .replace(Regex("['\"]"), "")
                        .replace(Regex("&\\s*ví dụ.*", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("in context.*", RegexOption.IGNORE_CASE), "")
                        .trim()

                    "Giải thích từ/cụm từ: '$targetWord'. Trả lời NGẮN GỌN trong DUY NHẤT 1 câu trả lời theo đúng mẫu sau (tuyệt đối không chào hỏi, không giới thiệu, không bảng biểu, không dùng [STEP]):\n" +
                    "$targetWord /IPA/ (Từ loại)\n" +
                    "• Nghĩa: [Giải thích nghĩa tiếng Việt ngắn gọn, súc tích]\n" +
                    "• Ví dụ: [Câu tiếng Anh] ([Dịch nghĩa tiếng Việt])\n" +
                    "• Từ đồng nghĩa: [3-5 từ đồng nghĩa]\n" +
                    "• Từ trái nghĩa: [2-4 từ trái nghĩa]"
                } else {
                    cleanQuestion
                }

                if (imageUri != null) {
                    answer = fetchAnswerFromAI(context, question, imageUri)
                } else if (getEffectiveActiveProvider() == PROVIDER_GEMINI) {
                    // Trực tiếp dùng Gemini 2.5 Flash Native Audio Dialog để tối ưu tốc độ và độ chính xác
                    answer = fetchAnswerFromGemini(context, promptForAi, null)
                    if (answer.startsWith("Lỗi kết nối") || answer.startsWith("⚠️")) {
                        try {
                            val token = authManager.getIdToken() ?: "mock_dev_token_kiki_explorer"
                            val weakPoints = _weakPoints.value?.take(5)?.map { it.question } ?: emptyList()
                            val request = MentorChatRequest(
                                message = promptForAi,
                                userLevel = _selectedLevel.value ?: "Intermediate",
                                currentChapter = _currentChapterTitle.value ?: "English Fundamentals",
                                recentWeakPoints = weakPoints
                            )
                            val response = backendApiService.mentorChat("Bearer $token", request)
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                answer = body.reply
                                grammarTip = body.grammarTip
                                vocabularies = body.detectedVocabularies
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    try {
                        val token = authManager.getIdToken() ?: "mock_dev_token_kiki_explorer"
                        val weakPoints = _weakPoints.value?.take(5)?.map { it.question } ?: emptyList()
                        val request = MentorChatRequest(
                            message = promptForAi,
                            userLevel = _selectedLevel.value ?: "Intermediate",
                            currentChapter = _currentChapterTitle.value ?: "English Fundamentals",
                            recentWeakPoints = weakPoints
                        )
                        val response = backendApiService.mentorChat("Bearer $token", request)
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            answer = body.reply
                            grammarTip = body.grammarTip
                            vocabularies = body.detectedVocabularies
                        } else {
                            answer = fetchAnswerFromAI(context, promptForAi, null)
                        }
                    } catch (e: Exception) {
                        answer = fetchAnswerFromAI(context, promptForAi, null)
                    }
                }
                answer = sanitizeAiMentorReply(answer)
                if (grammarTip != null) {
                    grammarTip = sanitizeAiMentorReply(grammarTip)
                }

                // Trích xuất thẻ từ vựng tự động từ kết quả Gemini nếu backend offline
                if (vocabularies.isEmpty() && isLookup && !answer.startsWith("Lỗi") && !answer.startsWith("⚠️")) {
                    val lines = answer.lines()
                    val headerLine = lines.firstOrNull { it.isNotBlank() } ?: ""
                    val phoneticMatch = Regex("/([^/]+)/").find(headerLine)?.value
                    val meaningLine = lines.firstOrNull { it.contains("Nghĩa:", ignoreCase = true) }
                        ?.replace(Regex("^•?\\s*Nghĩa:\\s*", RegexOption.IGNORE_CASE), "")?.trim()
                    val exampleLine = lines.firstOrNull { it.contains("Ví dụ:", ignoreCase = true) }
                        ?.replace(Regex("^•?\\s*Ví dụ:\\s*", RegexOption.IGNORE_CASE), "")?.trim()
                    if (!meaningLine.isNullOrBlank()) {
                        val targetWord = cleanQuestion
                            .replace(Regex("^🔍\\s*(Tra từ|Tra thành ngữ|Look up word|Look up idiom):?\\s*"), "")
                            .replace(Regex("['\"]"), "")
                            .replace(Regex("&\\s*ví dụ.*", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("in context.*", RegexOption.IGNORE_CASE), "")
                            .trim()
                        vocabularies = listOf(
                            VocabularyCardDto(
                                word = targetWord,
                                phonetic = phoneticMatch,
                                meaning = meaningLine,
                                example = exampleLine
                            )
                        )
                    }
                }

                val userId = _currentUserId.value
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, question, imageUri?.toString(), answer)
                    val data = repository.getAIHistory(userId).toMutableList()
                    if (data.isNotEmpty()) {
                        data[0] = data[0].copy(grammarTip = grammarTip, detectedVocabularies = vocabularies)
                    }
                    _history.value = data
                    repository.updateXP(userId, 20)
                    val newXp = (_xp.value ?: 0) + 20
                    _xp.value = newXp
                    _level.value = (newXp / 100) + 1
                    _rankTitle.value = calculateRankTitle(newXp)
                    firestoreSyncManager.syncUserProfile(userId)
                } else {
                    val current = _history.value ?: emptyList()
                    val updated = mutableListOf(
                        QAItem(
                            id = 0,
                            question = question,
                            imageUri = imageUri?.toString(),
                            answer = answer,
                            grammarTip = grammarTip,
                            detectedVocabularies = vocabularies
                        )
                    )
                    updated.addAll(current)
                    _history.value = updated
                }

                if (isAudioDialog && (_audioDialogAutoTts.value == true)) {
                    val spokenText = GeminiModels.formatForAudioSpeech(answer)
                    if (spokenText.isNotBlank()) {
                        ttsManager.speak(spokenText)
                    }
                }
            } finally {
                _isGenerating.value = false
                onSuccess?.invoke()
            }
        }
    }

    fun askGeminiAudioDialog(context: Context, spokenText: String, onSuccess: (() -> Unit)? = null) {
        askQuestion(context, spokenText, null, isAudioDialog = true, onSuccess = onSuccess)
    }

    fun askFollowUpQuestion(context: Context, currentQuestion: String, followUpType: String) {
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                var prompt = ""
                var title = ""
                when (followUpType) {
                    "EXPLAIN" -> {
                        prompt = "Based on the question: '$currentQuestion'. Please explain it in more detail concisely in 1 single answer with • bullet points. Do not include conversational greetings or markdown tables."
                        title = "Giải thích thêm: $currentQuestion"
                    }
                    "SIMILAR" -> {
                        prompt = "Based on the question: '$currentQuestion'. Give me a similar practice exercise with a hint concisely in 1 single answer with • bullet points. Do not include conversational greetings or markdown tables."
                        title = "Bài tập tương tự: $currentQuestion"
                    }
                }
                val rawAnswer = fetchAnswerFromAI(context, prompt, null)
                val answer = sanitizeAiMentorReply(rawAnswer)
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
                val prompt = "Perform a deep academic analysis of this topic: '$question'. Cover: 1) Core concept, 2) Theory, 3) Common mistakes, 4) Real-world applications. Format concisely in 1 single answer with • bullet points. Do not include greetings, conversational filler, or markdown tables."
                val title = "[Phân tích sâu] $question"
                val rawAnswer = fetchAnswerFromAI(context, prompt, null)
                val answer = sanitizeAiMentorReply(rawAnswer)
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
                val raw = fetchAnswerFromAI(context, prompt, null)
                val blocks = raw.split("---").map { it.trim() }.filter { it.isNotEmpty() }
                _quizQuestions.value = blocks
            } finally {
                _quizLoading.value = false
            }
        }
    }

    private val _isGeneratingChapter = MutableLiveData(false)
    val isGeneratingChapter: LiveData<Boolean> = _isGeneratingChapter

    /**
     * Sinh một chương học cá nhân hóa mới bằng Backend AI (hoặc bộ sinh thông minh nội bộ), tự động lưu vào Room SQLite
     */
    fun generateCustomChapter(
        level: String,
        topic: String,
        weakWords: List<String> = emptyList(),
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        _isGeneratingChapter.value = true
        viewModelScope.launch {
            try {
                var chapterData: ChapterGeneratedResponse? = null
                // 1. Thử gọi Backend với timeout 3.5s
                try {
                    val token = authManager.getIdToken() ?: "mock_dev_token_kiki_explorer"
                    val request = GenerateChapterRequest(
                        userId = _currentUserId.value?.toString(),
                        level = level,
                        topic = topic,
                        weakWords = weakWords,
                        questionCount = 5
                    )
                    val response = withContext(Dispatchers.IO) {
                        kotlinx.coroutines.withTimeoutOrNull(3500) {
                            backendApiService.generateChapter("Bearer $token", request)
                        }
                    }
                    if (response != null && response.isSuccessful && response.body() != null) {
                        chapterData = response.body()
                    }
                } catch (_: Exception) {}

                // 2. Nếu Backend offline hoặc quá tải: Dùng bộ kiến tạo lộ trình cá nhân hóa Kiki AI
                if (chapterData == null) {
                    chapterData = generatePersonalizedChapter(level, topic, weakWords)
                }

                val newChapterId = repository.saveAiGeneratedChapter(chapterData, isPriorityFirst = true)
                if (newChapterId != -1L) {
                    refreshChapters()
                    _isGeneratingChapter.value = false
                    onResult(true, "Đã khởi tạo thành công chương: ${chapterData.title}")
                } else {
                    _isGeneratingChapter.value = false
                    onResult(false, "Không thể lưu chương học vào cơ sở dữ liệu.")
                }
            } catch (e: Exception) {
                _isGeneratingChapter.value = false
                onResult(false, "Lỗi kết nối Backend AI: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Kiến tạo chương học cá nhân hóa hoàn chỉnh với đầy đủ 5 dạng câu hỏi theo tiêu chuẩn questions.json
     */
    fun generatePersonalizedChapter(
        level: String,
        topic: String,
        weakWords: List<String> = emptyList()
    ): ChapterGeneratedResponse {
        val isEng = _isEnglish.value ?: false
        val displayLevel = if (level.isBlank()) "Beginner" else level
        val cleanTopic = if (topic.isBlank()) "Daily Conversations" else topic

        val chapterTitle = if (isEng) "AI Roadmap: $cleanTopic ($displayLevel)" else "Lộ trình AI: $cleanTopic ($displayLevel)"
        val chapterDesc = if (isEng) "Customized curriculum tailored by Kiki AI based on your diagnostic placement test."
        else "Lộ trình cá nhân hóa do AI Kiki kiến tạo dựa trên bài kiểm tra chẩn đoán năng lực của bạn."

        // 1. Multiple Choice
        val q1 = QuestionItemDto(
            questionText = when {
                cleanTopic.contains("Grammar", ignoreCase = true) -> "She ______ to school by bus every morning."
                cleanTopic.contains("IELTS", ignoreCase = true) -> "The research indicates a significant ______ between study habits and performance."
                else -> "How do you respond politely when a friend asks 'How are you today?'"
            },
            questionType = "MULTIPLE_CHOICE",
            optionsJson = org.json.JSONArray(
                when {
                    cleanTopic.contains("Grammar", ignoreCase = true) -> listOf("goes", "go", "going", "is go")
                    cleanTopic.contains("IELTS", ignoreCase = true) -> listOf("correlation", "distraction", "neglect", "collapse")
                    else -> listOf("I'm doing great, thank you! How about you?", "Yes, I am.", "No, not at all.", "I go home.")
                }
            ).toString(),
            correctAnswer = "0",
            explanation = "Chọn phương án chính xác nhất để hoàn thiện câu phù hợp ngữ cảnh.",
            points = 10,
            difficulty = "EASY"
        )

        // 2. Sentence Builder
        val q2 = QuestionItemDto(
            questionText = when {
                cleanTopic.contains("Grammar", ignoreCase = true) -> "If I had more free time, I ______ learn another foreign language."
                cleanTopic.contains("IELTS", ignoreCase = true) -> "The new economic policy was designed to ______ sustainable development."
                else -> "Look! The children ______ happily in the school playground."
            },
            questionType = "SENTENCE_BUILDER",
            optionsJson = org.json.JSONArray(
                when {
                    cleanTopic.contains("Grammar", ignoreCase = true) -> listOf("would", "will", "am", "have")
                    cleanTopic.contains("IELTS", ignoreCase = true) -> listOf("foster", "demolish", "stagnate", "diminish")
                    else -> listOf("are playing", "plays", "played", "play")
                }
            ).toString(),
            correctAnswer = "0",
            explanation = "Sắp xếp và điền từ chính xác vào chỗ trống.",
            points = 10,
            difficulty = "MEDIUM"
        )

        // 3. Listening Challenge
        val q3 = QuestionItemDto(
            questionText = "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
            questionType = "LISTENING_CHALLENGE",
            optionsJson = org.json.JSONArray(
                when {
                    cleanTopic.contains("Grammar", ignoreCase = true) -> listOf("They have lived in this city since 2018")
                    cleanTopic.contains("IELTS", ignoreCase = true) -> listOf("Technological innovation has fundamentally reshaped modern global industries")
                    else -> listOf("It is wonderful to meet you here this morning")
                }
            ).toString(),
            correctAnswer = "0",
            explanation = "Luyện kỹ năng nghe và sắp xếp các từ thành câu hoàn chỉnh.",
            points = 10,
            difficulty = "MEDIUM"
        )

        // 4. Matching Pairs
        val q4 = QuestionItemDto(
            questionText = "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng 🧩",
            questionType = "MATCHING_PAIRS",
            optionsJson = org.json.JSONArray(
                when {
                    cleanTopic.contains("Grammar", ignoreCase = true) -> listOf(
                        "although=mặc dù", "furthermore=hơn nữa", "therefore=do đó", "meanwhile=trong khi đó"
                    )
                    cleanTopic.contains("IELTS", ignoreCase = true) -> listOf(
                        "substantial=đáng kể", "crucial=tối quan trọng", "feasible=khả thi", "alleviate=làm giảm bớt"
                    )
                    else -> listOf(
                        "catch up=hàn huyên", "hang out=đi chơi", "drop by=ghé thăm", "keep in touch=giữ liên lạc"
                    )
                }
            ).toString(),
            correctAnswer = "0",
            explanation = "Ghép cặp từ vựng chính xác để ghi nhớ nghĩa.",
            points = 10,
            difficulty = "MEDIUM"
        )

        // 5. Speaking Challenge
        val q5 = QuestionItemDto(
            questionText = "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
            questionType = "SPEAKING_CHALLENGE",
            optionsJson = org.json.JSONArray(
                when {
                    cleanTopic.contains("Grammar", ignoreCase = true) -> listOf("Practice makes perfect if you stay consistent every single day.")
                    cleanTopic.contains("IELTS", ignoreCase = true) -> listOf("Environmental conservation requires proactive collaboration across all communities.")
                    else -> listOf("Could you please show me the way to the nearest bookstore?")
                }
            ).toString(),
            correctAnswer = "0",
            explanation = "Luyện phát âm to, rõ ràng và chuẩn ngữ điệu bản xứ.",
            points = 10,
            difficulty = "HARD"
        )

        return ChapterGeneratedResponse(
            title = chapterTitle,
            description = chapterDesc,
            level = displayLevel,
            subject = "English",
            orderNo = 0,
            questions = listOf(q1, q2, q3, q4, q5)
        )
    }

    /**
     * Xử lý cơ chế mở khóa & tự sinh chương tiếp theo khi hoàn thành bài học thứ 5 (Giai đoạn 3)
     */
    fun handleChapterCompletion(userId: Long, completedChapterTitle: String) {
        viewModelScope.launch {
            val chapters = _availableChapters.value?.takeIf { it.isNotEmpty() }
                ?: repository.getChaptersByPreference(_selectedLevel.value, _selectedSubject.value)
            val currentIndex = chapters.indexOf(completedChapterTitle)
            val hasNextPredefined = (currentIndex != -1 && currentIndex + 1 < chapters.size)

            if (hasNextPredefined) {
                val nextChapter = chapters[currentIndex + 1]
                _chapterUnlockEvent.postValue(
                    ChapterUnlockEvent(
                        completedChapterTitle = completedChapterTitle,
                        nextChapterTitle = nextChapter,
                        isAiGenerated = false,
                        weakPointsAddressedCount = 0
                    )
                )
            } else {
                // Tự sinh chương tiếp theo với AI dựa trên điểm yếu
                generateNextChapterOnDemand(
                    userId = userId,
                    completedChapterTitle = completedChapterTitle
                )
            }
        }
    }

    val prefetchedMicrolearningCache = java.util.concurrent.ConcurrentHashMap<String, ChapterGeneratedResponse>()

    fun prefetchNextMicrolearningUnit(userId: Long, currentTopic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chapters = _availableChapters.value?.takeIf { it.isNotEmpty() }
                    ?: repository.getChaptersByPreference(_selectedLevel.value, _selectedSubject.value)
                val targetLevel = _selectedLevel.value ?: "Beginner"
                val nextTopic = determineNextTopic(chapters, targetLevel)
                if (prefetchedMicrolearningCache.containsKey(nextTopic)) return@launch

                val weakPointsList = repository.getWeakPoints(userId)
                val weakWords = extractWeakKeywords(weakPointsList)

                val generated = generate3CMicrolearningWithGemini(nextTopic, targetLevel, weakWords)
                if (generated != null) {
                    prefetchedMicrolearningCache[nextTopic] = generated
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun generate3CMicrolearningWithGemini(
        topic: String,
        level: String,
        weakWords: List<String>
    ): ChapterGeneratedResponse? = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveGeminiApiKey()
            if (apiKey.isBlank() || apiKey.startsWith("sk-")) return@withContext null

            val weakPrompt = if (weakWords.isNotEmpty()) {
                "Remediate these weak vocabulary/grammar items: ${weakWords.joinToString(", ")}."
            } else {
                "Focus on high-yield communicative proficiency."
            }

            val prompt = """
                You are Kiki AI, an expert Microlearning Pedagogical Designer.
                Create an Atomic Microlearning Unit (3-5 minutes) for the Single Learning Objective (SLO): "$topic" at level "$level".
                $weakPrompt

                You MUST return ONLY a raw JSON object (no markdown, no backticks) with this exact schema:
                {
                  "title": "$topic",
                  "description": "Atomic 3C Microlearning unit focused on single objective.",
                  "level": "$level",
                  "subject": "English",
                  "orderNo": 0,
                  "questions": [
                    {
                      "questionText": "Multiple choice question testing $topic",
                      "questionType": "MULTIPLE_CHOICE",
                      "optionsJson": "[\"option A\", \"option B\", \"option C\", \"option D\"]",
                      "correctAnswer": "0",
                      "explanation": "Detailed pedagogical explanation",
                      "points": 10,
                      "difficulty": "EASY"
                    },
                    {
                      "questionText": "Fill in the blank to build correct syntax",
                      "questionType": "SENTENCE_BUILDER",
                      "optionsJson": "[\"correct\", \"wrong1\", \"wrong2\", \"wrong3\"]",
                      "correctAnswer": "0",
                      "explanation": "Sentence structure rule",
                      "points": 10,
                      "difficulty": "MEDIUM"
                    },
                    {
                      "questionText": "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
                      "questionType": "LISTENING_CHALLENGE",
                      "optionsJson": "[\"A clean conversational English sentence of 6 words\"]",
                      "correctAnswer": "0",
                      "explanation": "Listening comprehension and audio token reconstruction",
                      "points": 10,
                      "difficulty": "MEDIUM"
                    },
                    {
                      "questionText": "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng 🧩",
                      "questionType": "MATCHING_PAIRS",
                      "optionsJson": "[\"word1=nghia1\", \"word2=nghia2\", \"word3=nghia3\", \"word4=nghia4\"]",
                      "correctAnswer": "0",
                      "explanation": "Vocabulary pairing mastery",
                      "points": 10,
                      "difficulty": "MEDIUM"
                    },
                    {
                      "questionText": "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
                      "questionType": "SPEAKING_CHALLENGE",
                      "optionsJson": "[\"A natural fluent sentence for native audio dialog practice\"]",
                      "correctAnswer": "0",
                      "explanation": "Pronunciation cadence and speaking clarity",
                      "points": 10,
                      "difficulty": "HARD"
                    }
                  ]
                }
            """.trimIndent()

            val contents = listOf(GeminiModels.Content(role = "user", parts = listOf(GeminiModels.Part(text = prompt))))
            val systemInstruction = GeminiModels.createSystemInstruction(
                "You are Kiki AI, an expert Microlearning Pedagogical Designer. Always output strict JSON only, without markdown fences or outside explanations."
            )
            val request = GeminiModels.GenerateContentRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                generationConfig = GeminiModels.GenerationConfig(
                    temperature = 0.4,
                    maxOutputTokens = 2048,
                    topP = 0.95,
                    topK = 40
                )
            )

            val response = geminiApiService.generateContent(GeminiModels.MODEL_GEMINI_2_5_FLASH, apiKey, request)
            if (response.isSuccessful) {
                val rawText = GeminiModels.extractResponseText(response.body()) ?: ""
                val cleanJson = rawText
                    .replace(Regex("^```(?:json)?\\s*"), "")
                    .replace(Regex("\\s*```$"), "")
                    .trim()

                val jsonObj = JSONObject(cleanJson)
                val title = jsonObj.optString("title", "Lộ trình AI: $topic")
                val desc = jsonObj.optString("description", "Bài học Microlearning 3C tối ưu theo mục tiêu học tập.")
                val questionsArr = jsonObj.optJSONArray("questions")
                val questionList = mutableListOf<QuestionItemDto>()

                if (questionsArr != null) {
                    for (i in 0 until questionsArr.length()) {
                        val qObj = questionsArr.getJSONObject(i)
                        questionList.add(
                            QuestionItemDto(
                                questionText = qObj.optString("questionText", "Question $i"),
                                questionType = qObj.optString("questionType", "MULTIPLE_CHOICE"),
                                optionsJson = qObj.optString("optionsJson", "[\"A\", \"B\"]"),
                                correctAnswer = qObj.optString("correctAnswer", "0"),
                                explanation = qObj.optString("explanation", "Giải thích"),
                                points = qObj.optInt("points", 10),
                                difficulty = qObj.optString("difficulty", "MEDIUM")
                            )
                        )
                    }
                }

                if (questionList.isNotEmpty()) {
                    return@withContext ChapterGeneratedResponse(
                        title = title,
                        description = desc,
                        level = level,
                        subject = "English",
                        orderNo = 0,
                        questions = questionList
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun exportResearchTelemetryCsv(
        context: Context,
        onResult: (Boolean, String, java.io.File?) -> Unit
    ) {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) {
            onResult(false, "Vui lòng đăng nhập để xuất dữ liệu nghiên cứu.", null)
            return
        }
        viewModelScope.launch {
            try {
                val list = repository.getAllTelemetry(userId)
                if (list.isEmpty()) {
                    onResult(false, "Chưa có dữ liệu đo lường thực nghiệm. Hãy làm thêm một số bài học và luyện tập thẻ!", null)
                    return@launch
                }
                val file = ResearchTelemetryExporter.saveCsvToFile(context, list)
                onResult(true, "Đã xuất thành công ${list.size} bản ghi dữ liệu thực nghiệm!", file)
            } catch (e: Exception) {
                onResult(false, "Lỗi khi xuất file CSV: ${e.localizedMessage ?: e.message}", null)
            }
        }
    }

    /**
     * Tự động hoặc chủ động gọi AI sinh chương học tiếp theo dựa trên Kho Điểm Yếu
     * Ưu tiên tuyệt đối: Bộ nhớ đệm dự đoán (Predictive Prefetched) -> Gemini 2.5 Flash -> Backend -> Fallback
     */
    fun generateNextChapterOnDemand(
        userId: Long? = _currentUserId.value,
        completedChapterTitle: String = _currentChapterTitle.value ?: "English Fundamentals",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val targetUserId = userId ?: return
        if (targetUserId == -1L) return

        _isGeneratingNextChapter.value = true
        viewModelScope.launch {
            try {
                // Chờ ngắn để đồng bộ các điểm yếu vừa ghi nhận nếu có
                delay(250)
                val weakPointsList = repository.getWeakPoints(targetUserId)
                val weakWords = extractWeakKeywords(weakPointsList)
                val chapters = _availableChapters.value?.takeIf { it.isNotEmpty() }
                    ?: repository.getChaptersByPreference(_selectedLevel.value, _selectedSubject.value)
                val targetLevel = _selectedLevel.value ?: "Beginner"
                val nextTopic = determineNextTopic(chapters, targetLevel)

                // 1. Kiểm tra bộ nhớ đệm nạp trước (Zero Latency UX)
                val prefetched = prefetchedMicrolearningCache.remove(nextTopic)
                if (prefetched != null) {
                    val newChapterId = repository.saveAiGeneratedChapter(prefetched)
                    if (newChapterId != -1L) {
                        refreshChapters()
                        _chapterUnlockEvent.postValue(
                            ChapterUnlockEvent(
                                completedChapterTitle = completedChapterTitle,
                                nextChapterTitle = prefetched.title,
                                isAiGenerated = true,
                                weakPointsAddressedCount = weakWords.size
                            )
                        )
                        onComplete(true, prefetched.title)
                        return@launch
                    }
                }

                // 2. Trực tiếp kích hoạt Gemini 2.5 Flash Microlearning Engine trên thiết bị
                val geminiChapter = generate3CMicrolearningWithGemini(nextTopic, targetLevel, weakWords)
                if (geminiChapter != null) {
                    val newChapterId = repository.saveAiGeneratedChapter(geminiChapter)
                    if (newChapterId != -1L) {
                        refreshChapters()
                        _chapterUnlockEvent.postValue(
                            ChapterUnlockEvent(
                                completedChapterTitle = completedChapterTitle,
                                nextChapterTitle = geminiChapter.title,
                                isAiGenerated = true,
                                weakPointsAddressedCount = weakWords.size
                            )
                        )
                        onComplete(true, geminiChapter.title)
                        return@launch
                    }
                }

                // 3. Fallback Backend API
                val token = authManager.getIdToken() ?: "mock_dev_token_kiki_explorer"
                val request = GenerateChapterRequest(
                    userId = targetUserId.toString(),
                    level = targetLevel,
                    topic = nextTopic,
                    weakWords = weakWords,
                    questionCount = 5
                )

                val response = backendApiService.generateChapter("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    val chapterData = response.body()!!
                    val newChapterId = repository.saveAiGeneratedChapter(chapterData)
                    if (newChapterId != -1L) {
                        refreshChapters()
                        _chapterUnlockEvent.postValue(
                            ChapterUnlockEvent(
                                completedChapterTitle = completedChapterTitle,
                                nextChapterTitle = chapterData.title,
                                isAiGenerated = true,
                                weakPointsAddressedCount = weakWords.size
                            )
                        )
                        onComplete(true, chapterData.title)
                    } else {
                        fallbackOfflineChapter(targetUserId, completedChapterTitle, nextTopic, targetLevel, weakWords)
                        onComplete(true, nextTopic)
                    }
                } else {
                    fallbackOfflineChapter(targetUserId, completedChapterTitle, nextTopic, targetLevel, weakWords)
                    onComplete(true, nextTopic)
                }
            } catch (e: Exception) {
                val chapters = _availableChapters.value ?: emptyList()
                val targetLevel = _selectedLevel.value ?: "Beginner"
                val nextTopic = determineNextTopic(chapters, targetLevel)
                val weakPointsList = repository.getWeakPoints(targetUserId)
                val weakWords = extractWeakKeywords(weakPointsList)
                fallbackOfflineChapter(targetUserId, completedChapterTitle, nextTopic, targetLevel, weakWords)
                onComplete(true, nextTopic)
            } finally {
                _isGeneratingNextChapter.postValue(false)
            }
        }
    }

    fun determineNextTopic(existingChapters: List<String>, level: String): String {
        val syllabus = listOf(
            "English Fundamentals",
            "Vocabulary & Daily Idioms",
            "Syntax & Sentence Mastery",
            "Conversational Fluency",
            "Academic & IELTS Mastery",
            "Professional & Business Communication",
            "Travel & Cultural Encounters",
            "Advanced Discourse & Critical Thinking",
            "Nuanced Collocations & Phrasal Verbs",
            "Mastery Summit & Fluency Capstone"
        )
        for (topic in syllabus) {
            if (!existingChapters.any { it.contains(topic, ignoreCase = true) }) {
                return topic
            }
        }
        return "Adaptive Mastery: Level $level Stage ${existingChapters.size + 1}"
    }

    fun extractWeakKeywords(weakPoints: List<WeakPointEntity>): List<String> {
        val keywords = mutableListOf<String>()
        val stringPattern = java.util.regex.Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"")
        for (wp in weakPoints) {
            try {
                val matcher = stringPattern.matcher(wp.optionsJson)
                val options = mutableListOf<String>()
                while (matcher.find()) {
                    val raw = matcher.group(1) ?: ""
                    options.add(raw.replace("\\\"", "\"").replace("\\\\", "\\").trim())
                }
                if (wp.correctIndex in 0 until options.size) {
                    val word = options[wp.correctIndex]
                    if (word.isNotBlank() && !keywords.contains(word)) {
                        keywords.add(word)
                    }
                } else {
                    val candidate = wp.question.split(" ").firstOrNull { it.length > 4 }
                    if (candidate != null && !keywords.contains(candidate)) {
                        keywords.add(candidate)
                    }
                }
            } catch (_: Exception) {
                val candidate = wp.question.split(" ").firstOrNull { it.length > 4 }
                if (candidate != null && !keywords.contains(candidate)) {
                    keywords.add(candidate)
                }
            }
            if (keywords.size >= 5) break
        }
        return keywords
    }

    private suspend fun fallbackOfflineChapter(
        userId: Long,
        completedChapterTitle: String,
        topic: String,
        level: String,
        weakWords: List<String>
    ) {
        val fallbackTitle = if (weakWords.isNotEmpty()) {
            "Reinforcement: $topic"
        } else {
            topic
        }

        val questions = (1..5).map { index ->
            val targetWord = weakWords.getOrNull(index - 1)
            val qText = if (targetWord != null) {
                "Reinforce target concept: Which sentence uses '$targetWord' correctly in context?"
            } else {
                "Challenge question $index for topic: $topic"
            }
            val options = if (targetWord != null) {
                listOf(
                    "This is the correct sentence utilizing '$targetWord' in proper context.",
                    "This sentence mistakenly uses '$targetWord' with incorrect grammar.",
                    "An alternative option that does not fit the context properly.",
                    "None of the above choices are correct."
                )
            } else {
                listOf(
                    "Correct contextual usage for $topic.",
                    "Incorrect phrasing.",
                    "Distractor choice.",
                    "Grammatically incomplete."
                )
            }
            val qType = when (index) {
                1 -> "MULTIPLE_CHOICE"
                2 -> "SENTENCE_BUILDER"
                3 -> "LISTENING_CHALLENGE"
                4 -> "MATCHING_PAIRS"
                5 -> "SPEAKING_CHALLENGE"
                else -> "MULTIPLE_CHOICE"
            }
            val finalOptions = when (qType) {
                "LISTENING_CHALLENGE" -> listOf("Consistent daily practice develops natural language intuition")
                "MATCHING_PAIRS" -> listOf("confidence=tự tin", "achievement=thành tựu", "progress=tiến bộ", "dedication=sự cống hiến")
                "SPEAKING_CHALLENGE" -> listOf("Consistency is the key to mastering English fluency.")
                else -> options
            }
            val finalQText = when (qType) {
                "LISTENING_CHALLENGE" -> "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧"
                "MATCHING_PAIRS" -> "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng 🧩"
                "SPEAKING_CHALLENGE" -> "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️"
                else -> qText
            }
            val optionsJson = org.json.JSONArray(finalOptions).toString()
            QuestionItemDto(
                questionText = finalQText,
                questionType = qType,
                optionsJson = optionsJson,
                correctAnswer = "0",
                explanation = "Lựa chọn phương án chính xác nhất để hoàn thiện câu và củng cố lỗ hổng kiến thức.",
                points = 10,
                difficulty = "MEDIUM"
            )
        }

        val generated = ChapterGeneratedResponse(
            title = fallbackTitle,
            description = "Chương học cá nhân hóa được tạo tự động nhằm củng cố kiến thức và mở rộng vốn từ.",
            level = level,
            subject = "English",
            orderNo = null,
            questions = questions
        )

        val newId = repository.saveAiGeneratedChapter(generated)
        if (newId != -1L) {
            refreshChapters()
            _chapterUnlockEvent.postValue(
                ChapterUnlockEvent(
                    completedChapterTitle = completedChapterTitle,
                    nextChapterTitle = fallbackTitle,
                    isAiGenerated = true,
                    weakPointsAddressedCount = weakWords.size
                )
            )
        }
    }

    fun addXp(amount: Int) {
        val oldLevel = _level.value ?: 1
        val newXp = (_xp.value ?: 0) + amount
        val newLevel = (newXp / 100) + 1
        _xp.value = newXp
        _level.value = newLevel
        _rankTitle.value = calculateRankTitle(newXp)

        if (newLevel > oldLevel) {
            soundEffectManager.playFanfare()
        }

        val userId = _currentUserId.value
        if (userId != null && userId != -1L) {
            viewModelScope.launch {
                repository.updateXP(userId, amount)
                fetchLeaderboard()
                firestoreSyncManager.syncUserProfile(userId)
            }
        }
    }

    fun checkAndUpdateStreak(userId: Long) {
        viewModelScope.launch {
            val profile = repository.getUserProfile(userId) ?: return@launch
            val todayStr = today()
            val lastDate = profile.lastActiveDate
            val currentStreak = profile.currentStreak
            val shields = profile.streakShields
            _streakShields.value = shields

            val newStreak = when {
                lastDate == null -> 1
                lastDate == todayStr -> currentStreak.coerceAtLeast(1)
                else -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    try {
                        val dLast = sdf.parse(lastDate)
                        val dToday = sdf.parse(todayStr)
                        if (dLast != null && dToday != null) {
                            val diffInDays = ((dToday.time - dLast.time) / (1000 * 60 * 60 * 24)).toInt()
                            when {
                                diffInDays == 1 -> currentStreak + 1
                                diffInDays == 0 -> currentStreak.coerceAtLeast(1)
                                shields > 0 -> {
                                    // Bùa bảo vệ chuỗi ngày (Streak Shield) kích hoạt tự động!
                                    val remainingShields = shields - 1
                                    repository.updateStreakShields(userId, remainingShields)
                                    _streakShields.value = remainingShields
                                    _shieldUsedNotice.value = true
                                    currentStreak.coerceAtLeast(1)
                                }
                                else -> 1
                            }
                        } else 1
                    } catch (_: Exception) {
                        1
                    }
                }
            }
            repository.updateStreak(userId, newStreak, todayStr)
            _streak.value = newStreak
            if (newStreak >= 7) {
                checkAndUnlockBadge(BadgeId.PERSISTENT_SCHOLAR)
            }
        }
    }

    fun refreshWeakPoints() {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) return
        viewModelScope.launch {
            _weakPoints.value = repository.getWeakPoints(userId)
        }
    }

    fun resolveWeakPoint(weakId: Long) {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) return
        viewModelScope.launch {
            repository.deleteWeakPoint(weakId)
            addXp(25)
            val curMana = _mana.value ?: 20
            setMana((curMana + 5).coerceAtMost(30))
            soundEffectManager.playCorrect(3)
            refreshWeakPoints()

            // Cập nhật tiến độ Bác Sĩ Trưởng
            val newCuredCount = prefs.getInt("cured_weak_points_count_$userId", 0) + 1
            prefs.edit().putInt("cured_weak_points_count_$userId", newCuredCount).apply()
            _curedWeakPointsCount.value = newCuredCount
            if (newCuredCount >= 10) {
                checkAndUnlockBadge(BadgeId.CHIEF_DOCTOR)
            }
        }
    }

    fun updateProfile(displayName: String, avatarUri: String?, motto: String?) {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) return
        viewModelScope.launch {
            repository.updateProfileInfo(userId, displayName.trim(), avatarUri, motto?.trim())
            _userName.value = displayName.trim()
            _avatarUri.value = avatarUri
            _studyMotto.value = motto?.trim() ?: ""
            fetchLeaderboard()
            firestoreSyncManager.syncUserProfile(userId)
        }
    }

    private val _dueVocabularyList = MutableLiveData<List<VocabularyEntity>>(emptyList())
    val dueVocabularyList: LiveData<List<VocabularyEntity>> = _dueVocabularyList

    fun refreshVocabulary() {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) return
        viewModelScope.launch {
            _vocabularyList.value = repository.getVocabularyList(userId)
            _dueVocabularyList.value = repository.getDueVocabularyList(userId)
        }
    }

    fun saveWord(word: String, phonetic: String = "", meaning: String, example: String = "") {
        val userId = _currentUserId.value ?: return
        if (userId == -1L) return
        viewModelScope.launch {
            repository.insertVocabulary(userId, word.trim(), phonetic.trim(), meaning.trim(), example.trim())
            refreshVocabulary()
            firestoreSyncManager.syncVocabulary(userId)
        }
    }

    fun reviewVocabularySm2(entity: VocabularyEntity, quality: Int, latencyMs: Long = 2500L) {
        viewModelScope.launch {
            val newState = repository.processVocabularySm2Review(entity.vocabId, quality, entity)
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                // Ghi nhận telemetry thẻ từ vựng phục vụ nghiên cứu
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val isDistracted = latencyMs > Sm2Algorithm.DISTRACTION_LATENCY_THRESHOLD_MS
                val retentionR = Sm2Algorithm.calculateRetentionProbability(0.0, newState.intervalDays.toDouble()).toFloat()

                val telemetry = ResearchTelemetryEntity(
                    sessionId = "VOCAB_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()),
                    userId = userId,
                    timestamp = nowStr,
                    sloTopic = "Vocabulary Vault",
                    challengeType = "FLASHCARD",
                    itemIdentifier = entity.word,
                    responseLatencyMs = latencyMs,
                    isCorrect = quality >= 3,
                    usedHint = false,
                    isDistracted = isDistracted,
                    objectiveQuality = quality,
                    easinessFactor = newState.easinessFactor,
                    intervalDays = newState.intervalDays,
                    retentionProbability = retentionR
                )
                repository.saveTelemetry(telemetry)

                refreshVocabulary()
                val masteredCount = repository.getMasteredVocabularyCount(userId)
                _masteredVocabCount.value = masteredCount
                if (masteredCount >= 30) {
                    checkAndUnlockBadge(BadgeId.SUPER_MEMORY)
                }
                val xpGain = when (quality) {
                    5 -> 25
                    4 -> 20
                    3 -> 15
                    else -> 10
                }
                addXp(xpGain)
                firestoreSyncManager.syncVocabulary(userId)
            }
        }
    }

    fun toggleVocabMastered(vocabId: Long, isMastered: Boolean) {
        viewModelScope.launch {
            repository.toggleVocabularyMastered(vocabId, isMastered)
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                _vocabularyList.value = repository.getVocabularyList(userId)
                _dueVocabularyList.value = repository.getDueVocabularyList(userId)
                val masteredCount = repository.getMasteredVocabularyCount(userId)
                _masteredVocabCount.value = masteredCount
                if (masteredCount >= 30) {
                    checkAndUnlockBadge(BadgeId.SUPER_MEMORY)
                }
            }
        }
    }

    fun deleteVocab(vocabId: Long) {
        viewModelScope.launch {
            repository.deleteVocabulary(vocabId)
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                _vocabularyList.value = repository.getVocabularyList(userId)
            }
        }
    }

    fun logout() {
        authManager.signOut()
        prefs.edit().remove("last_logged_in_email").apply()
        _currentUserId.value = -1L
        _userName.value = "Adventurer"
        _avatarUri.value = null
        _studyMotto.value = ""
        _history.value = emptyList()
        _dailyTasks.value = emptyList()
        _dailyChestOpened.value = false
    }

    private suspend fun fetchAnswerFromAI(
        context: Context,
        prompt: String,
        uri: Uri?
    ): String {
        val geminiKey = getEffectiveGeminiApiKey()
        if (geminiKey.isBlank()) {
            return "⚠️ Bạn chưa cài đặt GEMINI_API_KEY.\n\n• Vui lòng cấu hình biến GEMINI_API_KEY trong file .env để kích hoạt Gemini 2.5 Flash Native Audio Dialog.\n• Hoặc mở Cài đặt và dán khóa API của bạn."
        }

        val answer = fetchAnswerFromGemini(context, prompt, uri, apiKey = geminiKey)
        return sanitizeAiMentorReply(answer)
    }

    private suspend fun fetchAnswerFromGemini(
        context: Context,
        prompt: String,
        uri: Uri?,
        audioBytes: ByteArray? = null,
        audioMime: String = "audio/wav",
        apiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val effectiveKey = if (!apiKey.isNullOrBlank()) apiKey else getEffectiveGeminiApiKey()
            if (effectiveKey.isBlank() || effectiveKey.startsWith("sk-")) {
                return@withContext "⚠️ Khóa API không hợp lệ cho Google Gemini. Vui lòng cấu hình GEMINI_API_KEY trong file .env để sử dụng Gemini 2.5 Flash Native Audio Dialog."
            }

            val actualPrompt = if (prompt.isBlank() && uri != null) {
                "Hãy phân tích chi tiết và giải thích bài tập trong hình ảnh này."
            } else if (prompt.isBlank() && audioBytes != null) {
                "Lắng nghe đoạn ghi âm giọng nói tiếng Anh này của tôi, nhận xét phát âm, ngữ pháp và gợi ý cải thiện ngắn gọn."
            } else {
                prompt
            }

            // Bộ nhớ đệm InMemory (Zero latency & Zero token khi hỏi lặp lại trong phiên học)
            val cacheKey = "${actualPrompt.trim().lowercase()}_${uri != null}_${audioBytes != null}"
            aiQueryCache[cacheKey]?.let { cached ->
                return@withContext cached
            }

            val parts = mutableListOf<GeminiModels.Part>()
            if (actualPrompt.isNotBlank()) {
                parts.add(GeminiModels.Part(text = actualPrompt))
            }

            val base64Image = if (uri != null) uriToBase64(context, uri) else null
            if (base64Image != null) {
                parts.add(GeminiModels.Part(inlineData = GeminiModels.InlineData("image/jpeg", base64Image)))
            }

            if (audioBytes != null) {
                val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                parts.add(GeminiModels.Part(inlineData = GeminiModels.InlineData(audioMime, base64Audio)))
            }

            val contents = listOf(GeminiModels.Content(role = "user", parts = parts))
            val systemInstruction = GeminiModels.createSystemInstruction(AI_MENTOR_SYSTEM_PROMPT)

            val request = GeminiModels.GenerateContentRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                generationConfig = GeminiModels.GenerationConfig(
                    temperature = 0.6,
                    maxOutputTokens = 1024,
                    topP = 0.95,
                    topK = 40
                )
            )

            val targetModel = _geminiModel.value ?: DEFAULT_GEMINI_MODEL
            val candidateModels = (listOf(targetModel) + GeminiModels.SUPPORTED_MODELS).distinct()

            var lastError = ""
            var hasHighDemand = false

            for (model in candidateModels) {
                try {
                    val response = geminiApiService.generateContent(model, effectiveKey, request)
                    if (response.isSuccessful) {
                        val text = GeminiModels.extractResponseText(response.body())
                        if (!text.isNullOrBlank()) {
                            aiQueryCache[cacheKey] = text
                            return@withContext text
                        }
                    }
                    val code = response.code()
                    val errorDetail = response.errorBody()?.string() ?: "Code $code"
                    if (code == 400 && errorDetail.contains("API_KEY_INVALID")) {
                        lastError = "Khóa API không hợp lệ cho dịch vụ Google Gemini. Vui lòng kiểm tra lại khóa trong Cài đặt hoặc file .env."
                        break
                    }
                    lastError = "Gemini ($model): $errorDetail"

                    // Nếu gặp 503 (High demand) hoặc 429 (Rate limit tạm thời), delay ngắn và chuyển model tiếp theo
                    if (code == 503 || code == 429) {
                        hasHighDemand = true
                        kotlinx.coroutines.delay(200)
                    }

                    // Nếu khóa API sai (401/403) thì mới dừng vòng lặp
                    if (code == 401 || code == 403) {
                        break
                    }
                } catch (e: Exception) {
                    lastError = "Gemini Network Error ($model): ${e.localizedMessage}"
                }
            }

            if (hasHighDemand) {
                "⚡ Máy chủ AI Gemini hiện đang chịu tải cao tạm thời. Hệ thống đã tự động thử các mô hình Flash dự phòng. Bạn vui lòng thử lại sau vài giây."
            } else {
                "Lỗi kết nối Gemini API: $lastError"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Lỗi hệ thống/mạng Gemini: ${e.message}"
        }
    }

    private val phoneticCache = mutableMapOf<String, String>()

    suspend fun autoLookupPhonetic(word: String): String? = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isBlank()) return@withContext null
        if (phoneticCache.containsKey(cleanWord)) return@withContext phoneticCache[cleanWord]

        // 1. Try public dictionary API first
        try {
            val encoded = java.net.URLEncoder.encode(cleanWord, "UTF-8")
            val url = java.net.URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("\"text\"\\s*:\\s*\"(/[^\"]+/)\"").find(json)
                    ?: Regex("\"phonetic\"\\s*:\\s*\"(/[^\"]+/)\"").find(json)
                if (match != null) {
                    val result = match.groupValues[1]
                    phoneticCache[cleanWord] = result
                    return@withContext result
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback to Groq AI
        try {
            val apiKey = getEffectiveApiKey()
            if (apiKey.isNotBlank()) {
                val messages = listOf(
                    GroqModels.createTextMessage(
                        role = "system",
                        text = "Return ONLY the IPA phonetic transcription wrapped in slashes (e.g. /həˈloʊ/) for the given English word or phrase. No explanations, no markdown, no other text."
                    ),
                    GroqModels.createTextMessage(
                        role = "user",
                        text = cleanWord
                    )
                )
                val request = GroqModels.ChatRequest(
                    model = GroqModels.MODEL_TEXT,
                    messages = messages,
                    temperature = 0.1,
                    maxTokens = 30
                )
                val response = groqApiService.generateChatCompletion("Bearer $apiKey", request)
                if (response.isSuccessful && response.body() != null) {
                    val raw = response.body()?.choices?.firstOrNull()?.message?.content?.trim() ?: ""
                    val ipaMatch = Regex("/[^/]+/").find(raw)
                    val result = ipaMatch?.value ?: if (raw.startsWith("/") && raw.endsWith("/")) raw else if (raw.isNotBlank()) "/$raw/" else ""
                    if (result.length in 3..50) {
                        phoneticCache[cleanWord] = result
                        return@withContext result
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    private suspend fun fetchAnswerFromGroq(
        context: Context,
        prompt: String,
        uri: Uri?,
        apiKeyOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else getEffectiveApiKey()
            if (apiKey.isBlank()) {
                return@withContext "⚠️ Bạn chưa cài đặt AI API Key.\n\nVui lòng mở Cài đặt và dán API Key của bạn (hỗ trợ Groq, Gemini, Claude, OpenAI) hoặc cấu hình AI_API_KEY trong file .env."
            }

            // Cross-provider auto routing:
            if (apiKey.startsWith("AIza")) {
                return@withContext fetchAnswerFromGemini(context, prompt, uri, apiKey = apiKey)
            }
            if (apiKey.startsWith("sk-ant-")) {
                return@withContext fetchAnswerFromClaude(context, prompt, uri, apiKey)
            }
            if (apiKey.startsWith("sk-")) {
                return@withContext fetchAnswerFromOpenAI(context, prompt, uri, apiKey)
            }

            val actualPrompt = if (prompt.isBlank()) "Hãy phân tích chi tiết và giải thích bài tập trong hình ảnh này." else prompt

            val messages = mutableListOf<GroqModels.ChatMessage>()
            messages.add(
                GroqModels.createTextMessage(
                    role = "system",
                    text = AI_MENTOR_SYSTEM_PROMPT
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
                maxTokens = if (modelToUse == GroqModels.MODEL_VISION) 450 else 800
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

    private suspend fun fetchAnswerFromClaude(
        context: Context,
        prompt: String,
        uri: Uri?,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext "⚠️ Bạn chưa cài đặt Anthropic Claude API Key.\n\nVui lòng mở Cài đặt và dán API Key của bạn (bắt đầu bằng sk-ant-...)."
            }

            val actualPrompt = if (prompt.isBlank()) "Hãy phân tích chi tiết và giải thích bài tập tiếng Anh này." else prompt
            val systemPrompt = AI_MENTOR_SYSTEM_PROMPT

            val jsonBody = JSONObject().apply {
                put("model", "claude-3-5-sonnet-20241022")
                put("max_tokens", 2048)
                put("system", systemPrompt)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", actualPrompt)
                    })
                })
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = genericHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Lỗi kết nối Claude API (${response.code}): $bodyString"
            }

            val jsonResponse = JSONObject(bodyString)
            val contentArray = jsonResponse.optJSONArray("content")
            if (contentArray != null && contentArray.length() > 0) {
                return@withContext contentArray.getJSONObject(0).optString("text", "")
            }
            "Không nhận được nội dung phản hồi từ Claude API."
        } catch (e: Exception) {
            "Lỗi kết nối Claude API: ${e.localizedMessage}"
        }
    }

    private suspend fun fetchAnswerFromOpenAI(
        context: Context,
        prompt: String,
        uri: Uri?,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext "⚠️ Bạn chưa cài đặt OpenAI API Key.\n\nVui lòng mở Cài đặt và dán API Key của bạn (bắt đầu bằng sk-...)."
            }

            val actualPrompt = if (prompt.isBlank()) "Hãy phân tích chi tiết và giải thích bài tập tiếng Anh này." else prompt
            val systemPrompt = AI_MENTOR_SYSTEM_PROMPT

            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.7)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", actualPrompt)
                    })
                })
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = genericHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Lỗi kết nối OpenAI API (${response.code}): $bodyString"
            }

            val jsonResponse = JSONObject(bodyString)
            val choicesArray = jsonResponse.optJSONArray("choices")
            if (choicesArray != null && choicesArray.length() > 0) {
                return@withContext choicesArray.getJSONObject(0).optJSONObject("message")?.optString("content", "") ?: ""
            }
            "Không nhận được nội dung phản hồi từ OpenAI API."
        } catch (e: Exception) {
            "Lỗi kết nối OpenAI API: ${e.localizedMessage}"
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
    fun deleteAIQuestion(questionId: Long) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                repository.deleteAIQuestion(questionId)
                refreshHistory(userId)
            } else {
                val current = _history.value ?: emptyList()
                _history.value = current.filter { it.id.toLong() != questionId }
            }
        }
    }

    fun clearAIHistory() {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId != null && userId != -1L) {
                repository.clearAIHistory(userId)
                _history.value = emptyList()
            } else {
                _history.value = emptyList()
            }
        }
    }

    private suspend fun recordAdaptedWeakPoint(userId: Long, item: QuestItem) {
        when (item.type) {
            QuestionType.SENTENCE_BUILDER -> {
                val correctSentence = item.correctSentence
                if (correctSentence.isNotBlank()) {
                    val words = correctSentence.split(" ").filter { it.isNotBlank() }
                    val d1 = if (words.size > 2) {
                        words.toMutableList().apply {
                            val temp = this[0]
                            this[0] = this[1]
                            this[1] = temp
                        }.joinToString(" ")
                    } else "$correctSentence not"
                    val d2 = if (words.size > 3) {
                        words.toMutableList().apply {
                            val last = removeAt(size - 1)
                            add(0, last)
                        }.joinToString(" ")
                    } else "$correctSentence already"
                    val d3 = if (words.size > 1) {
                        words.shuffled().joinToString(" ")
                    } else "$correctSentence soon"

                    val rawOptions = listOf(correctSentence, d1, d2, d3).distinct()
                    val paddedOptions = if (rawOptions.size < 4) {
                        rawOptions + listOf("None of the above", "Incorrect word order", "Incomplete phrase").take(4 - rawOptions.size)
                    } else rawOptions.take(4)

                    val shuffled = paddedOptions.shuffled()
                    val newCorrectIdx = shuffled.indexOf(correctSentence).coerceAtLeast(0)
                    val qPrompt = "Sắp xếp & chọn câu đúng ngữ pháp: \"${item.question}\""
                    repository.recordWeakPoint(userId, qPrompt, shuffled, newCorrectIdx)
                }
            }
            QuestionType.MATCHING_PAIRS -> {
                val pair = item.matchingPairs.firstOrNull()
                if (pair != null) {
                    val correctMeaning = pair.vietnamese
                    val otherMeanings = repository.vocabularyPairsBank
                        .filter { it.vietnamese != correctMeaning }
                        .map { it.vietnamese }
                        .shuffled()
                        .take(3)
                    val allMeanings = (listOf(correctMeaning) + otherMeanings).distinct()
                    val padded = if (allMeanings.size < 4) {
                        allMeanings + listOf("không xác định", "nghĩa khác", "tất cả đều sai").take(4 - allMeanings.size)
                    } else allMeanings.take(4)
                    val shuffled = padded.shuffled()
                    val newCorrectIdx = shuffled.indexOf(correctMeaning).coerceAtLeast(0)
                    val qPrompt = "Từ vựng '${pair.english.replaceFirstChar { it.uppercase() }}' có nghĩa là gì?"
                    repository.recordWeakPoint(userId, qPrompt, shuffled, newCorrectIdx)
                }
            }
            QuestionType.SPEAKING_CHALLENGE -> {
                val sentence = item.speakingSentence.ifEmpty { item.question }
                if (sentence.isNotBlank()) {
                    val words = sentence.split(" ")
                    val d1 = if (words.size > 2) {
                        words.mapIndexed { i, w -> if (i == 1) "${w}s" else w }.joinToString(" ")
                    } else "$sentence too"
                    val d2 = if (words.size > 2) {
                        words.filterIndexed { i, _ -> i != words.size - 1 }.joinToString(" ")
                    } else "$sentence well"
                    val d3 = if (words.size > 3) {
                        words.mapIndexed { i, w -> if (i == 2) "${w}ed" else w }.joinToString(" ")
                    } else "$sentence now"

                    val allOpts = listOf(sentence, d1, d2, d3).distinct()
                    val padded = if (allOpts.size < 4) {
                        allOpts + listOf("Incorrect grammar pattern", "Incomplete pronunciation", "Misplaced word").take(4 - allOpts.size)
                    } else allOpts.take(4)

                    val shuffled = padded.shuffled()
                    val newCorrectIdx = shuffled.indexOf(sentence).coerceAtLeast(0)
                    val qPrompt = "Luyện phát âm & chọn câu chuẩn: \"$sentence\""
                    repository.recordWeakPoint(userId, qPrompt, shuffled, newCorrectIdx)
                }
            }
            else -> {
                if (item.options.size >= 4) {
                    repository.recordWeakPoint(userId, item.question, item.options, item.correctIndex)
                } else if (item.options.isNotEmpty()) {
                    val correctText = item.options.getOrNull(item.correctIndex) ?: item.options.first()
                    val distractors = listOf("All of the above", "None of the above", "Cannot be determined")
                    val combined = (item.options + distractors).distinct().take(4).shuffled()
                    val newIdx = combined.indexOf(correctText).coerceAtLeast(0)
                    repository.recordWeakPoint(userId, item.question, combined, newIdx)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
        speechRecognitionManager.destroy()
        soundEffectManager.release()
    }
}
