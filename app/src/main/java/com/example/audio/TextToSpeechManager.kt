package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = mutableStateOf(false)
    val isSpeaking: State<Boolean> = _isSpeaking

    private val _currentSpeakingText = mutableStateOf<String?>(null)
    val currentSpeakingText: State<String?> = _currentSpeakingText

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(0.92f) // Tốc độ tự nhiên, rõ ràng cho người học tiếng Anh
                tts?.setPitch(1.0f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingText.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingText.value = null
                    }
                })
            }
        }
    }

    fun speak(text: String, isSlow: Boolean = false) {
        if (text.isBlank()) return
        val cleanText = sanitizeTextForSpeech(text)

        // Nếu đang đọc chính câu này thì bấm lần nữa để tạm dừng/ngắt
        if (_isSpeaking.value && _currentSpeakingText.value == cleanText) {
            stop()
            return
        }

        if (!isInitialized) {
            initTts()
        }

        // Điều chỉnh tốc độ phát âm (chậm 0.68f cho người mới / luyện nghe âm đuôi, chuẩn 0.92f)
        val rate = if (isSlow) 0.68f else 0.92f
        tts?.setSpeechRate(rate)

        _currentSpeakingText.value = cleanText
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSpeakingText.value = null
    }

    private fun sanitizeTextForSpeech(raw: String): String {
        return raw
            .replace(Regex("\\[STEP\\]"), " ")
            .replace(Regex("[*#_`~]"), "") // Loại bỏ markdown
            .replace(Regex("https?://\\S+"), "") // Loại bỏ url
            .trim()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
