package com.example.model

enum class QuestionType {
    MULTIPLE_CHOICE,
    SENTENCE_BUILDER,
    MATCHING_PAIRS,
    SPEAKING_CHALLENGE
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
    var pronunciationScore: Int = 0
)

