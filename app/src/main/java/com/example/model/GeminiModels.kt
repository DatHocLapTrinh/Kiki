package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object GeminiModels {

    const val MODEL_GEMINI_2_5_FLASH = "gemini-2.5-flash"
    const val MODEL_FALLBACK_DEFAULT = "gemini-flash-lite-latest"
    val SUPPORTED_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-flash-lite-latest",
        "gemini-2.0-flash",
        "gemini-2.5-flash-lite",
        "gemini-1.5-flash",
        "gemini-3.5-flash"
    )

    @JsonClass(generateAdapter = true)
    data class GenerateContentRequest(
        @param:Json(name = "contents") val contents: List<Content>,
        @param:Json(name = "systemInstruction") val systemInstruction: Content? = null,
        @param:Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
    )

    @JsonClass(generateAdapter = true)
    data class Content(
        @param:Json(name = "role") val role: String = "user",
        @param:Json(name = "parts") val parts: List<Part>
    )

    @JsonClass(generateAdapter = true)
    data class Part(
        @param:Json(name = "text") val text: String? = null,
        @param:Json(name = "inlineData") val inlineData: InlineData? = null
    )

    @JsonClass(generateAdapter = true)
    data class InlineData(
        @param:Json(name = "mimeType") val mimeType: String,
        @param:Json(name = "data") val data: String
    )

    @JsonClass(generateAdapter = true)
    data class GenerationConfig(
        @param:Json(name = "temperature") val temperature: Double = 0.7,
        @param:Json(name = "maxOutputTokens") val maxOutputTokens: Int = 2048,
        @param:Json(name = "topP") val topP: Double? = 0.95,
        @param:Json(name = "topK") val topK: Int? = 64
    )

    @JsonClass(generateAdapter = true)
    data class GenerateContentResponse(
        @param:Json(name = "candidates") val candidates: List<Candidate>? = null,
        @param:Json(name = "promptFeedback") val promptFeedback: PromptFeedback? = null
    )

    @JsonClass(generateAdapter = true)
    data class Candidate(
        @param:Json(name = "content") val content: Content? = null,
        @param:Json(name = "finishReason") val finishReason: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class PromptFeedback(
        @param:Json(name = "blockReason") val blockReason: String? = null
    )

    fun createTextContent(role: String, text: String): Content {
        return Content(role = role, parts = listOf(Part(text = text)))
    }

    fun createVisionContent(role: String, text: String, base64ImageJpeg: String): Content {
        val parts = mutableListOf<Part>()
        if (text.isNotBlank()) {
            parts.add(Part(text = text))
        }
        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64ImageJpeg)))
        return Content(role = role, parts = parts)
    }

    fun createAudioContent(role: String, text: String?, base64Audio: String, mimeType: String = "audio/wav"): Content {
        val parts = mutableListOf<Part>()
        if (!text.isNullOrBlank()) {
            parts.add(Part(text = text))
        }
        parts.add(Part(inlineData = InlineData(mimeType = mimeType, data = base64Audio)))
        return Content(role = role, parts = parts)
    }

    fun createSystemInstruction(text: String): Content {
        return Content(role = "system", parts = listOf(Part(text = text)))
    }

    /**
     * Extracts full response text cleanly from candidates.
     */
    fun extractResponseText(response: GenerateContentResponse?): String? {
        val candidate = response?.candidates?.firstOrNull() ?: return null
        return candidate.content?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("\n")
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * Formats AI response text cleanly for natural Text-To-Speech (Native Audio Dialog).
     * Strips phonetics slashes, bullets, markdown asterisks to make speech playback natural.
     */
    fun formatForAudioSpeech(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("/[^/\n]+/"), "") // Bỏ ký hiệu phiên âm IPA trong ngoặc /.../
            .replace(Regex("(?m)^\\s*•\\s*"), "") // Bỏ dấu chấm tròn đầu dòng
            .replace("•", "")
            .replace("[STEP]", "")
            .replace(Regex("[*#_`]"), "") // Bỏ ký tự markdown
            .replace(Regex("(?i)^Nghĩa:\\s*"), "Nghĩa là: ")
            .replace(Regex("(?i)^Ví dụ:\\s*"), "Ví dụ: ")
            .replace(Regex("(?i)^Từ đồng nghĩa:\\s*"), "Từ đồng nghĩa: ")
            .replace(Regex("(?i)^Từ trái nghĩa:\\s*"), "Từ trái nghĩa: ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
