package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateChapterRequest(
    @param:Json(name = "userId") val userId: String? = null,
    @param:Json(name = "level") val level: String,
    @param:Json(name = "topic") val topic: String,
    @param:Json(name = "weakWords") val weakWords: List<String> = emptyList(),
    @param:Json(name = "questionCount") val questionCount: Int = 5
)

@JsonClass(generateAdapter = true)
data class ChapterGeneratedResponse(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "level") val level: String? = null,
    @param:Json(name = "subject") val subject: String? = null,
    @param:Json(name = "orderNo") val orderNo: Int? = null,
    @param:Json(name = "questions") val questions: List<QuestionItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuestionItemDto(
    @param:Json(name = "questionText") val questionText: String,
    @param:Json(name = "questionType") val questionType: String? = null,
    @param:Json(name = "optionsJson") val optionsJson: String,
    @param:Json(name = "correctAnswer") val correctAnswer: String,
    @param:Json(name = "explanation") val explanation: String? = null,
    @param:Json(name = "points") val points: Int = 10,
    @param:Json(name = "difficulty") val difficulty: String? = "MEDIUM"
)

@JsonClass(generateAdapter = true)
data class MentorChatRequest(
    @param:Json(name = "message") val message: String,
    @param:Json(name = "userLevel") val userLevel: String? = null,
    @param:Json(name = "currentChapter") val currentChapter: String? = null,
    @param:Json(name = "recentWeakPoints") val recentWeakPoints: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MentorChatResponse(
    @param:Json(name = "reply") val reply: String,
    @param:Json(name = "grammarTip") val grammarTip: String? = null,
    @param:Json(name = "detectedVocabularies") val detectedVocabularies: List<VocabularyCardDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VocabularyCardDto(
    @param:Json(name = "word") val word: String,
    @param:Json(name = "phonetic") val phonetic: String? = null,
    @param:Json(name = "meaning") val meaning: String,
    @param:Json(name = "example") val example: String? = null
)

@JsonClass(generateAdapter = true)
data class CosmicCallTurnDto(
    @param:Json(name = "role") val role: String,
    @param:Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class CosmicCallRequest(
    @param:Json(name = "userAnswer") val userAnswer: String,
    @param:Json(name = "currentQuestion") val currentQuestion: String? = null,
    @param:Json(name = "planetOrTopic") val planetOrTopic: String? = null,
    @param:Json(name = "userLevel") val userLevel: String? = null,
    @param:Json(name = "conversationHistory") val conversationHistory: List<CosmicCallTurnDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CosmicCallResponse(
    @param:Json(name = "score") val score: Int,
    @param:Json(name = "spokenFeedbackEn") val spokenFeedbackEn: String,
    @param:Json(name = "feedbackVi") val feedbackVi: String,
    @param:Json(name = "grammarCorrection") val grammarCorrection: String? = null,
    @param:Json(name = "nextQuestionEn") val nextQuestionEn: String? = null,
    @param:Json(name = "nextHintVi") val nextHintVi: String? = null
)
