package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

class GeminiModels {

    @JsonClass(generateAdapter = true)
    data class GenerateContentRequest(
        @Json(name = "contents") val contents: List<Content>,
        @Json(name = "system_instruction") val systemInstruction: Content? = null
    )

    @JsonClass(generateAdapter = true)
    data class Content @JvmOverloads constructor(
        @Json(name = "parts") val parts: List<Part>,
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

    @JsonClass(generateAdapter = true)
    data class GenerateContentResponse(
        @Json(name = "candidates") val candidates: List<Candidate>? = null
    )

    @JsonClass(generateAdapter = true)
    data class Candidate(
        @Json(name = "content") val content: Content
    )
}
