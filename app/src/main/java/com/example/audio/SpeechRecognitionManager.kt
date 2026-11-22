package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognitionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = mutableStateOf(false)
    val isListening: State<Boolean> = _isListening

    private val _rmsDb = mutableFloatStateOf(0f)
    val rmsDb: State<Float> = _rmsDb

    private val _partialText = mutableStateOf("")
    val partialText: State<String> = _partialText

    private val _candidateResults = mutableStateOf<List<String>>(emptyList())
    val candidateResults: State<List<String>> = _candidateResults

    private val _speechError = mutableStateOf<String?>(null)
    val speechError: State<String?> = _speechError

    private var onResultCallback: ((String) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    // Chế độ thu âm liên tục cho câu dài (không bị dừng khi tạm ngắt nghỉ)
    private var isContinuous: Boolean = false
    private var currentLanguage: String = "en-US"
    private val accumulatedSpokenText = java.lang.StringBuilder()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun createSpeechIntent(language: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            // Mở rộng thời gian chờ im lặng khi người dùng ngắt nghỉ suy nghĩ hoặc đọc câu dài (3.5s - 4.5s)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
        }
    }

    fun startListening(
        language: String = "en-US",
        continuous: Boolean = false,
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
        onError: (String) -> Unit = {}
    ) {
        onResultCallback = onResult
        onPartialResultCallback = onPartialResult
        onErrorCallback = onError
        isContinuous = continuous
        currentLanguage = language
        accumulatedSpokenText.setLength(0)

        _speechError.value = null
        _partialText.value = ""
        _candidateResults.value = emptyList()
        _rmsDb.floatValue = 0f

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer?.setRecognitionListener(createListener())
                speechRecognizer?.startListening(createSpeechIntent(language))
                _isListening.value = true
            } catch (e: Exception) {
                _isListening.value = false
                val err = "Microphone initialization error: ${e.localizedMessage}"
                _speechError.value = err
                onErrorCallback?.invoke(err)
            }
        }
    }

    fun stopListening() {
        isContinuous = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _rmsDb.floatValue = 0f
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancel() {
        isContinuous = false
        accumulatedSpokenText.setLength(0)
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _isListening.value = false
                _rmsDb.floatValue = 0f
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun destroy() {
        isContinuous = false
        accumulatedSpokenText.setLength(0)
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                _isListening.value = false
                _rmsDb.floatValue = 0f
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _speechError.value = null
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB ranges typically from -2 to 10+
                _rmsDb.floatValue = rmsdB.coerceAtLeast(0f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _rmsDb.floatValue = 0f
                if (!isContinuous) {
                    _isListening.value = false
                }
            }

            override fun onError(error: Int) {
                _rmsDb.floatValue = 0f

                // Nếu đang ở chế độ câu dài và gặp timeout do người dùng ngắt nghỉ lấy hơi, tự động tiếp tục lắng nghe vế tiếp theo
                if (isContinuous && _isListening.value && (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH)) {
                    mainHandler.postDelayed({
                        try {
                            if (_isListening.value && isContinuous) {
                                speechRecognizer?.startListening(createSpeechIntent(currentLanguage))
                            }
                        } catch (_: Exception) {}
                    }, 250)
                    return
                }

                _isListening.value = false

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly into the mic."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input. Please try speaking again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue for speech engine. Using offline recognition."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again."
                    else -> "Speech recognition error ($error). Please try again."
                }
                _speechError.value = errorMsg

                // Nếu đã thu được một phần câu từ trước, vẫn giữ lại để người dùng không bị mất lời đã nói!
                val existingText = if (accumulatedSpokenText.isNotEmpty()) accumulatedSpokenText.toString() else _partialText.value
                if (existingText.isNotBlank()) {
                    onResultCallback?.invoke(existingText)
                }
                onErrorCallback?.invoke(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                _rmsDb.floatValue = 0f

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                _candidateResults.value = matches
                val chunk = matches.firstOrNull() ?: ""

                if (isContinuous && _isListening.value) {
                    if (chunk.isNotBlank()) {
                        if (accumulatedSpokenText.isNotEmpty()) {
                            accumulatedSpokenText.append(" ")
                        }
                        accumulatedSpokenText.append(chunk)
                    }
                    val fullText = accumulatedSpokenText.toString().trim()
                    if (fullText.isNotBlank()) {
                        _partialText.value = fullText
                        onResultCallback?.invoke(fullText)
                        onPartialResultCallback?.invoke(fullText)
                    }

                    // Tiếp tục lắng nghe liền mạch vế tiếp theo mà không cắt đứt câu
                    mainHandler.post {
                        try {
                            if (_isListening.value && isContinuous) {
                                speechRecognizer?.startListening(createSpeechIntent(currentLanguage))
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    _isListening.value = false
                    val spokenText = if (accumulatedSpokenText.isNotEmpty()) {
                        if (chunk.isNotBlank()) accumulatedSpokenText.append(" ").append(chunk).toString().trim()
                        else accumulatedSpokenText.toString().trim()
                    } else {
                        chunk.ifBlank { _partialText.value }
                    }
                    _partialText.value = spokenText
                    onResultCallback?.invoke(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    val live = if (accumulatedSpokenText.isNotEmpty()) {
                        "${accumulatedSpokenText.toString().trim()} $text"
                    } else {
                        text
                    }
                    _partialText.value = live
                    onPartialResultCallback?.invoke(live)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
