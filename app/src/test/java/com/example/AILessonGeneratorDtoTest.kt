package com.example

import com.example.model.ChapterGeneratedResponse
import com.example.model.GenerateChapterRequest
import com.example.model.QuestionItemDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class AILessonGeneratorDtoTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testGenerateChapterRequestSerialization() {
        val request = GenerateChapterRequest(
            userId = "user_123",
            level = "B1",
            topic = "Travel & Airport",
            weakWords = listOf("boarding pass", "departure"),
            questionCount = 5
        )
        val adapter = moshi.adapter(GenerateChapterRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"level\":\"B1\""))
        assertTrue(json.contains("\"topic\":\"Travel & Airport\""))
        assertTrue(json.contains("\"boarding pass\""))
    }

    @Test
    fun testChapterGeneratedResponseDeserialization() {
        val sampleJson = """
            {
              "title": "Chinh phục Sân bay Quốc tế",
              "description": "Học từ vựng và câu đàm thoại làm thủ tục check-in tại sân bay.",
              "level": "B1",
              "subject": "Travel",
              "orderNo": 2,
              "questions": [
                {
                  "questionText": "Excuse me, where is the check-in ______ for flight VN123?",
                  "questionType": "MULTIPLE_CHOICE",
                  "optionsJson": "[\"counter\", \"table\", \"chair\", \"door\"]",
                  "correctAnswer": "0",
                  "explanation": "'Check-in counter' nghĩa là quầy làm thủ tục tại sân bay.",
                  "points": 10,
                  "difficulty": "EASY"
                },
                {
                  "questionText": "Sắp xếp câu: passport / please / your / show / me",
                  "questionType": "SENTENCE_BUILDER",
                  "optionsJson": "[\"Please\", \"show\", \"me\", \"your\", \"passport\"]",
                  "correctAnswer": "Please show me your passport",
                  "explanation": "Câu yêu cầu lịch sự: Please show me your passport.",
                  "points": 15,
                  "difficulty": "MEDIUM"
                },
                {
                  "questionText": "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
                  "questionType": "SPEAKING_CHALLENGE",
                  "optionsJson": "[\"Where can I collect my luggage?\"]",
                  "correctAnswer": "Where can I collect my luggage?",
                  "explanation": "Câu hỏi vị trí lấy hành lý khi hạ cánh.",
                  "points": 20,
                  "difficulty": "HARD"
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(ChapterGeneratedResponse::class.java)
        val response = adapter.fromJson(sampleJson)

        assertNotNull(response)
        assertEquals("Chinh phục Sân bay Quốc tế", response!!.title)
        assertEquals("B1", response.level)
        assertEquals("Travel", response.subject)
        assertEquals(3, response.questions.size)

        val q1 = response.questions[0]
        assertEquals("MULTIPLE_CHOICE", q1.questionType)
        assertEquals("0", q1.correctAnswer)

        val q2 = response.questions[1]
        assertEquals("SENTENCE_BUILDER", q2.questionType)
        assertEquals("Please show me your passport", q2.correctAnswer)

        val q3 = response.questions[2]
        assertEquals("SPEAKING_CHALLENGE", q3.questionType)
        assertEquals(20, q3.points)
    }
}
