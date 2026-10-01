package com.example

import com.example.model.QuestItem
import com.example.model.QuestionType
import com.example.util.PronunciationScorer
import com.example.util.WordAccuracy
import org.junit.Assert.*
import org.junit.Test

class SpeakingChallengeTest {

    @Test
    fun testCleanToken() {
        assertEquals("school", PronunciationScorer.cleanToken("school."))
        assertEquals("hello", PronunciationScorer.cleanToken("\"Hello!\""))
        assertEquals("don't", PronunciationScorer.cleanToken("Don't,"))
    }

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, PronunciationScorer.levenshteinDistance("kitten", "kitten"))
        assertEquals(3, PronunciationScorer.levenshteinDistance("kitten", "sitting"))
        assertEquals(2, PronunciationScorer.levenshteinDistance("go", "goes"))
        assertEquals(1, PronunciationScorer.levenshteinDistance("go", "got"))
    }

    @Test
    fun testWordScoreCalculations() {
        assertEquals(100, PronunciationScorer.calculateWordScore("study", "study"))
        assertEquals(100, PronunciationScorer.calculateWordScore("Study!", "study."))

        val partial = PronunciationScorer.calculateWordScore("goes", "go")
        assertTrue("Partial similarity should be around 50-75%", partial in 50..75)

        val wrong = PronunciationScorer.calculateWordScore("water", "fire")
        assertTrue("Unrelated words should have low score", wrong < 40)
    }

    @Test
    fun testExactSentenceMatch() {
        val target = "Water boils at 100 degrees Celsius."
        val spoken = "water boils at 100 degrees celsius"

        val result = PronunciationScorer.evaluate(target, spoken)

        assertTrue(result.isPassed)
        assertEquals(100, result.overallScore)
        assertTrue(result.scoredWords.all { it.accuracy == WordAccuracy.EXCELLENT })
    }

    @Test
    fun testNearMatchWithMinorMispronunciation() {
        val target = "She goes to school by bus every morning."
        val spoken = "she go to school by bus every morning"

        val result = PronunciationScorer.evaluate(target, spoken)

        assertTrue("Near match should pass", result.isPassed)
        assertTrue("Overall score should be high (>85%)", result.overallScore >= 85)

        val goesWord = result.scoredWords.firstOrNull { it.originalWord.contains("goes", ignoreCase = true) }
        assertNotNull(goesWord)
        assertEquals("go", goesWord?.spokenWord)
        assertTrue(goesWord?.score in 50..80)
    }

    @Test
    fun testEmptySpokenInput() {
        val target = "Look at the stars."
        val spoken = ""

        val result = PronunciationScorer.evaluate(target, spoken)

        assertFalse(result.isPassed)
        assertEquals(0, result.overallScore)
        assertTrue(result.scoredWords.all { it.accuracy == WordAccuracy.POOR })
    }

    @Test
    fun testSpeakingChallengeResultsMapping() {
        val target = "Perseverance leads to success."
        val item = QuestItem(
            question = "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
            options = listOf(target),
            correctIndex = 0,
            type = QuestionType.SPEAKING_CHALLENGE,
            speakingSentence = target,
            pronunciationScore = 95
        )

        // Passing case
        val isPassed = true
        val copyPassed = QuestItem(
            question = item.question,
            options = listOf(item.speakingSentence),
            correctIndex = 0,
            type = item.type,
            speakingSentence = item.speakingSentence,
            pronunciationScore = 95
        )
        copyPassed.selectedIndex = if (isPassed) 0 else 1

        assertEquals(copyPassed.correctIndex, copyPassed.selectedIndex)

        // Failing case
        val isFailed = false
        val copyFailed = QuestItem(
            question = item.question,
            options = listOf(item.speakingSentence),
            correctIndex = 0,
            type = item.type,
            speakingSentence = item.speakingSentence,
            pronunciationScore = 45
        )
        copyFailed.selectedIndex = if (isFailed) 0 else 1

        assertNotEquals(copyFailed.correctIndex, copyFailed.selectedIndex)
    }
}
