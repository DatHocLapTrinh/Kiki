package com.example

import com.example.model.DiagnosticSkillType
import com.example.util.AdaptiveDiagnosticEngine
import com.example.util.DiagnosticAnswerRecord
import org.junit.Assert.*
import org.junit.Test

class AdaptiveDiagnosticEngineTest {

    @Test
    fun testItemBankStructureAndCalibration() {
        val bank = AdaptiveDiagnosticEngine.itemBank
        assertEquals("Item bank must contain exactly 12 calibrated items across 4 stages", 12, bank.size)

        val grammarItems = bank.filter { it.skillType == DiagnosticSkillType.GRAMMAR }
        val vocabItems = bank.filter { it.skillType == DiagnosticSkillType.VOCABULARY }
        val listeningItems = bank.filter { it.skillType == DiagnosticSkillType.LISTENING }
        val speakingItems = bank.filter { it.skillType == DiagnosticSkillType.SPEAKING }

        assertEquals(3, grammarItems.size)
        assertEquals(3, vocabItems.size)
        assertEquals(3, listeningItems.size)
        assertEquals(3, speakingItems.size)

        // Check difficulty b is within calibrated Rasch range [-1.5, +1.5]
        bank.forEach { item ->
            assertTrue("Difficulty b should be within [-1.5, +1.5]", item.difficultyB in -1.5f..1.5f)
            assertTrue("Prompt must not be empty", item.questionPrompt.isNotBlank())
            assertTrue("Skill tag must not be empty", item.skillTag.isNotBlank())
        }
    }

    @Test
    fun testSelectNextItemAdaptsToSkillAndTheta() {
        // Stage 1 (Grammar) with low theta (-0.9) should pick easiest item (Present Simple, b = -0.8)
        val easyItem = AdaptiveDiagnosticEngine.selectNextItem(stage = 1, currentTheta = -0.9f, answeredIds = emptySet())
        assertEquals(101, easyItem.id)
        assertEquals(DiagnosticSkillType.GRAMMAR, easyItem.skillType)

        // Stage 1 with high theta (+1.0) should pick hardest item (Scarcely inversion, b = 1.1)
        val hardItem = AdaptiveDiagnosticEngine.selectNextItem(stage = 1, currentTheta = 1.0f, answeredIds = emptySet())
        assertEquals(103, hardItem.id)

        // Stage 3 (Listening) should only select Listening items
        val listeningItem = AdaptiveDiagnosticEngine.selectNextItem(stage = 3, currentTheta = 0.0f, answeredIds = emptySet())
        assertEquals(DiagnosticSkillType.LISTENING, listeningItem.skillType)

        // Stage 4 (Speaking) should only select Speaking items
        val speakingItem = AdaptiveDiagnosticEngine.selectNextItem(stage = 4, currentTheta = 0.0f, answeredIds = emptySet())
        assertEquals(DiagnosticSkillType.SPEAKING, speakingItem.skillType)
    }

    @Test
    fun testUpdateThetaRaschModel() {
        val initialTheta = 0.0f
        val itemDifficulty = 0.0f

        // Correct answer increases theta
        val thetaAfterCorrect = AdaptiveDiagnosticEngine.updateTheta(
            currentTheta = initialTheta,
            itemDifficulty = itemDifficulty,
            isCorrect = true,
            latencyMs = 3000L
        )
        assertTrue("Theta must increase after correct answer", thetaAfterCorrect > initialTheta)

        // Incorrect answer decreases theta
        val thetaAfterIncorrect = AdaptiveDiagnosticEngine.updateTheta(
            currentTheta = initialTheta,
            itemDifficulty = itemDifficulty,
            isCorrect = false,
            latencyMs = 3000L
        )
        assertTrue("Theta must decrease after incorrect answer", thetaAfterIncorrect < initialTheta)

        // Fast correct response (<2500ms) gets reflex bonus
        val thetaFast = AdaptiveDiagnosticEngine.updateTheta(
            currentTheta = initialTheta,
            itemDifficulty = itemDifficulty,
            isCorrect = true,
            latencyMs = 1200L
        )
        assertTrue("Fast correct response should yield higher theta than slow correct response", thetaFast > thetaAfterCorrect)

        // Theta must remain bounded in [-2.0, 2.0]
        val extremeHigh = AdaptiveDiagnosticEngine.updateTheta(2.0f, -1.0f, true, 1000L)
        assertEquals(2.0f, extremeHigh, 0.001f)

        val extremeLow = AdaptiveDiagnosticEngine.updateTheta(-2.0f, 1.0f, false, 5000L)
        assertEquals(-2.0f, extremeLow, 0.001f)
    }

    @Test
    fun testEvaluateFinalProfile() {
        val itemGrammar = AdaptiveDiagnosticEngine.itemBank[0]
        val itemVocab = AdaptiveDiagnosticEngine.itemBank[3]
        val itemListening = AdaptiveDiagnosticEngine.itemBank[6]
        val itemSpeaking = AdaptiveDiagnosticEngine.itemBank[9]

        val answers = listOf(
            DiagnosticAnswerRecord(itemGrammar, selectedIndex = 0, isCorrect = true, latencyMs = 2000L),
            DiagnosticAnswerRecord(itemVocab, selectedIndex = 0, isCorrect = true, latencyMs = 2100L),
            DiagnosticAnswerRecord(itemListening, selectedIndex = 1, isCorrect = false, latencyMs = 4500L), // Incorrect
            DiagnosticAnswerRecord(itemSpeaking, selectedIndex = 0, isCorrect = true, latencyMs = 2500L)
        )

        val profile = AdaptiveDiagnosticEngine.evaluateFinalProfile(
            userId = 12345L,
            goal = "Everyday Fluency",
            dailyTargetMinutes = 5,
            selfEfficacy = "ELEMENTARY",
            priorityModality = "AUDIO_DIALOG",
            finalTheta = 0.2f,
            answers = answers
        )

        assertEquals(12345L, profile.userId)
        assertEquals("Everyday Fluency", profile.goal)
        assertEquals(5, profile.dailyTargetMinutes)
        assertEquals("Intermediate", profile.evaluatedCefr)
        assertEquals(100, profile.grammarAccuracy)
        assertEquals(100, profile.vocabularyAccuracy)
        assertEquals(0, profile.acousticAccuracy)
        assertEquals(100, profile.speakingAccuracy)

        // Weak points must include the failed listening item's skill tag
        assertTrue("Weak points must include Acoustic_Everyday_Greeting", profile.baselineWeakPoints.contains("Acoustic_Everyday_Greeting"))

        // Initial EF must be calibrated around 2.5 + theta * 0.1
        val expectedEf = (2.5f + 0.2f * 0.1f).coerceIn(2.3f, 2.7f)
        assertEquals(expectedEf, profile.initialEasinessFactor, 0.01f)
    }

    @Test
    fun testCefrCutoffs() {
        val emptyAnswers = emptyList<DiagnosticAnswerRecord>()

        val beginner = AdaptiveDiagnosticEngine.evaluateFinalProfile(
            1L, "Goal", 5, "NEWCOMER", "AUDIO_DIALOG", -0.5f, emptyAnswers
        )
        assertEquals("Beginner", beginner.evaluatedCefr)

        val intermediate = AdaptiveDiagnosticEngine.evaluateFinalProfile(
            1L, "Goal", 5, "INTERMEDIATE", "AUDIO_DIALOG", 0.3f, emptyAnswers
        )
        assertEquals("Intermediate", intermediate.evaluatedCefr)

        val advanced = AdaptiveDiagnosticEngine.evaluateFinalProfile(
            1L, "Goal", 5, "CONFIDENT", "AUDIO_DIALOG", 0.9f, emptyAnswers
        )
        assertEquals("Advanced", advanced.evaluatedCefr)
    }
}
