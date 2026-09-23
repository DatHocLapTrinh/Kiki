package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Singleton
class SoundEffectManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_study_mentor_prefs", Context.MODE_PRIVATE)

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    // Pre-rendered audio buffers
    private val correctTrack: AudioTrack by lazy { createTrack(generateCorrectPcm()) }
    private val incorrectTrack: AudioTrack by lazy { createTrack(generateIncorrectPcm()) }
    private val fanfareTrack: AudioTrack by lazy { createTrack(generateFanfarePcm()) }
    private val clickTrack: AudioTrack by lazy { createTrack(generateClickPcm()) }

    private fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", true)
    }

    fun playCorrect() {
        if (!isSoundEnabled()) return
        scope.launch {
            playTrack(correctTrack)
        }
    }

    fun playIncorrect() {
        if (!isSoundEnabled()) return
        scope.launch {
            playTrack(incorrectTrack)
        }
    }

    fun playFanfare() {
        if (!isSoundEnabled()) return
        scope.launch {
            playTrack(fanfareTrack)
        }
    }

    fun playClick() {
        if (!isSoundEnabled()) return
        scope.launch {
            playTrack(clickTrack)
        }
    }

    private fun playTrack(track: AudioTrack) {
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {
            // Audio hardware fallback handling
        }
    }

    private fun createTrack(pcmData: ShortArray): AudioTrack {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(pcmData.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcmData, 0, pcmData.size)
        return track
    }

    /**
     * Âm thanh Ding vui tươi khi chọn đúng (2 nốt A5 -> E6 ngân dài dịu êm)
     */
    private fun generateCorrectPcm(): ShortArray {
        val duration = 0.32f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 8.5).toFloat()
            // Nốt 1 (0 -> 120ms): 880Hz, Nốt 2 (100ms -> hết): 1318Hz
            val freq = if (t < 0.10f) 880.0 else 1318.51
            val sample = sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * (freq * 2) * t) * 0.2
            buffer[i] = (sample * env * 32767 * 0.75).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    /**
     * Âm thanh báo sai nhẹ nhàng, ấm (220Hz trượt xuống 180Hz)
     */
    private fun generateIncorrectPcm(): ShortArray {
        val duration = 0.22f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 9.0).toFloat()
            val freq = 220.0 - (t / duration) * 40.0
            val sample = sin(2.0 * PI * freq * t) * 0.8
            buffer[i] = (sample * env * 32767 * 0.65).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    /**
     * Âm thanh rộn ràng khi hoàn thành màn / mở rương (Hợp âm C-E-G-C)
     */
    private fun generateFanfarePcm(): ShortArray {
        val duration = 0.45f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = when {
                t < 0.08f -> 523.25 // C5
                t < 0.16f -> 659.25 // E5
                t < 0.24f -> 783.99 // G5
                else -> 1046.50     // C6
            }
            val localT = if (t >= 0.24f) t - 0.24f else t % 0.08f
            val decayRate = if (t >= 0.24f) 6.0 else 14.0
            val env = exp(-localT * decayRate).toFloat()
            val sample = sin(2.0 * PI * freq * t) * 0.75 + sin(2.0 * PI * (freq * 1.5) * t) * 0.15
            buffer[i] = (sample * env * 32767 * 0.8).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    /**
     * Âm thanh click tinh tế
     */
    private fun generateClickPcm(): ShortArray {
        val duration = 0.02f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 120.0).toFloat()
            val sample = sin(2.0 * PI * 1800.0 * t)
            buffer[i] = (sample * env * 32767 * 0.4).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }
}
