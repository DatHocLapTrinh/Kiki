package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

class GeminiModels {

    @JsonClass(generateAdapter = true)
    data class GenerateContentRequest(
        @Json(name = "model") val model: String = "llama-3.3-70b-versatile",
        @Json(name = "messages") val messages: List<Message>,
        @Json(name = "temperature") val temperature: Double = 0.7,
        @Json(name = "max_tokens") val maxTokens: Int = 1024
    )

    @JsonClass(generateAdapter = true)
    data class Message(
        @Json(name = "role") val role: String,
        @Json(name = "content") val content: String
    )

    @JsonClass(generateAdapter = true)
    data class GenerateContentResponse(
        @Json(name = "choices") val choices: List<Choice>? = null
    )

    @JsonClass(generateAdapter = true)
    data class Choice(
        @Json(name = "message") val message: Message
    )

    // --- Giữ lại để tương thích với code cũ ---
    @JsonClass(generateAdapter = true)
    data class Content @JvmOverloads constructor(
        @Json(name = "parts") val parts: List<Part> = emptyList(),
        @Json(name = "role") val role: String = "user"
    )

    @JsonClass(generateAdapter = true)
    data class Part(
        @Json(name = "text") val text: String? = null,
        @Json(name = "inline_data") val inlineData: InlineData? = null
    )

    @JsonClass(generateAdapter = true)
    data class InlineData(
        @Json(name = "mime_type") val mimeType: String,
        @Json(name = "data") val data: String
    )
}
