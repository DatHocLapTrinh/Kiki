package com.example

import com.example.model.QuestItem
import com.example.model.QuestionType
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ListeningChallengeTest {

    private fun normalize(s: String): String {
        return s.lowercase(Locale.ROOT)
            .replace(Regex("[.,!?;:\"]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    @Test
    fun testListeningChallengeCreationAndAttributes() {
        val sentence = "They studied English for two hours yesterday"
        val tokens = listOf("They", "studied", "English", "for", "two", "hours", "yesterday")
        val distractors = listOf("study", "tomorrow")
        val availableBank = (tokens + distractors).shuffled()

        val item = QuestItem(
            question = "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
            options = listOf(sentence),
            correctIndex = 0,
            type = QuestionType.LISTENING_CHALLENGE,
            sentenceTokens = availableBank,
            correctSentence = sentence,
            speakingSentence = sentence
        )

        assertEquals(QuestionType.LISTENING_CHALLENGE, item.type)
        assertEquals(sentence, item.correctSentence)
        assertEquals(sentence, item.speakingSentence)
        assertTrue(item.sentenceTokens.containsAll(tokens))
    }

    @Test
    fun testListeningChallengeAnswerChecking() {
        val targetSentence = "She reads a book every single night."
        val item = QuestItem(
            question = "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
            options = listOf(targetSentence),
            correctIndex = 0,
            type = QuestionType.LISTENING_CHALLENGE,
            correctSentence = targetSentence,
            speakingSentence = targetSentence
        )

        // Perfect match
        val userTokensExact = listOf("She", "reads", "a", "book", "every", "single", "night.")
        val isExact = normalize(userTokensExact.joinToString(" ")) == normalize(item.correctSentence)
        assertTrue(isExact)

        // Case-insensitive match
        val userTokensCase = listOf("she", "reads", "A", "book", "Every", "Single", "night")
        val isCaseInsensitive = normalize(userTokensCase.joinToString(" ")) == normalize(item.correctSentence)
        assertTrue(isCaseInsensitive)

        // Wrong order
        val userTokensWrongOrder = listOf("reads", "she", "a", "book", "every", "single", "night")
        val isWrong = normalize(userTokensWrongOrder.joinToString(" ")) == normalize(item.correctSentence)
        assertFalse(isWrong)
    }

    @Test
    fun testAllQuestionTypesDistinct() {
        val types = QuestionType.values()
        assertTrue(types.contains(QuestionType.MULTIPLE_CHOICE))
        assertTrue(types.contains(QuestionType.SENTENCE_BUILDER))
        assertTrue(types.contains(QuestionType.MATCHING_PAIRS))
        assertTrue(types.contains(QuestionType.SPEAKING_CHALLENGE))
        assertTrue(types.contains(QuestionType.LISTENING_CHALLENGE))
        assertEquals(5, types.size)
    }
}
