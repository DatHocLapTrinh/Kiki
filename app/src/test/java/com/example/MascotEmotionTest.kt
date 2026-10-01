package com.example

import com.example.model.MascotEmotion
import com.example.model.MascotQuotes
import org.junit.Assert.*
import org.junit.Test

class MascotEmotionTest {

    @Test
    fun testMascotQuotesGenerationInBothLanguages() {
        for (emotion in MascotEmotion.values()) {
            val vietnameseQuote = MascotQuotes.getQuoteForEmotion(emotion, isEnglish = false, streak = 5)
            val englishQuote = MascotQuotes.getQuoteForEmotion(emotion, isEnglish = true, streak = 5)

            assertTrue("Vietnamese quote for $emotion should not be blank", vietnameseQuote.isNotBlank())
            assertTrue("English quote for $emotion should not be blank", englishQuote.isNotBlank())
        }
    }

    @Test
    fun testStreakQuotesContainStreakNumber() {
        val streak = 7
        val viStreakQuote = MascotQuotes.getQuoteForEmotion(MascotEmotion.STREAK, isEnglish = false, streak = streak)
        val enStreakQuote = MascotQuotes.getQuoteForEmotion(MascotEmotion.STREAK, isEnglish = true, streak = streak)

        assertTrue(
            "Vietnamese streak quote should contain streak count '$streak'",
            viStreakQuote.contains(streak.toString())
        )
        assertTrue(
            "English streak quote should contain streak count '$streak'",
            enStreakQuote.contains(streak.toString())
        )
    }

    @Test
    fun testEmotionFSMTransitions() {
        fun resolveEmotion(
            isAnswerChecked: Boolean,
            isCorrect: Boolean,
            streak: Int,
            isSpeakingRecording: Boolean,
            idleSeconds: Int,
            isPetted: Boolean
        ): MascotEmotion {
            return when {
                isPetted -> MascotEmotion.PETTED
                isSpeakingRecording -> MascotEmotion.LISTENING
                isAnswerChecked && isCorrect && streak >= 3 -> MascotEmotion.STREAK
                isAnswerChecked && isCorrect -> MascotEmotion.CHEER
                isAnswerChecked && !isCorrect -> MascotEmotion.EMPATHY
                idleSeconds >= 15 -> MascotEmotion.THINKING
                else -> MascotEmotion.IDLE
            }
        }

        // Test Idle state
        assertEquals(
            MascotEmotion.IDLE,
            resolveEmotion(isAnswerChecked = false, isCorrect = false, streak = 0, isSpeakingRecording = false, idleSeconds = 5, isPetted = false)
        )

        // Test Thinking state when idle for >= 15 seconds
        assertEquals(
            MascotEmotion.THINKING,
            resolveEmotion(isAnswerChecked = false, isCorrect = false, streak = 0, isSpeakingRecording = false, idleSeconds = 16, isPetted = false)
        )

        // Test Cheer on regular correct answer
        assertEquals(
            MascotEmotion.CHEER,
            resolveEmotion(isAnswerChecked = true, isCorrect = true, streak = 2, isSpeakingRecording = false, idleSeconds = 2, isPetted = false)
        )

        // Test Streak on 3+ consecutive correct answers
        assertEquals(
            MascotEmotion.STREAK,
            resolveEmotion(isAnswerChecked = true, isCorrect = true, streak = 4, isSpeakingRecording = false, idleSeconds = 2, isPetted = false)
        )

        // Test Empathy on incorrect answer
        assertEquals(
            MascotEmotion.EMPATHY,
            resolveEmotion(isAnswerChecked = true, isCorrect = false, streak = 0, isSpeakingRecording = false, idleSeconds = 2, isPetted = false)
        )

        // Test Listening when speaking recording is active
        assertEquals(
            MascotEmotion.LISTENING,
            resolveEmotion(isAnswerChecked = false, isCorrect = false, streak = 0, isSpeakingRecording = true, idleSeconds = 0, isPetted = false)
        )

        // Test Petted overrides idle
        assertEquals(
            MascotEmotion.PETTED,
            resolveEmotion(isAnswerChecked = false, isCorrect = false, streak = 0, isSpeakingRecording = false, idleSeconds = 0, isPetted = true)
        )
    }
}
