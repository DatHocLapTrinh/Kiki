package com.example

import com.example.audio.TextToSpeechManager
import com.example.model.CosmicCallRequest
import com.example.model.CosmicCallResponse
import com.example.model.CosmicCallTurnDto
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CosmicCallDynamicConversationTest {

    @Test
    fun testVietnameseTextDetection() {
        val vietnamesePattern = Regex("[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]")
        
        // Tiếng Việt có dấu
        assertTrue(vietnamesePattern.containsMatchIn("Phản xạ rất tốt, phát âm chuẩn!"))
        assertTrue(vietnamesePattern.containsMatchIn("Chào Trường, rất vui được gặp bạn"))
        assertTrue(vietnamesePattern.containsMatchIn("Luyện tập thêm âm đuôi nhé"))

        // Tiếng Anh thuần túy
        assertFalse(vietnamesePattern.containsMatchIn("Nice to meet you, Truong! How are you today?"))
        assertFalse(vietnamesePattern.containsMatchIn("I live in Hanoi and I like coffee."))
        assertFalse(vietnamesePattern.containsMatchIn("Great job! Your answer was fluent and clear."))
    }

    @Test
    fun testCosmicCallResponseModel() {
        val response = CosmicCallResponse(
            score = 92,
            spokenFeedbackEn = "Awesome job Truong! Your introduction was clear and fluent.",
            feedbackVi = "Phản xạ xuất sắc! Câu trả lời mạch lạc và phát âm chuẩn.",
            grammarCorrection = "My name is Truong and I live in Hanoi.",
            nextQuestionEn = "What is your favorite coffee shop in Hanoi?",
            nextHintVi = "Nói về quán cà phê bạn hay ghé.",
            suggestedStarterEn = "My favorite coffee shop is...",
            sanitizedUserAnswer = "My name is Truong"
        )

        assertEquals(92, response.score)
        assertTrue(response.spokenFeedbackEn.contains("Truong"))
        assertTrue(response.feedbackVi.contains("Phản xạ xuất sắc"))
        assertEquals("My name is Truong and I live in Hanoi.", response.grammarCorrection)
        assertNotNull(response.nextQuestionEn)
        assertNotNull(response.nextHintVi)
        assertEquals("My favorite coffee shop is...", response.suggestedStarterEn)
        assertEquals("My name is Truong", response.sanitizedUserAnswer)
    }

    @Test
    fun testCosmicCallRequestWithHistory() {
        val history = listOf(
            CosmicCallTurnDto(role = "kiki", text = "Can you introduce yourself?"),
            CosmicCallTurnDto(role = "user", text = "My name is Truong and I live in Hanoi.")
        )

        val request = CosmicCallRequest(
            userAnswer = "I usually drink egg coffee in the morning",
            currentQuestion = "What kind of coffee do you usually drink?",
            planetOrTopic = "Earth: Food & Drinks",
            userLevel = "A2 Elementary",
            conversationHistory = history,
            userName = "Nguyễn Văn Trường",
            candidateTranscripts = listOf("I usually drink egg coffee in the morning")
        )

        assertEquals("I usually drink egg coffee in the morning", request.userAnswer)
        assertEquals(2, request.conversationHistory.size)
        assertEquals("kiki", request.conversationHistory[0].role)
        assertEquals("user", request.conversationHistory[1].role)
        assertEquals("Nguyễn Văn Trường", request.userName)
        assertNotNull(request.candidateTranscripts)
    }

    @Test
    fun testZeroSpeechAndInaccurateAnswerGrading() {
        // Response with 0 points for silence
        val zeroResponse = CosmicCallResponse(
            score = 0,
            spokenFeedbackEn = "I didn't hear your voice. Please tap the mic and speak aloud!",
            feedbackVi = "Kiki chưa ghi nhận được câu trả lời. Hãy bấm nút Micro và nói tiếng Anh nhé! 🎙️",
            grammarCorrection = "",
            nextQuestionEn = "What is your name?",
            nextHintVi = "Bấm giữ nút Micro và nói tiếng Anh.",
            suggestedStarterEn = "Hello Kiki,..."
        )

        assertEquals(0, zeroResponse.score)
        assertTrue(zeroResponse.feedbackVi.contains("chưa ghi nhận được"));

        // Response with low score for inaccurate/off-topic answer
        val inaccurateResponse = CosmicCallResponse(
            score = 25,
            spokenFeedbackEn = "That didn't quite make sense for this question.",
            feedbackVi = "Câu trả lời chưa đúng trọng tâm câu hỏi hoặc từ ngữ chưa chính xác.",
            grammarCorrection = "My name is Truong.",
            nextQuestionEn = "What is your name?",
            nextHintVi = "Nói tên của bạn."
        )

        assertTrue(inaccurateResponse.score <= 35)
        assertTrue(inaccurateResponse.feedbackVi.contains("chưa đúng trọng tâm"))
    }
}
