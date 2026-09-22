package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object GroqModels {

    const val MODEL_TEXT = "llama-3.3-70b-versatile"
    const val MODEL_VISION = "llama-3.2-11b-vision-preview"

    @JsonClass(generateAdapter = true)
    data class ChatRequest(
        @param:Json(name = "model") val model: String = MODEL_TEXT,
        @param:Json(name = "messages") val messages: List<ChatMessage>,
        @param:Json(name = "temperature") val temperature: Double = 0.7,
        @param:Json(name = "max_tokens") val maxTokens: Int = 1024
    )

    @JsonClass(generateAdapter = true)
    data class ChatMessage(
        @param:Json(name = "role") val role: String,
        @param:Json(name = "content") val content: Any
    )

    @JsonClass(generateAdapter = true)
    data class ChatResponse(
        @param:Json(name = "choices") val choices: List<Choice>? = null
    )

    @JsonClass(generateAdapter = true)
    data class Choice(
        @param:Json(name = "message") val message: ResponseMessage
    )

    @JsonClass(generateAdapter = true)
    data class ResponseMessage(
        @param:Json(name = "role") val role: String,
        @param:Json(name = "content") val content: String
    )

    fun createTextMessage(role: String, text: String): ChatMessage {
        return ChatMessage(role = role, content = text)
    }

    fun createVisionMessage(role: String, text: String, base64ImageJpeg: String): ChatMessage {
        val parts = listOf(
            mapOf("type" to "text", "text" to text),
            mapOf(
                "type" to "image_url",
                "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64ImageJpeg")
            )
        )
        return ChatMessage(role = role, content = parts)
    }
}
