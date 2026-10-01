package com.example

import com.example.model.QuestItem
import com.example.model.QuestionType
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SentenceBuilderTest {

    private fun normalize(s: String): String {
        return s.lowercase(Locale.ROOT)
            .replace(Regex("[.,!?;:\"]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    @Test
    fun testQuestItemDefaultInitialization() {
        val defaultItem = QuestItem(
            question = "Sample question",
            options = listOf("A", "B"),
            correctIndex = 0
        )

        assertEquals(QuestionType.MULTIPLE_CHOICE, defaultItem.type)
        assertTrue(defaultItem.sentenceTokens.isEmpty())
        assertEquals("", defaultItem.correctSentence)
        assertTrue(defaultItem.userSentenceTokens.isEmpty())
        assertEquals(-1, defaultItem.selectedIndex)
    }

    @Test
    fun testSentenceBuilderCreationAndEvaluation() {
        val targetSentence = "She goes to school by bus every morning."
        val tokens = listOf("She", "goes", "to", "school", "by", "bus", "every", "morning")
        val distractors = listOf("go", "going")
        val bank = (tokens + distractors).shuffled()

        val item = QuestItem(
            question = "She ______ to school by bus every morning.",
            options = listOf("go", "goes", "going", "is go"),
            correctIndex = 1,
            type = QuestionType.SENTENCE_BUILDER,
            sentenceTokens = bank,
            correctSentence = targetSentence
        )

        assertEquals(QuestionType.SENTENCE_BUILDER, item.type)
        assertEquals(targetSentence, item.correctSentence)
        assertTrue(item.sentenceTokens.containsAll(tokens))
        assertTrue(item.sentenceTokens.contains("go"))

        // Correct Assembly
        val userAssemblyCorrect = listOf("She", "goes", "to", "school", "by", "bus", "every", "morning")
        val isCorrect = normalize(userAssemblyCorrect.joinToString(" ")) == normalize(item.correctSentence)
        assertTrue("User sentence matching target sentence ignoring punctuation and case should be true", isCorrect)

        // Incorrect Assembly (with distractor)
        val userAssemblyWrong = listOf("She", "go", "to", "school", "by", "bus", "every", "morning")
        val isWrong = normalize(userAssemblyWrong.joinToString(" ")) == normalize(item.correctSentence)
        assertFalse("User sentence with wrong verb form should be false", isWrong)
    }

    @Test
    fun testSentenceBuilderResultsMapping() {
        val targetSentence = "They studied English for two hours yesterday."
        val item = QuestItem(
            question = "They ______ English for two hours yesterday.",
            options = listOf("study", "studied", "studying", "studies"),
            correctIndex = 0,
            type = QuestionType.SENTENCE_BUILDER,
            sentenceTokens = listOf("They", "studied", "English", "for", "two", "hours", "yesterday", "study"),
            correctSentence = targetSentence
        )

        // Simulate user correct answer
        val isItemCorrect = true
        val copyCorrect = QuestItem(
            question = item.question,
            options = if (isItemCorrect) listOf(item.correctSentence) else listOf(item.correctSentence, "wrong"),
            correctIndex = 0,
            type = item.type,
            sentenceTokens = item.sentenceTokens,
            correctSentence = item.correctSentence
        )
        copyCorrect.selectedIndex = if (isItemCorrect) 0 else 1

        assertEquals(copyCorrect.correctIndex, copyCorrect.selectedIndex)

        // Simulate user incorrect answer
        val isItemWrong = false
        val copyWrong = QuestItem(
            question = item.question,
            options = if (isItemWrong) listOf(item.correctSentence) else listOf(item.correctSentence, "They study English"),
            correctIndex = 0,
            type = item.type,
            sentenceTokens = item.sentenceTokens,
            correctSentence = item.correctSentence
        )
        copyWrong.selectedIndex = if (isItemWrong) 0 else 1

        assertNotEquals(copyWrong.correctIndex, copyWrong.selectedIndex)
        assertEquals("They study English", copyWrong.options[copyWrong.selectedIndex])
        assertEquals(targetSentence, copyWrong.options[copyWrong.correctIndex])
    }
}
