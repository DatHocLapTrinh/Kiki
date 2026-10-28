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
import com.example.model.ClozeChoiceSlot
import com.example.model.SentenceBuilderSlot
import com.example.model.ListeningChallengeSlot
import com.example.model.PairSlotItem
import com.example.model.MatchingPairsSlot
import com.example.model.SpeakingChallengeSlot
import com.example.model.InjectedAtomicLessonUnit
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

    private val _currentChapterTitle = MutableLiveData("Chặng 1: Chào Hỏi & Làm Quen Cơ Bản (Hello & Daily Greetings)")
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

            // Thanh trừng triệt để bất kỳ câu hỏi mẫu cũ nào còn sót lại HOẶC nếu chapter chưa đủ 25 câu (kẹt 5 câu cũ)
            if (allChapterQuestions.any { it.question.contains("______") || it.question.contains("in her crib", ignoreCase = true) || it.question.contains("by bus every morning", ignoreCase = true) }
                || (allChapterQuestions.isNotEmpty() && allChapterQuestions.size < 25)) {
                repository.clearQuestionsByChapter(chapterId)
                allChapterQuestions = emptyList()
            }

            // JIT (Just-In-Time) AI Microlearning Question Generation:
            // Sinh trọn vẹn 25 câu hỏi (5 bài học x 5 câu) phân tầng từ từ vựng đơn đến hội thoại
            if (allChapterQuestions.isEmpty()) {
                val currentTitle = repository.getChapterTitleById(chapterId)
                    ?: _currentChapterTitle.value
                    ?: "Chặng 1: Chào Hỏi & Làm Quen Cơ Bản (Hello & Daily Greetings)"
                val level = _selectedLevel.value ?: "Beginner"
                val userId = _currentUserId.value ?: 0L
                val weakPointsList = if (userId > 0) repository.getWeakPoints(userId) else emptyList()
                val weakWords = extractWeakKeywords(weakPointsList)

                val generated = generate3CMicrolearningWithGemini(currentTitle, level, weakWords)
                    ?.takeIf { it.questions.size >= 25 }
                    ?: generatePersonalizedChapter(level, currentTitle, weakWords)

                generated.questions.forEach { qDto ->
                    val question = com.example.sqlite.room.QuestionEntity(
                        chapterId = chapterId,
                        questionText = qDto.questionText,
                        questionType = qDto.questionType ?: "MULTIPLE_CHOICE",
                        optionsJson = qDto.optionsJson,
                        correctAnswer = qDto.correctAnswer,
                        explanation = qDto.explanation,
                        points = qDto.points,
                        difficulty = qDto.difficulty ?: "MEDIUM"
                    )
                    repository.insertQuestion(question)
                }
                allChapterQuestions = repository.getQuestionsByChapter(chapterId)
            }

            startLesson(lessonIndex)
        }
    }

    fun startLesson(lessonIndex: Int) {
        val safeLesson = lessonIndex.coerceIn(1, 5)
        val pageSize = 5 // Tiêu chuẩn 3C Microlearning: 5 câu hỏi nguyên tử / bài học
        val rawStartIndex = (safeLesson - 1) * pageSize
        if (allChapterQuestions.isNotEmpty()) {
            val safeStart = if (rawStartIndex in 0 until allChapterQuestions.size) rawStartIndex else (rawStartIndex % allChapterQuestions.size)
            val endIndex = (safeStart + pageSize).coerceAtMost(allChapterQuestions.size)
            _questions.value = allChapterQuestions.subList(safeStart, if (endIndex > safeStart) endIndex else allChapterQuestions.size)
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
                    val defaultChapters = repository.getChaptersByPreference(_selectedLevel.value ?: "Beginner", "English")
                    val defaultTitle = defaultChapters.firstOrNull() ?: "Atomic Microlearning"
                    val defId = repository.getChapterIdByTitle(defaultTitle)
                    if (defId != -1L) {
                        allChapterQuestions = repository.getQuestionsByChapter(defId)
                    }
                }

                if (allChapterQuestions.isNotEmpty()) {
                    val safeStart = if (rawStartIndex in 0 until allChapterQuestions.size) rawStartIndex else (rawStartIndex % allChapterQuestions.size)
                    val endIndex = (safeStart + pageSize).coerceAtMost(allChapterQuestions.size)
                    _questions.value = allChapterQuestions.subList(safeStart, if (endIndex > safeStart) endIndex else allChapterQuestions.size)
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

                // 1. ƯU TIÊN SỐ 1: GỌI TRỰC TIẾP GEMINI 2.5 FLASH NATIVE AUDIO MICROLEARNING ENGINE
                chapterData = generate3CMicrolearningWithGemini(topic, level, weakWords)

                // 2. Thử gọi Backend nếu Gemini chưa trả về
                if (chapterData == null) {
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
                            kotlinx.coroutines.withTimeoutOrNull(3000) {
                                backendApiService.generateChapter("Bearer $token", request)
                            }
                        }
                        if (response != null && response.isSuccessful && response.body() != null) {
                            chapterData = response.body()
                        }
                    } catch (_: Exception) {}
                }

                // 3. Fallback thông minh nội bộ
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
     * Kiến tạo đơn vị học cá nhân hóa bằng Hệ thống Khuôn Mẫu Rỗng Xoắn Ốc (Spiral Slot-Based Blueprint).
     * Phân hóa triệt để theo TỪNG CHỦ ĐỀ (Topic-Aware) và TỪNG CẤP ĐỘ CEFR (A0 -> A1 -> A2 -> B1 -> B2 -> C1),
     * đảm bảo mỗi chặng có đúng 25 câu hỏi tuần tự (5 bài học x 5 câu) theo quy tắc Bắc cầu (Scaffolding 5 bước).
     */
    fun generatePersonalizedChapter(
        level: String,
        topic: String,
        weakWords: List<String> = emptyList()
    ): ChapterGeneratedResponse {
        val isEng = _isEnglish.value ?: false
        return com.example.model.SpiralCurriculumBlueprint.generateTopicAwareChapter(
            level = level,
            topic = topic,
            weakWords = weakWords,
            isEng = isEng
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

            val isBeginnerLevel = level.equals("Beginner", ignoreCase = true) ||
                                  level.equals("A1", ignoreCase = true) ||
                                  level.equals("A0", ignoreCase = true)

            val isAdvancedLevel = level.equals("Advanced", ignoreCase = true) ||
                                  level.equals("C1", ignoreCase = true) ||
                                  level.equals("B2", ignoreCase = true)

            val topicSpecificGuidance = com.example.model.SpiralCurriculumBlueprint.buildTopicSpecificGeminiGuidance(level, topic)

            val levelQuestionConstraints = when {
                isBeginnerLevel -> """
                    PEDAGOGICAL CONSTRAINT: BEGINNER SPIRAL PROGRESSION (A0 -> A1 -> A2):
                    - Teach the specific vocabulary and sentences of the target topic "$topic" following the Topic Focus below, while recycling earlier basic words!
                    - Keep sentences short and clear (2 words in Lesson 1-2, 2-3 words in Lesson 3, 3-4 words in Lesson 4, 4-5 words in Lesson 5).
                    - MULTIPLE_CHOICE: Prompt in Vietnamese explaining a clear situation for "$topic".
                    - SENTENCE_BUILDER: Target sentence must match the lesson's scaffolding step (2 tokens in L1-L2, 3 tokens in L3, 4 tokens in L4, 4-5 tokens in L5).
                    - LISTENING_CHALLENGE: Short, clear audio sentence matching the target topic.
                    - MATCHING_PAIRS: 4 vocabulary/phrase pairs ("english=vietnamese") directly teaching "$topic".
                    - SPEAKING_CHALLENGE: Short word/phrase/sentence from "$topic".
                    - STRICTLY FORBIDDEN: No complex clauses, no idioms, no connected speech reductions (could've, gonna), no passive voice!
                """.trimIndent()
                isAdvancedLevel -> """
                    PEDAGOGICAL CONSTRAINT: ADVANCED LEARNER (B2-C1):
                    - Challenge the learner with sophisticated lexical nuances, academic idioms, syntactic inversion, diplomatic hedging, and complex sentence structures tailored to "$topic".
                    - MULTIPLE_CHOICE: Nuanced distinction between C1 academic or executive expressions.
                    - SENTENCE_BUILDER: 6-9 tokens with inverted syntax or strategic clauses.
                    - LISTENING_CHALLENGE: High-register executive or academic discourse.
                    - MATCHING_PAIRS: 4 advanced C1 collocations or terminology ("english=vietnamese").
                    - SPEAKING_CHALLENGE: Executive leadership cadence and persuasive rhetoric.
                """.trimIndent()
                else -> """
                    PEDAGOGICAL CONSTRAINT: INTERMEDIATE LEARNER (B1-B2):
                    - Focus on practical conversational fluency, natural collocations, phrasal verbs, polite indirect inquiries, and conditionals tailored to "$topic".
                    - MULTIPLE_CHOICE: Practical communicative or grammatical dilemma in everyday/work context.
                    - SENTENCE_BUILDER: 4-7 words forming a natural sentence for "$topic".
                    - LISTENING_CHALLENGE: Everyday conversational or workplace sentence.
                    - MATCHING_PAIRS: 4 practical B1-B2 collocations or phrasal verbs ("english=vietnamese").
                    - SPEAKING_CHALLENGE: Natural pronunciation of daily/workplace expressions.
                """.trimIndent()
            }

            val prompt = """
                You are Kiki AI, an expert Microlearning Pedagogical Designer modeled after Duolingo's Spiral Curriculum.
                Create a complete 5-Lesson Chapter Curriculum with exactly 25 progressive questions (5 lessons x 5 questions per lesson) for the topic: "$topic" at level "$level".
                $weakPrompt
                $levelQuestionConstraints
                $topicSpecificGuidance
                CRITICAL INSTRUCTION: NEVER use raw blanks or underscores like '______'. All sentences and questions must be complete, natural, and engaging.

                SCAFFOLDING LESSON STRUCTURE (EXACTLY 25 QUESTIONS SPECIFIC TO "$topic"):
                - Lesson 1 (Questions 1-5): Step 1 Foundation - Core single words/concepts of "$topic". 1 MULTIPLE_CHOICE, 1 SENTENCE_BUILDER, 1 LISTENING_CHALLENGE, 1 MATCHING_PAIRS (4 pairs), 1 SPEAKING_CHALLENGE.
                - Lesson 2 (Questions 6-10): Step 2 Expansion - Action verbs, modifiers, or reactions of "$topic". 1 MULTIPLE_CHOICE, 1 SENTENCE_BUILDER, 1 LISTENING_CHALLENGE, 1 MATCHING_PAIRS, 1 SPEAKING_CHALLENGE.
                - Lesson 3 (Questions 11-15): Step 3 Phrase & Chunk Assembly - 2-3 word collocations/phrases of "$topic". 1 MULTIPLE_CHOICE, 1 SENTENCE_BUILDER, 1 LISTENING_CHALLENGE, 1 MATCHING_PAIRS, 1 SPEAKING_CHALLENGE.
                - Lesson 4 (Questions 16-20): Step 4 Sentence Construction - Complete sentences applying "$topic". 1 MULTIPLE_CHOICE, 1 SENTENCE_BUILDER, 1 LISTENING_CHALLENGE, 1 MATCHING_PAIRS, 1 SPEAKING_CHALLENGE.
                - Lesson 5 (Questions 21-25): Step 5 Reflex Dialogue & Mastery - Real-world Q&A dialogue mastering "$topic". 1 MULTIPLE_CHOICE, 1 SENTENCE_BUILDER, 1 LISTENING_CHALLENGE, 1 MATCHING_PAIRS, 1 SPEAKING_CHALLENGE.

                You MUST return ONLY a raw JSON object (no markdown, no backticks) with this exact schema:
                {
                  "title": "$topic",
                  "description": "5-Lesson progressive microlearning roadmap designed by Kiki AI.",
                  "level": "$level",
                  "subject": "English",
                  "orderNo": 0,
                  "questions": [
                    {
                      "questionText": "Prompt in Vietnamese or clear English instruction",
                      "questionType": "MULTIPLE_CHOICE",
                      "optionsJson": "[\"option A\", \"option B\", \"option C\", \"option D\"]",
                      "correctAnswer": "0",
                      "explanation": "Pedagogical explanation in Vietnamese",
                      "points": 10,
                      "difficulty": "EASY"
                    }
                  ]
                }
            """.trimIndent()

            val contents = listOf(GeminiModels.Content(role = "user", parts = listOf(GeminiModels.Part(text = prompt))))
            val systemInstruction = GeminiModels.createSystemInstruction(
                "You are Kiki AI, an expert Microlearning Pedagogical Designer. Always output strict JSON only, without markdown fences or outside explanations. NEVER generate raw underscores or blanks. Strictly teach the vocabulary and grammar of the requested topic '$topic' at CEFR level '$level' across all 25 progressive questions (5 lessons x 5 questions)."
            )
            val request = GeminiModels.GenerateContentRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                generationConfig = GeminiModels.GenerationConfig(
                    temperature = 0.4,
                    maxOutputTokens = 4096,
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
                val title = jsonObj.optString("title", topic).ifBlank { topic }
                val desc = jsonObj.optString("description", "Bài học Microlearning 3C tối ưu theo mục tiêu học tập.")
                val questionsArr = jsonObj.optJSONArray("questions")
                val questionList = mutableListOf<QuestionItemDto>()

                if (questionsArr != null) {
                    for (i in 0 until questionsArr.length()) {
                        val qObj = questionsArr.getJSONObject(i)
                        val qText = qObj.optString("questionText", "").trim()
                        if (qText.isBlank() || qText.contains("______")) continue
                        val rawOpts = qObj.opt("optionsJson") ?: qObj.opt("options")
                        val finalOptionsJson = when (rawOpts) {
                            is org.json.JSONArray -> rawOpts.toString()
                            is String -> rawOpts
                            else -> "[\"A\", \"B\"]"
                        }
                        questionList.add(
                            QuestionItemDto(
                                questionText = qText,
                                questionType = qObj.optString("questionType", "MULTIPLE_CHOICE"),
                                optionsJson = finalOptionsJson,
                                correctAnswer = qObj.optString("correctAnswer", "0"),
                                explanation = qObj.optString("explanation", "Giải thích chi tiết từ Kiki AI."),
                                points = qObj.optInt("points", 10),
                                difficulty = qObj.optString("difficulty", "MEDIUM")
                            )
                        )
                    }
                }

                if (questionList.isNotEmpty()) {
                    // Smart Hybrid Supplement: nếu Gemini trả về ít hơn 25 câu (do giới hạn token),
                    // tự động bù đắp các bài học còn thiếu từ khung chương trình chuẩn theo đúng chủ đề (Topic-Aware Blueprint)
                    val completeQuestions = if (questionList.size >= 25) {
                        questionList.take(25)
                    } else {
                        val blueprint = generatePersonalizedChapter(level, topic, weakWords).questions
                        (questionList + blueprint.drop(questionList.size)).take(25)
                    }
                    return@withContext ChapterGeneratedResponse(
                        title = title,
                        description = desc,
                        level = level,
                        subject = "English",
                        orderNo = 0,
                        questions = completeQuestions
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun generate5StageMicrolearningRoadmapWithGemini(
        profile: LearnerPersonaProfile
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveGeminiApiKey()
            if (apiKey.isBlank() || apiKey.startsWith("sk-")) return@withContext getScientificDefaultStages(profile)

            val weakPrompt = if (profile.baselineWeakPoints.isNotEmpty()) {
                "Remediate these diagnosed weak points in early stages: ${profile.baselineWeakPoints.joinToString(", ")}."
            } else {
                "Focus on accelerating conversational fluency and natural language intuition."
            }

            val isBeginner = profile.evaluatedCefr.equals("Beginner", ignoreCase = true) ||
                             profile.evaluatedCefr.equals("A1", ignoreCase = true) ||
                             profile.evaluatedCefr.equals("A0", ignoreCase = true) ||
                             profile.thetaAbility < -0.3f ||
                             profile.selfEfficacy.equals("NEWCOMER", ignoreCase = true)

            val isAdvanced = profile.evaluatedCefr.equals("Advanced", ignoreCase = true) ||
                             profile.evaluatedCefr.equals("B2", ignoreCase = true) ||
                             profile.evaluatedCefr.equals("C1", ignoreCase = true) ||
                             profile.thetaAbility >= 0.7f

            val levelPedagogyGuidance = when {
                isBeginner -> """
                    CRITICAL PEDAGOGICAL LEVEL CONSTRAINT: ZERO-KNOWLEDGE BEGINNER (A0-A1).
                    - The learner knows ALMOST ZERO ENGLISH (bắt đầu học từ con số 0).
                    - Stage 1 MUST start with basic greetings & introduction: "Hello", "Hi", "My name is...", "Nice to meet you".
                    - Stage 2 MUST cover personal status & asking: "How are you", "I am fine", "Thank you".
                    - Stage 3 MUST focus on essential daily items & requests: "Water", "Coffee", "Please", "Excuse me".
                    - Stage 4 MUST cover basic numbers 1-10 & simple time/price asking.
                    - Stage 5 MUST synthesize short 2-3 word daily dialogues.
                    - STRICT NEGATIVE CONSTRAINT: DO NOT teach idioms, DO NOT teach connected speech/elision/nuốt âm (could've/gonna), DO NOT teach complex clauses or inversion! Keep vocabulary strictly elementary.
                """.trimIndent()
                isAdvanced -> """
                    CRITICAL PEDAGOGICAL LEVEL CONSTRAINT: ADVANCED (B2-C1).
                    - The learner has high English proficiency and demands advanced nuances, idioms, connected speech, elision, debate arguments, and executive eloquence.
                    - Stage 1: Nuanced idiom & academic collocation precision.
                    - Stage 2: Complex syntactic inversion & subjunctive discourse.
                    - Stage 3: Connected speech decoding, native reductions (could've, gonna), and cadence.
                    - Stage 4: Strategic negotiation & persuasive argumentation.
                    - Stage 5: Executive fluency & high-stakes communication synthesis.
                """.trimIndent()
                else -> """
                    CRITICAL PEDAGOGICAL LEVEL CONSTRAINT: INTERMEDIATE (B1-B2).
                    - The learner has basic English knowledge and wants to build conversational flow, phrasal verbs, polite indirect questions, and work/travel dialogues.
                    - Stage 1: Active conversational starters & daily collocations.
                    - Stage 2: High-frequency phrasal verbs & natural expressions.
                    - Stage 3: Polite indirect inquiries & listening comprehension.
                    - Stage 4: Storytelling & past/future discourse.
                    - Stage 5: Spontaneous conversation synthesis.
                """.trimIndent()
            }

            val prompt = """
                You are Kiki AI, an elite Microlearning Pedagogical Curriculum Architect.
                Design a 5-Stage Scientific Competency Roadmap for a learner with:
                - Goal: "${profile.goal}"
                - Target CEFR Level: "${profile.evaluatedCefr}"
                - Ability parameter Theta: ${profile.thetaAbility}
                - Pacing: ${profile.dailyTargetMinutes} minutes/day (Atomic Microlearning)
                - Modality: ${profile.priorityModality}
                $weakPrompt

                $levelPedagogyGuidance

                Return ONLY a raw JSON array of 5 objects (no markdown, no backticks) with this exact schema:
                [
                  {
                    "title": "Stage 1 Short Title in Vietnamese with English subtitle",
                    "description": "Pedagogical single learning objective in Vietnamese (1 concise sentence)"
                  },
                  {
                    "title": "Stage 2 Short Title in Vietnamese with English subtitle",
                    "description": "Pedagogical single learning objective in Vietnamese (1 concise sentence)"
                  },
                  {
                    "title": "Stage 3 Short Title in Vietnamese with English subtitle",
                    "description": "Pedagogical single learning objective in Vietnamese (1 concise sentence)"
                  },
                  {
                    "title": "Stage 4 Short Title in Vietnamese with English subtitle",
                    "description": "Pedagogical single learning objective in Vietnamese (1 concise sentence)"
                  },
                  {
                    "title": "Stage 5 Short Title in Vietnamese with English subtitle",
                    "description": "Pedagogical single learning objective in Vietnamese (1 concise sentence)"
                  }
                ]
            """.trimIndent()

            val contents = listOf(GeminiModels.Content(role = "user", parts = listOf(GeminiModels.Part(text = prompt))))
            val systemInstruction = GeminiModels.createSystemInstruction(
                "You are Kiki AI, an expert Microlearning Curriculum Designer. Return ONLY raw JSON array containing exactly 5 stage objects. Strictly tailor the curriculum difficulty to the specified CEFR level: beginners MUST start from basic greetings (Hello/Hi/My name is) and must NOT receive idioms or connected speech."
            )
            val request = GeminiModels.GenerateContentRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                generationConfig = GeminiModels.GenerationConfig(
                    temperature = 0.3,
                    maxOutputTokens = 1024,
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

                val jsonArr = org.json.JSONArray(cleanJson)
                val stages = mutableListOf<Pair<String, String>>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val title = obj.optString("title", "Chặng ${i + 1}")
                    val desc = obj.optString("description", "Bài học Microlearning 3C tối ưu theo mục tiêu học.")
                    stages.add(Pair(title, desc))
                }
                if (stages.size >= 3) return@withContext stages
            }
        } catch (_: Exception) {}

        return@withContext getScientificDefaultStages(profile)
    }

    fun getScientificDefaultStages(profile: LearnerPersonaProfile): List<Pair<String, String>> {
        val isBeginner = profile.evaluatedCefr.equals("Beginner", ignoreCase = true) ||
                         profile.evaluatedCefr.equals("A1", ignoreCase = true) ||
                         profile.evaluatedCefr.equals("A0", ignoreCase = true) ||
                         profile.thetaAbility < -0.3f ||
                         profile.selfEfficacy.equals("NEWCOMER", ignoreCase = true)

        if (isBeginner) {
            return when (profile.goal) {
                "Career & Business" -> listOf(
                    Pair("Chặng 1: Chào Hỏi Đồng Nghiệp & Làm Quen Cơ Bản (Office Greetings & Intro)", "Bắt đầu từ số 0 với Hello, Hi, Good morning, My name is nơi công sở."),
                    Pair("Chặng 2: Giới Thiệu Chức Danh & Phòng Ban (Job Titles & Who I Am)", "Học nói về tên và công việc đơn giản: I am a / My job is..."),
                    Pair("Chặng 3: Đồ Dùng Văn Phòng & Nhờ Vả Cơ Bản (Office Items & Polite Requests)", "Nhờ vả đơn giản với Please, Thank you, Coffee, Water, Laptop."),
                    Pair("Chặng 4: Số Đếm, Giờ Giấc & Lịch Hẹn Đơn Giản (Numbers, Time & Schedules)", "Làm quen số đếm 1-100 và hỏi giờ họp đơn giản: What time is the meeting?"),
                    Pair("Chặng 5: Đàm Thoại Ngắn Phản Xạ Nơi Làm Việc (Short Workplace Dialogues)", "Phản xạ giao tiếp 2-3 câu hàng ngày tự tin không vấp.")
                )
                "Grammar & Vocabulary" -> listOf(
                    Pair("Chặng 1: Làm Quen Từ Vựng & Lời Chào Số 0 (Greetings & Daily Words)", "Bắt đầu từ số 0: Hello, Hi, Thank you, Yes, No, các từ căn bản nhất."),
                    Pair("Chặng 2: Đại Từ Nhân Xưng & Động Từ To Be (I am, You are, He is)", "Nắm vững bản chất I am, You are, He is với tên và nghề nghiệp cơ bản."),
                    Pair("Chặng 3: Đồ Vật Quanh Ta & Số Ít Số Nhiều (This, That, A, An)", "Nhận biết danh từ hàng ngày: a book, a pen, this is, that is."),
                    Pair("Chặng 4: Động Từ Hành Động Hàng Ngày (Action Verbs: Go, Like, Eat)", "Tạo câu 3 từ đơn giản: I like coffee, I go to work."),
                    Pair("Chặng 5: Hỏi Đáp Đơn Giản Có/Không (Basic Yes/No Questions)", "Tự tin hỏi và trả lời câu hỏi Yes/No ngắn: Are you ready? Do you like tea?")
                )
                "IELTS & TOEIC" -> listOf(
                    Pair("Chặng 1: Chào Hỏi & Giới Thiệu Bản Thân Đơn Giản (Speaking Part 1 Starter)", "Làm quen cách nói tên, tuổi và quê quán bằng các câu ngắn 3-4 từ."),
                    Pair("Chặng 2: Từ Vựng Gia Đình & Sở Thích Số 0 (Family & Basic Hobbies)", "Học từ vựng và câu đơn giản mô tả người thân và sở thích hàng ngày."),
                    Pair("Chặng 3: Nhận Diện Chữ Cái & Số Đếm Nghe Cơ Bản (Letters & Numbers Dictation)", "Luyện nghe bắt số điện thoại, ngày tháng và đánh vần tên cơ bản."),
                    Pair("Chặng 4: Mô Tả Đồ Vật & Vị Trí Đơn Giản (Describing Objects & Places)", "Cấu trúc There is / There are và các giới từ vị trí cơ bản in, on, at."),
                    Pair("Chặng 5: Phản Xạ Trả Lời Câu Hỏi Ngắn (Short Q&A Reflex)", "Tự tin trả lời 3 câu hỏi phỏng vấn ngắn quen thuộc trong đời sống.")
                )
                else -> listOf(
                    Pair("Chặng 1: Chào Hỏi & Làm Quen Cơ Bản (Hello & Daily Greetings)", "Bắt đầu từ số 0 với các từ chào hỏi kinh điển Hello, Hi, Thank you, My name is."),
                    Pair("Chặng 2: Giới Thiệu Bản Thân & Hỏi Thăm (Personal Identity & How Are You)", "Tự tin giới thiệu bản thân và hỏi thăm bạn bè: How are you, I am fine."),
                    Pair("Chặng 3: Nhu Cầu & Đồ Vật Thiết Yếu (Everyday Essentials & Requests)", "Giao tiếp cơ bản khi xin phép, cảm ơn và gọi nước, đồ ăn hàng ngày."),
                    Pair("Chặng 4: Số Đếm, Thời Gian & Mua Sắm Cơ Bản (Numbers, Time & Basic Requests)", "Làm quen số đếm 1-10, hỏi giờ và hỏi giá đơn giản."),
                    Pair("Chặng 5: Đàm Thoại Ngắn Phản Xạ Đời Sống (Short Daily Dialogue & Mastery)", "Phản xạ giao tiếp tự nhiên với các hội thoại ngắn đời thường 2-3 câu.")
                )
            }
        }

        return when (profile.goal) {
            "Grammar & Vocabulary" -> listOf(
                Pair("Chặng 1: Củng Cố Cấu Trúc Cốt Lõi (Core Syntax)", "Xóa bỏ lỗi chia động từ và hòa hợp chủ vị cơ bản."),
                Pair("Chặng 2: Làm Chủ Câu Điều Kiện Giả Định (Conditionals)", "Nắm vững bản chất câu điều kiện loại 2 và 3 trong giao tiếp."),
                Pair("Chặng 3: Đảo Ngữ & Trạng Từ Nhấn Mạnh (Inversion)", "Sử dụng các trạng từ phủ định đảo ngữ để nâng cao sắc thái biểu đạt."),
                Pair("Chặng 4: Mệnh Đề Rút Gọn & Cú Pháp Nâng Cao (Reduced Clauses)", "Rút gọn mệnh đề quan hệ và mệnh đề trạng ngữ linh hoạt."),
                Pair("Chặng 5: Tổng Hợp Ngữ Pháp Đỉnh Cao (Mastery Synthesis)", "Chinh phục bài tập phản xạ ngữ pháp tổng hợp toàn diện.")
            )
            "IELTS & TOEIC" -> listOf(
                Pair("Chặng 1: Nền Tảng Từ Vựng Học Thuật (Academic Lexicon)", "Làm chủ 20 collocations học thuật xuất hiện thường trực trong đề thi."),
                Pair("Chặng 2: Nhận Diện Cấu Trúc Câu Phức Đề Thi (Complex Structures)", "Phân tích và dựng câu phức chuẩn mực cho bài thi viết và đọc."),
                Pair("Chặng 3: Kỹ Thuật Nghe Bắt Ý Chính (Audio Gist Extraction)", "Rèn luyện tai nhận diện bẫy âm thanh và từ đồng nghĩa trong bài nghe."),
                Pair("Chặng 4: Diễn Đạt Ý Nghĩa Đa Chiều (Paraphrasing Mastery)", "Kỹ năng biến đổi từ loại và cấu trúc câu tương đương tức thì."),
                Pair("Chặng 5: Luyện Đề Bứt Phá Band Điểm (Exam Mastery Simulation)", "Thử thách phản xạ câu hỏi đề thi chuẩn hóa thời gian thực.")
            )
            "Career & Business" -> listOf(
                Pair("Chặng 1: Giao Tiếp Công Sở & Email Chuẩn Mực (Workplace Etiquette)", "Sử dụng văn phong trang trọng và lịch thiệp trong trao đổi công việc."),
                Pair("Chặng 2: Đàm Thoại & Thuyết Trình Dự Án (Business Pitching)", "Cấu trúc hóa luận điểm và trình bày báo cáo ngắn gọn, thuyết phục."),
                Pair("Chặng 3: Thảo Luận & Từ Chối Khéo Léo (Professional Diplomacy)", "Nghệ thuật phản hồi bất đồng và thương lượng trong cuộc họp."),
                Pair("Chặng 4: Đàm Phán & Thuyết Phục Đối Tác (Strategic Negotiation)", "Vận dụng các thuật ngữ nhượng bộ và chốt thỏa thuận kinh doanh."),
                Pair("Chặng 5: Khẩu Ngữ Lãnh Đạo Chuyên Nghiệp (Executive Fluency)", "Nâng tầm phong thái phát âm và giao tiếp bản lĩnh.")
            )
            else -> listOf(
                Pair("Chặng 1: Khởi Động & Phản Xạ Đàm Thoại (Daily Greetings)", "Tự tin mở đầu câu chuyện và phản xạ chào hỏi tự nhiên không vấp."),
                Pair("Chặng 2: Giải Mã Nuốt Âm & Nối Âm (Connected Speech & Elision)", "Làm chủ hiện tượng nuốt âm could've, gonna trong tiếng Anh bản xứ."),
                Pair("Chặng 3: Đặt Câu Hỏi Gián Tiếp Lịch Sự (Polite Indirect Inquiries)", "Giao tiếp lịch thiệp khi hỏi đường, nhờ vả và gọi món."),
                Pair("Chặng 4: Cụm Động Từ Thông Dụng (High-Frequency Phrasal Verbs)", "Sử dụng phrasal verbs tự nhiên thay cho các từ đơn điệu."),
                Pair("Chặng 5: Đột Phá Đàm Thoại Trôi Chảy (Mastery Conversational Fluency)", "Phản xạ giao tiếp Native Audio trôi chảy trong mọi ngữ cảnh.")
            )
        }
    }

    fun initializeAiMicrolearningRoadmap(
        userId: Long,
        profile: LearnerPersonaProfile,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // 1. Sinh 5 chặng lộ trình bằng Gemini 2.5 Flash
                val stages = generate5StageMicrolearningRoadmapWithGemini(profile)

                // 2. Sinh đơn vị bài học 3C đầu tiên cho Chặng 1 bằng Gemini 2.5 Flash
                val firstStageTitle = stages.firstOrNull()?.first ?: profile.goal
                val chapter1Units = generate3CMicrolearningWithGemini(
                    topic = firstStageTitle,
                    level = profile.evaluatedCefr,
                    weakWords = profile.baselineWeakPoints
                ) ?: generatePersonalizedChapter(profile.evaluatedCefr, firstStageTitle, profile.baselineWeakPoints)

                // 3. Xóa các chapter tĩnh cũ và nạp lộ trình 100% AI vào Room SQLite
                val targetUserId = if (userId > 0) userId else _currentUserId.value ?: 0L
                repository.resetWithAiMicrolearningRoadmap(
                    level = profile.evaluatedCefr,
                    stages = stages,
                    firstChapterUnits = chapter1Units
                )

                // 4. Lưu hồ sơ cá nhân hóa
                saveLearnerPersonaProfile(profile)

                // 5. Cập nhật trạng thái
                refreshChapters()

                // 6. Prefetch trước Chặng 2 ngầm bằng Gemini 2.5 Flash
                if (stages.size > 1) {
                    val secondStageTitle = stages[1].first
                    prefetchNextMicrolearningUnit(targetUserId, secondStageTitle)
                }

                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
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
     * Tự động thăng cấp trình độ CEFR (Automatic Spiral Level-Up) khi người học hoàn thành các cột mốc chặng:
     * - Beginner (A0-A1): Sau khi hoàn thành >= 6 chặng nền tảng (150 câu hỏi từ vựng, câu ngắn, hỏi đường),
     *   tự động thăng cấp lên Intermediate (B1-B2).
     * - Intermediate (B1-B2): Sau khi hoàn thành >= 10 chặng (250 câu hỏi),
     *   tự động thăng cấp lên Advanced (B2-C1).
     */
    fun resolveProgressedLevel(existingChaptersCount: Int, currentLevel: String): String {
        val isBeginner = currentLevel.equals("Beginner", ignoreCase = true) ||
                         currentLevel.equals("A1", ignoreCase = true) ||
                         currentLevel.equals("A0", ignoreCase = true)
        val isIntermediate = currentLevel.equals("Intermediate", ignoreCase = true) ||
                             currentLevel.equals("B1", ignoreCase = true) ||
                             currentLevel.equals("A2", ignoreCase = true)

        return when {
            isBeginner && existingChaptersCount >= 6 -> "Intermediate"
            isIntermediate && existingChaptersCount >= 10 -> "Advanced"
            currentLevel.isBlank() -> "Beginner"
            else -> currentLevel
        }
    }

    /**
     * Tự động hoặc chủ động gọi AI sinh chương học tiếp theo dựa trên Kho Điểm Yếu & Lộ Trình Xoắn Ốc
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
                val rawLevel = _selectedLevel.value ?: "Beginner"
                val targetLevel = resolveProgressedLevel(chapters.size, rawLevel)
                if (!targetLevel.equals(rawLevel, ignoreCase = true)) {
                    _selectedLevel.postValue(targetLevel)
                }
                val nextTopic = determineNextTopic(chapters, targetLevel)

                // 1. Kiểm tra bộ nhớ đệm nạp trước (Zero Latency UX)
                val prefetched = prefetchedMicrolearningCache.remove(nextTopic)?.takeIf { it.questions.size >= 25 }
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
                    questionCount = 25
                )

                val response = backendApiService.generateChapter("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    val rawData = response.body()!!
                    val chapterData = if (rawData.questions.size >= 25) {
                        rawData
                    } else {
                        val blueprint = generatePersonalizedChapter(targetLevel, nextTopic, weakWords).questions
                        rawData.copy(questions = (rawData.questions + blueprint.drop(rawData.questions.size)).take(25))
                    }
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
                val rawLevel = _selectedLevel.value ?: "Beginner"
                val targetLevel = resolveProgressedLevel(chapters.size, rawLevel)
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
        val progressedLevel = resolveProgressedLevel(existingChapters.size, level)
        val isBeginner = progressedLevel.equals("Beginner", ignoreCase = true) ||
                         progressedLevel.equals("A1", ignoreCase = true) ||
                         progressedLevel.equals("A0", ignoreCase = true)
        val isAdvanced = progressedLevel.equals("Advanced", ignoreCase = true) ||
                         progressedLevel.equals("C1", ignoreCase = true) ||
                         progressedLevel.equals("B2", ignoreCase = true)

        val beginnerSyllabus = listOf(
            "Chào Hỏi & Làm Quen Cơ Bản (Hello & Daily Greetings)",
            "Giới Thiệu Bản Thân & Hỏi Thăm (Personal Identity & How Are You)",
            "Nhu Cầu & Đồ Vật Thiết Yếu (Everyday Essentials & Requests)",
            "Số Đếm, Thời Gian & Mua Sắm Cơ Bản (Numbers, Time & Basic Requests)",
            "Đàm Thoại Ngắn Phản Xạ Đời Sống (Short Daily Dialogue & Mastery)",
            "Hỏi Đường & Nơi Chốn Đơn Giản (Places & Directions)"
        )

        val intermediateSyllabus = listOf(
            "Giải Mã Nuốt Âm & Nối Âm (Connected Speech & Elision)",
            "Đặt Câu Hỏi Gián Tiếp Lịch Sự (Polite Indirect Inquiries)",
            "Cụm Động Từ Thông Dụng (High-Frequency Phrasal Verbs)",
            "Làm Chủ Câu Điều Kiện Giả Định (Conditionals & Complex Syntax)",
            "Giao Tiếp Công Sở & Email Chuẩn Mực (Workplace Etiquette)",
            "Đàm Thoại & Thuyết Trình Dự Án (Business Pitching)"
        )

        val advancedSyllabus = listOf(
            "Đảo Ngữ & Trạng Từ Nhấn Mạnh (Inversion & Complex Syntax)",
            "Đàm Phán & Thuyết Phục Đối Tác (Strategic Negotiation & Diplomacy)",
            "Nền Tảng Từ Vựng Học Thuật (Academic & IELTS Lexicon)",
            "Khẩu Ngữ Lãnh Đạo Chuyên Nghiệp (Executive Fluency & Rhetoric)"
        )

        val fullSpiralCurriculum = when {
            isBeginner -> beginnerSyllabus + intermediateSyllabus + advancedSyllabus
            isAdvanced -> advancedSyllabus + intermediateSyllabus
            else -> intermediateSyllabus + advancedSyllabus
        }

        val stageNumber = existingChapters.size + 1
        for (topic in fullSpiralCurriculum) {
            val coreName = topic.substringBefore("(").trim()
            val alreadyExists = existingChapters.any { existing ->
                existing.contains(topic, ignoreCase = true) ||
                (coreName.length > 5 && existing.contains(coreName, ignoreCase = true))
            }
            if (!alreadyExists) {
                return "Chặng $stageNumber: $topic"
            }
        }
        return "Chặng $stageNumber: Đàm Phán & Thuyết Phục Đối Tác (Strategic Negotiation Stage $stageNumber)"
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
        val generated = generatePersonalizedChapter(level, topic, weakWords)
        val newId = repository.saveAiGeneratedChapter(generated)
        if (newId != -1L) {
            refreshChapters()
            _chapterUnlockEvent.postValue(
                ChapterUnlockEvent(
                    completedChapterTitle = completedChapterTitle,
                    nextChapterTitle = generated.title,
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
