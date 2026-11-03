package com.example.model

enum class QuestionType {
    MULTIPLE_CHOICE,
    SENTENCE_BUILDER,
    MATCHING_PAIRS,
    SPEAKING_CHALLENGE,
    LISTENING_CHALLENGE  // Nghe câu và sắp xếp lại các từ
}

enum class QuestionDifficulty {
    VERY_EASY, // 0 cơ hội thử lại (sai là mất tim & qua câu)
    EASY,      // 1 cơ hội thử lại (sai 1 lần có cơ hội sửa)
    MEDIUM,    // 2 cơ hội thử lại
    HARD       // 3 cơ hội thử lại (câu khó cho thử lại 2-3 lần)
}

data class MatchingPair(
    val id: Int,
    val english: String,
    val vietnamese: String
)

data class QuestItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    var selectedIndex: Int = -1,
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val sentenceTokens: List<String> = emptyList(),
    val correctSentence: String = "",
    var userSentenceTokens: List<String> = emptyList(),
    val matchingPairs: List<MatchingPair> = emptyList(),
    val speakingSentence: String = "",
    var pronunciationScore: Int = 0,
    val difficulty: QuestionDifficulty = when {
        type == QuestionType.SPEAKING_CHALLENGE || type == QuestionType.SENTENCE_BUILDER -> QuestionDifficulty.HARD
        type == QuestionType.LISTENING_CHALLENGE || type == QuestionType.MATCHING_PAIRS -> QuestionDifficulty.MEDIUM
        options.size <= 2 -> QuestionDifficulty.VERY_EASY
        else -> QuestionDifficulty.EASY
    }
) {
    val maxRetries: Int
        get() = when (difficulty) {
            QuestionDifficulty.VERY_EASY -> 0
            QuestionDifficulty.EASY -> 1
            QuestionDifficulty.MEDIUM -> 2
            QuestionDifficulty.HARD -> 3
        }
}

