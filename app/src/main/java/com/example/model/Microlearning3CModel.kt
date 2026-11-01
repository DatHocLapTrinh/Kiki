package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Cấu trúc bài học Microlearning 3C (Concept - Check - Consolidate) chuẩn quốc tế:
 * - Concept: Khái niệm nguyên tử cô đọng dưới 60 từ + 1 ví dụ trực quan.
 * - Check: 3-5 thử thách truy xuất chủ động (Active Retrieval).
 * - Consolidate: Củng cố, tóm tắt ghi nhớ sâu.
 */
@JsonClass(generateAdapter = true)
data class Microlearning3CUnit(
    @param:Json(name = "slo") val slo: String,
    @param:Json(name = "conceptExplanation") val conceptExplanation: String,
    @param:Json(name = "conceptExample") val conceptExample: String,
    @param:Json(name = "challenges") val challenges: List<MicroChallengeItem> = emptyList(),
    @param:Json(name = "consolidationTip") val consolidationTip: String,
    @param:Json(name = "estimatedMinutes") val estimatedMinutes: Int = 3
)

@JsonClass(generateAdapter = true)
data class MicroChallengeItem(
    @param:Json(name = "challengeType") val challengeType: String, // MULTIPLE_CHOICE, SENTENCE_BUILDER, LISTENING_CHALLENGE, MATCHING_PAIRS, SPEAKING_CHALLENGE
    @param:Json(name = "prompt") val prompt: String,
    @param:Json(name = "options") val options: List<String> = emptyList(),
    @param:Json(name = "correctIndex") val correctIndex: Int = 0,
    @param:Json(name = "explanation") val explanation: String = "",
    @param:Json(name = "correctSentence") val correctSentence: String? = null,
    @param:Json(name = "speakingSentence") val speakingSentence: String? = null
)
