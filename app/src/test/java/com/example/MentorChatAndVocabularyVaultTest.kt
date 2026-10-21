package com.example

import com.example.model.MentorChatRequest
import com.example.model.MentorChatResponse
import com.example.model.QAItem
import com.example.model.VocabularyCardDto
import com.example.sqlite.room.VocabularyEntity
import org.junit.Assert.*
import org.junit.Test

class MentorChatAndVocabularyVaultTest {

    @Test
    fun testQAItemModelWithMentorInsight() {
        val vocab1 = VocabularyCardDto(
            word = "Persevere",
            phonetic = "/ˌpɜːsɪˈvɪər/",
            meaning = "Kiên trì vượt qua khó khăn",
            example = "She persevered despite early failures."
        )
        val vocab2 = VocabularyCardDto(
            word = "Resilient",
            phonetic = "/rɪˈzɪl.jənt/",
            meaning = "Kiên cường, mau phục hồi",
            example = "Children are remarkably resilient."
        )

        val item = QAItem(
            id = 1,
            question = "Làm sao để dùng từ persevere trong IELTS?",
            answer = "Từ persevere thường đi với giới từ in hoặc with...",
            grammarTip = "Chú ý: persevere in doing something (không dùng to do).",
            detectedVocabularies = listOf(vocab1, vocab2)
        )

        assertEquals("Làm sao để dùng từ persevere trong IELTS?", item.question)
        assertEquals("Chú ý: persevere in doing something (không dùng to do).", item.grammarTip)
        assertEquals(2, item.detectedVocabularies.size)
        assertEquals("Persevere", item.detectedVocabularies[0].word)
        assertEquals("/ˌpɜːsɪˈvɪər/", item.detectedVocabularies[0].phonetic)
        assertEquals("Resilient", item.detectedVocabularies[1].word)
    }

    @Test
    fun testMentorChatRequestAndResponse() {
        val request = MentorChatRequest(
            message = "Phân biệt giữa affect và effect",
            userLevel = "Intermediate",
            currentChapter = "Syntax & Sentence Mastery",
            recentWeakPoints = listOf("verbs", "grammar")
        )

        assertEquals("Phân biệt giữa affect và effect", request.message)
        assertEquals("Intermediate", request.userLevel)
        assertEquals("Syntax & Sentence Mastery", request.currentChapter)
        assertEquals(2, request.recentWeakPoints.size)

        val response = MentorChatResponse(
            reply = "Affect thường là động từ, Effect thường là danh từ.",
            grammarTip = "Mẹo nhớ: A for Action (Affect = Verb), E for End result (Effect = Noun).",
            detectedVocabularies = listOf(
                VocabularyCardDto("Affect", "/əˈfekt/", "Làm ảnh hưởng đến (động từ)", "Smoking affects your health."),
                VocabularyCardDto("Effect", "/ɪˈfekt/", "Kết quả, hiệu ứng (danh từ)", "The effect of music is profound.")
            )
        )

        assertEquals("Affect thường là động từ, Effect thường là danh từ.", response.reply)
        assertNotNull(response.grammarTip)
        assertEquals(2, response.detectedVocabularies.size)
    }

    @Test
    fun testVocabularyEntityInitializationForSm2() {
        val word = "Resilient"
        val phonetic = "/rɪˈzɪl.jənt/"
        val meaning = "Kiên cường"
        val example = "Stay resilient during tough times."

        val vocab = VocabularyEntity(
            userId = 42L,
            word = word.trim(),
            phonetic = phonetic.trim(),
            meaning = meaning.trim(),
            example = example.trim(),
            isMastered = false,
            repetitionLevel = 0,
            intervalDays = 1,
            easinessFactor = 2.5f
        )

        assertEquals(42L, vocab.userId)
        assertEquals("Resilient", vocab.word)
        assertEquals(1, vocab.intervalDays)
        assertEquals(2.5f, vocab.easinessFactor, 0.001f)
        assertEquals(0, vocab.repetitionLevel)
        assertFalse(vocab.isMastered)
    }
}
