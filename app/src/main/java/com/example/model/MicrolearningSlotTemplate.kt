package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Kiến trúc Khuôn Mẫu Rỗng (Slot-Based Question Templates) cho AI Microlearning
 * Ứng dụng đóng vai trò là Hệ thống Trình diễn Khuôn Mẫu (Template Presentation Engine).
 * AI Gemini đóng vai trò là Bộ Não Bơm Dữ Liệu Tức Thời (Real-time Payload Injector).
 */
object MicrolearningSlotArchetypes {
    const val CLOZE_CHOICE = "MULTIPLE_CHOICE"
    const val SENTENCE_BUILDER = "SENTENCE_BUILDER"
    const val LISTENING_CHALLENGE = "LISTENING_CHALLENGE"
    const val MATCHING_PAIRS = "MATCHING_PAIRS"
    const val SPEAKING_CHALLENGE = "SPEAKING_CHALLENGE"
}

internal fun listToJsonArray(items: List<String>): String {
    if (items.isEmpty()) return "[]"
    return items.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]") { item ->
        item.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}

@JsonClass(generateAdapter = true)
data class ClozeChoiceSlot(
    @param:Json(name = "promptQuestion") val promptQuestion: String,
    @param:Json(name = "options") val options: List<String>,
    @param:Json(name = "correctIndex") val correctIndex: Int = 0,
    @param:Json(name = "explanation") val explanation: String = "",
    @param:Json(name = "targetSkillTag") val targetSkillTag: String? = null
) {
    fun toQuestionItemDto(): QuestionItemDto {
        return QuestionItemDto(
            questionText = promptQuestion,
            questionType = MicrolearningSlotArchetypes.CLOZE_CHOICE,
            optionsJson = listToJsonArray(options),
            correctAnswer = correctIndex.toString(),
            explanation = explanation,
            points = 10,
            difficulty = "EASY"
        )
    }
}

@JsonClass(generateAdapter = true)
data class SentenceBuilderSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String,
    @param:Json(name = "targetSentence") val targetSentence: String,
    @param:Json(name = "wordTokens") val wordTokens: List<String>,
    @param:Json(name = "distractorTokens") val distractorTokens: List<String> = emptyList(),
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        val allTokens = (wordTokens + distractorTokens).shuffled()
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.SENTENCE_BUILDER,
            optionsJson = listToJsonArray(allTokens),
            correctAnswer = targetSentence,
            explanation = explanation,
            points = 15,
            difficulty = "MEDIUM"
        )
    }
}

@JsonClass(generateAdapter = true)
data class ListeningChallengeSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String = "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
    @param:Json(name = "audioSentence") val audioSentence: String,
    @param:Json(name = "wordTokens") val wordTokens: List<String> = emptyList(),
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        val tokens = if (wordTokens.isNotEmpty()) {
            wordTokens.shuffled()
        } else {
            audioSentence.split(Regex("\\s+"))
                .map { it.trim().trim('.', ',', '!', '?', ';', ':', '"') }
                .filter { it.isNotEmpty() }
                .shuffled()
        }
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.LISTENING_CHALLENGE,
            optionsJson = listToJsonArray(listOf(audioSentence)),
            correctAnswer = audioSentence,
            explanation = explanation,
            points = 10,
            difficulty = "MEDIUM"
        )
    }
}

@JsonClass(generateAdapter = true)
data class PairSlotItem(
    @param:Json(name = "english") val english: String,
    @param:Json(name = "vietnamese") val vietnamese: String
)

@JsonClass(generateAdapter = true)
data class MatchingPairsSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String = "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng 🧩",
    @param:Json(name = "pairs") val pairs: List<PairSlotItem>,
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        val formattedPairs = pairs.map { "${it.english}=${it.vietnamese}" }
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.MATCHING_PAIRS,
            optionsJson = listToJsonArray(formattedPairs),
            correctAnswer = "0",
            explanation = explanation,
            points = 10,
            difficulty = "MEDIUM"
        )
    }
}

@JsonClass(generateAdapter = true)
data class SpeakingChallengeSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String = "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
    @param:Json(name = "targetSentence") val targetSentence: String,
    @param:Json(name = "phoneticFocusWord") val phoneticFocusWord: String? = null,
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.SPEAKING_CHALLENGE,
            optionsJson = listToJsonArray(listOf(targetSentence)),
            correctAnswer = targetSentence,
            explanation = explanation,
            points = 20,
            difficulty = "HARD"
        )
    }
}

/**
 * Đơn vị Bài học Nguyên tử 3C gồm 5 Slot hoàn chỉnh được bơm dữ liệu động từ AI
 */
@JsonClass(generateAdapter = true)
data class InjectedAtomicLessonUnit(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "level") val level: String,
    @param:Json(name = "subject") val subject: String = "English",
    @param:Json(name = "slot1Cloze") val slot1Cloze: ClozeChoiceSlot,
    @param:Json(name = "slot2SentenceBuilder") val slot2SentenceBuilder: SentenceBuilderSlot,
    @param:Json(name = "slot3Listening") val slot3Listening: ListeningChallengeSlot,
    @param:Json(name = "slot4Matching") val slot4Matching: MatchingPairsSlot,
    @param:Json(name = "slot5Speaking") val slot5Speaking: SpeakingChallengeSlot
) {
    fun toQuestionsList(): List<QuestionItemDto> {
        return listOf(
            slot1Cloze.toQuestionItemDto(),
            slot2SentenceBuilder.toQuestionItemDto(),
            slot3Listening.toQuestionItemDto(),
            slot4Matching.toQuestionItemDto(),
            slot5Speaking.toQuestionItemDto()
        )
    }

    fun toChapterGeneratedResponse(orderNo: Int = 1): ChapterGeneratedResponse {
        return ChapterGeneratedResponse(
            title = title,
            description = description,
            level = level,
            subject = subject,
            orderNo = orderNo,
            questions = toQuestionsList()
        )
    }
}

