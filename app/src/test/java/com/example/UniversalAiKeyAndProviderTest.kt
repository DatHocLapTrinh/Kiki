package com.example

import com.example.viewmodel.StudyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalAiKeyAndProviderTest {

    @Test
    fun testGroqKeyAutoDetection() {
        val key = "gsk_SampleGroqKey123456789"
        val detected = StudyViewModel.detectAiProvider(key)
        assertEquals(StudyViewModel.PROVIDER_GROQ, detected)
    }

    @Test
    fun testGeminiKeyAutoDetection() {
        val keyAIza = "AIzaSyDummyGeminiKeySample98765"
        val keyAQ = "AQ.DummyGeminiOAuthFormatKeySample12345"
        assertEquals(StudyViewModel.PROVIDER_GEMINI, StudyViewModel.detectAiProvider(keyAIza))
        assertEquals(StudyViewModel.PROVIDER_GEMINI, StudyViewModel.detectAiProvider(keyAQ))
    }

    @Test
    fun testDefaultProviderIsGemini() {
        assertEquals(StudyViewModel.PROVIDER_GEMINI, StudyViewModel.detectAiProvider(""))
        assertEquals(StudyViewModel.PROVIDER_GEMINI, StudyViewModel.detectAiProvider("custom_random_token"))
    }

    @Test
    fun testClaudeKeyAutoDetection() {
        val key = "sk-ant-api03-sampleClaudeKey"
        val detected = StudyViewModel.detectAiProvider(key)
        assertEquals(StudyViewModel.PROVIDER_CLAUDE, detected)
    }

    @Test
    fun testOpenAiKeyAutoDetection() {
        val keyProj = "sk-proj-sampleOpenAiProjectKey"
        val keyStandard = "sk-standardOpenAiKey"
        assertEquals(StudyViewModel.PROVIDER_OPENAI, StudyViewModel.detectAiProvider(keyProj))
        assertEquals(StudyViewModel.PROVIDER_OPENAI, StudyViewModel.detectAiProvider(keyStandard))
    }

    @Test
    fun testKeyWithWhitespaceTrimming() {
        val keyWithSpaces = "   gsk_GroqWithSpaces   "
        val detected = StudyViewModel.detectAiProvider(keyWithSpaces)
        assertEquals(StudyViewModel.PROVIDER_GROQ, detected)
    }

    @Test
    fun testFormatForAudioSpeech() {
        val input = "Resilient /rɪˈzɪliənt/ (Tính từ)\n• Nghĩa: Kiên cường\n• Ví dụ: **She is resilient**."
        val output = com.example.model.GeminiModels.formatForAudioSpeech(input)
        org.junit.Assert.assertFalse(output.contains("/"))
        org.junit.Assert.assertFalse(output.contains("•"))
        org.junit.Assert.assertFalse(output.contains("**"))
        assertTrue(output.contains("Kiên cường"))
        assertTrue(output.contains("She is resilient"))
    }

    @Test
    fun testProviderDisplayNames() {
        assertTrue(StudyViewModel.getProviderDisplayName(StudyViewModel.PROVIDER_GROQ).contains("Groq"))
        assertTrue(StudyViewModel.getProviderDisplayName(StudyViewModel.PROVIDER_GEMINI).contains("Gemini"))
        assertTrue(StudyViewModel.getProviderDisplayName(StudyViewModel.PROVIDER_CLAUDE).contains("Claude"))
        assertTrue(StudyViewModel.getProviderDisplayName(StudyViewModel.PROVIDER_OPENAI).contains("OpenAI"))
        assertTrue(StudyViewModel.getProviderDisplayName(StudyViewModel.PROVIDER_AUTO).contains("Gemini"))
    }
}
