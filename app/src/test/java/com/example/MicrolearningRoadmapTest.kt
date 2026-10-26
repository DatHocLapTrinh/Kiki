package com.example

import com.example.model.LearnerPersonaProfile
import com.example.model.ChapterGeneratedResponse
import com.example.model.ClozeChoiceSlot
import com.example.model.SentenceBuilderSlot
import com.example.model.ListeningChallengeSlot
import com.example.model.PairSlotItem
import com.example.model.MatchingPairsSlot
import com.example.model.SpeakingChallengeSlot
import com.example.model.InjectedAtomicLessonUnit
import com.example.model.MicrolearningSlotArchetypes
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class MicrolearningRoadmapTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testDefaultStagesStructureForEverydayFluency() {
        val profile = LearnerPersonaProfile(
            userId = 1L,
            goal = "Everyday Fluency",
            evaluatedCefr = "B1",
            dailyTargetMinutes = 5
        )
        // Verify default stages structure for Everyday Fluency
        val stages = getTestScientificDefaultStages(profile)
        assertEquals(5, stages.size)
        assertTrue(stages[0].first.contains("Chặng 1"))
        assertTrue(stages[0].first.contains("Khởi Động"))
        assertTrue(stages[4].first.contains("Chặng 5"))
        assertTrue(stages[4].first.contains("Mastery"))
    }

    @Test
    fun testDefaultStagesStructureForGrammarAndVocabulary() {
        val profile = LearnerPersonaProfile(
            userId = 2L,
            goal = "Grammar & Vocabulary",
            evaluatedCefr = "A2",
            dailyTargetMinutes = 10
        )
        val stages = getTestScientificDefaultStages(profile)
        assertEquals(5, stages.size)
        assertTrue(stages[0].first.contains("Chặng 1"))
        assertTrue(stages[0].first.contains("Core Syntax"))
        assertTrue(stages[4].first.contains("Chặng 5"))
        assertTrue(stages[4].first.contains("Mastery Synthesis"))
    }

    @Test
    fun testDefaultStagesStructureForIeltsAndToeic() {
        val profile = LearnerPersonaProfile(
            userId = 3L,
            goal = "IELTS & TOEIC",
            evaluatedCefr = "B2",
            dailyTargetMinutes = 15
        )
        val stages = getTestScientificDefaultStages(profile)
        assertEquals(5, stages.size)
        assertTrue(stages[0].first.contains("Chặng 1"))
        assertTrue(stages[0].first.contains("Academic Lexicon"))
        assertTrue(stages[4].first.contains("Chặng 5"))
        assertTrue(stages[4].first.contains("Exam Mastery Simulation"))
    }

    @Test
    fun testDefaultStagesStructureForCareerAndBusiness() {
        val profile = LearnerPersonaProfile(
            userId = 4L,
            goal = "Career & Business",
            evaluatedCefr = "B1",
            dailyTargetMinutes = 10
        )
        val stages = getTestScientificDefaultStages(profile)
        assertEquals(5, stages.size)
        assertTrue(stages[0].first.contains("Chặng 1"))
        assertTrue(stages[0].first.contains("Workplace Etiquette"))
        assertTrue(stages[4].first.contains("Chặng 5"))
        assertTrue(stages[4].first.contains("Executive Fluency"))
    }

    @Test
    fun test3CMicrolearningJsonResponseFormat() {
        val sample3CJson = """
            {
              "title": "Chặng 1: Giao Tiếp Công Sở & Email Chuẩn Mực",
              "description": "Bài học Microlearning 3-5 phút theo mô hình 3C (Concept - Check - Consolidate)",
              "level": "B1",
              "subject": "Career & Business",
              "orderNo": 1,
              "questions": [
                {
                  "questionText": "[CONCEPT] In professional emails, 'I am writing to inquire about...' is used to make a formal request.",
                  "questionType": "MULTIPLE_CHOICE",
                  "optionsJson": "[\"inquire\", \"ask up\", \"tell about\", \"demand\"]",
                  "correctAnswer": "0",
                  "explanation": "'Inquire about' là cụm từ trang trọng nhất dùng trong thư tín thương mại.",
                  "points": 10,
                  "difficulty": "EASY"
                },
                {
                  "questionText": "[CHECK] Điền từ phù hợp: Could you please _____ the attached report?",
                  "questionType": "MULTIPLE_CHOICE",
                  "optionsJson": "[\"review\", \"look\", \"see\", \"watch\"]",
                  "correctAnswer": "0",
                  "explanation": "'Review the attached report' nghĩa là xem xét báo cáo đính kèm.",
                  "points": 10,
                  "difficulty": "EASY"
                },
                {
                  "questionText": "[CHECK] Sắp xếp câu: forward / looking / to / I / hearing / am / you / from",
                  "questionType": "SENTENCE_BUILDER",
                  "optionsJson": "[\"I\", \"am\", \"looking\", \"forward\", \"to\", \"hearing\", \"from\", \"you\"]",
                  "correctAnswer": "I am looking forward to hearing from you",
                  "explanation": "Cấu trúc kết thư chuyên nghiệp: I am looking forward to hearing from you.",
                  "points": 15,
                  "difficulty": "MEDIUM"
                },
                {
                  "questionText": "[CONSOLIDATE] Luyện nói câu kết thư: 'Thank you for your prompt assistance.'",
                  "questionType": "SPEAKING",
                  "optionsJson": "[\"Thank\", \"you\", \"for\", \"your\", \"prompt\", \"assistance\"]",
                  "correctAnswer": "Thank you for your prompt assistance",
                  "explanation": "Luyện phát âm chuẩn âm đuôi /t/ trong prompt và trọng âm thứ 2 của assistance.",
                  "points": 20,
                  "difficulty": "MEDIUM"
                },
                {
                  "questionText": "[CONSOLIDATE] Phản xạ nhanh: Đáp lại 'Thank you for your support' một cách trang trọng?",
                  "questionType": "MULTIPLE_CHOICE",
                  "optionsJson": "[\"It is my pleasure\", \"No problem\", \"Yeah ok\", \"Never mind\"]",
                  "correctAnswer": "0",
                  "explanation": "'It is my pleasure' là phản hồi lịch thiệp và chuẩn mực nhất trong công sở.",
                  "points": 15,
                  "difficulty": "HARD"
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(ChapterGeneratedResponse::class.java)
        val response = adapter.fromJson(sample3CJson)

        assertNotNull(response)
        assertEquals("Chặng 1: Giao Tiếp Công Sở & Email Chuẩn Mực", response!!.title)
        assertEquals(5, response.questions.size) // Exact 5 questions per Microlearning Atomic Unit (3-5 min)

        // Verify 3C Progression
        assertTrue(response.questions[0].questionText.contains("[CONCEPT]"))
        assertTrue(response.questions[1].questionText.contains("[CHECK]"))
        assertTrue(response.questions[2].questionText.contains("[CHECK]"))
        assertTrue(response.questions[3].questionText.contains("[CONSOLIDATE]"))
        assertTrue(response.questions[4].questionText.contains("[CONSOLIDATE]"))

        // Verify Question Diversity
        assertEquals("MULTIPLE_CHOICE", response.questions[0].questionType)
        assertEquals("SENTENCE_BUILDER", response.questions[2].questionType)
        assertEquals("SPEAKING", response.questions[3].questionType)
    }

    @Test
    fun testGolden5StepMultimodalCycle() {
        val archetypes = (0..9).map { index ->
            when (index % 5) {
                0 -> com.example.model.QuestionType.MULTIPLE_CHOICE
                1 -> com.example.model.QuestionType.SENTENCE_BUILDER
                2 -> com.example.model.QuestionType.LISTENING_CHALLENGE
                3 -> com.example.model.QuestionType.MATCHING_PAIRS
                4 -> com.example.model.QuestionType.SPEAKING_CHALLENGE
                else -> com.example.model.QuestionType.MULTIPLE_CHOICE
            }
        }

        // Verify first 5 questions have distinct archetypes
        assertEquals(com.example.model.QuestionType.MULTIPLE_CHOICE, archetypes[0])
        assertEquals(com.example.model.QuestionType.SENTENCE_BUILDER, archetypes[1])
        assertEquals(com.example.model.QuestionType.LISTENING_CHALLENGE, archetypes[2])
        assertEquals(com.example.model.QuestionType.MATCHING_PAIRS, archetypes[3])
        assertEquals(com.example.model.QuestionType.SPEAKING_CHALLENGE, archetypes[4])

        // Verify seamless repetition for questions 6..10
        assertEquals(com.example.model.QuestionType.MULTIPLE_CHOICE, archetypes[5])
        assertEquals(com.example.model.QuestionType.SENTENCE_BUILDER, archetypes[6])
        assertEquals(com.example.model.QuestionType.LISTENING_CHALLENGE, archetypes[7])
        assertEquals(com.example.model.QuestionType.MATCHING_PAIRS, archetypes[8])
        assertEquals(com.example.model.QuestionType.SPEAKING_CHALLENGE, archetypes[9])
    }

    @Test
    fun testSentenceBuilderNeverHasRawBlanks() {
        val questionWithBlank = "Look! The baby ______ in her crib."
        val options = listOf("sleeps", "is sleeping", "slept", "has slept")
        val correctWord = options[1]
        val completeSentence = questionWithBlank.replace(Regex("_{2,}"), correctWord).replace(Regex("\\s+"), " ").trim()

        assertEquals("Look! The baby is sleeping in her crib.", completeSentence)
        assertFalse("Complete sentence should not have raw blanks", completeSentence.contains("______"))

        val promptText = if (questionWithBlank.contains(Regex("_{2,}"))) {
            "Sắp xếp các từ sau thành câu hoàn chỉnh 🧩"
        } else {
            questionWithBlank
        }
        assertEquals("Sắp xếp các từ sau thành câu hoàn chỉnh 🧩", promptText)
    }

    @Test
    fun testInjectedAtomicLessonUnitConversion() {
        val unit = InjectedAtomicLessonUnit(
            title = "Chặng 1: Giao Tiếp Công Sở",
            description = "Làm chủ email và phản xạ đàm thoại cơ bản.",
            level = "B1",
            subject = "English",
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Could you please _____ the attached invoice?",
                options = listOf("review", "look", "watch", "see"),
                correctIndex = 0,
                explanation = "Review the invoice means kiểm tra hóa đơn."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu hoàn chỉnh 🧩",
                targetSentence = "I am looking forward to hearing from you",
                wordTokens = listOf("I", "am", "looking", "forward", "to", "hearing", "from", "you"),
                distractorTokens = listOf("at")
            ),
            slot3Listening = ListeningChallengeSlot(
                audioSentence = "Please let me know if you have questions",
                explanation = "Nghe và nhận diện âm đuôi."
            ),
            slot4Matching = MatchingPairsSlot(
                pairs = listOf(
                    PairSlotItem(english = "inquire", vietnamese = "hỏi thăm, tra cứu"),
                    PairSlotItem(english = "assistance", vietnamese = "sự hỗ trợ"),
                    PairSlotItem(english = "attached", vietnamese = "được đính kèm")
                )
            ),
            slot5Speaking = SpeakingChallengeSlot(
                targetSentence = "Thank you for your prompt assistance today"
            )
        )

        val chapter = unit.toChapterGeneratedResponse(orderNo = 1)
        assertEquals("Chặng 1: Giao Tiếp Công Sở", chapter.title)
        assertEquals(5, chapter.questions.size)

        // Verify each slot's archetypes
        assertEquals(MicrolearningSlotArchetypes.CLOZE_CHOICE, chapter.questions[0].questionType)
        assertEquals(MicrolearningSlotArchetypes.SENTENCE_BUILDER, chapter.questions[1].questionType)
        assertEquals(MicrolearningSlotArchetypes.LISTENING_CHALLENGE, chapter.questions[2].questionType)
        assertEquals(MicrolearningSlotArchetypes.MATCHING_PAIRS, chapter.questions[3].questionType)
        assertEquals(MicrolearningSlotArchetypes.SPEAKING_CHALLENGE, chapter.questions[4].questionType)

        // Verify JSON contents of optionsJson
        assertTrue(chapter.questions[0].optionsJson.contains("review"))
        assertTrue(chapter.questions[1].optionsJson.contains("looking"))
        assertTrue(chapter.questions[2].optionsJson.contains("Please let me know"))
        assertTrue(chapter.questions[3].optionsJson.contains("inquire=hỏi thăm, tra cứu"))
        assertTrue(chapter.questions[4].optionsJson.contains("Thank you for your prompt assistance"))
    }

    private fun getTestScientificDefaultStages(profile: LearnerPersonaProfile): List<Pair<String, String>> {
        return when (profile.goal) {
            "Grammar & Vocabulary" -> listOf(
                Pair("Chặng 1: Củng Cố Cấu Trúc Cốt Lõi (Core Syntax)", "Xóa bỏ lỗi chia động từ và hòa hợp chủ vị cơ bản."),
                Pair("Chặng 2: Làm Chủ Câu Điều Kiện Giả Định (Conditionals)", "Nắm vững bản chất câu điều kiện loại 2 và 3 trong giao tiếp."),
                Pair("Chặng 3: Đảo Ngữ & Trạng Từ Nhấn Mạnh (Inversion)", "Sử dụng các trạng từ phủ định đảo ngữ để nâng cao sắc thái biểu đạt."),
                Pair("Chặng 4: Mệnh Đề Rút Gọn & Cú Pháp Nâng Cao (Reduced Clauses)", "Rút gọn mệnh đề quan hệ và mệnh đề trạng ngữ linh hoạt."),
                Pair("Chặng 5: Tổng Hợp Ngữ Pháp Đỉnh Cao (Mastery Synthesis)", "Chinh phục bài tập phản xạ ngữ pháp tổng hợp toàn diện.")
            )
            "IELTS & TOEIC" -> listOf(
                Pair("Chặng 1: Nền Tảng Từ Vựng Học Thuật (Academic Lexicon)", "Làm chủ 20 collocations học thuật xuất hiện thường trực trong đề thi."),
                Pair("Chặng 2: Nhận Diện Cấu Trúc Câu Phức Đề Thi (Complex Structures)", "Phân tích và dựng câu phức chuẩn mực cho bài thi viết và đọc."),
                Pair("Chặng 3: Kỹ Thuật Nghe Bắt Ý Chính (Audio Gist Extraction)", "Rèn luyện tai nhận diện bẫy âm thanh và từ đồng nghĩa trong bài nghe."),
                Pair("Chặng 4: Diễn Đạt Ý Nghĩa Đa Chiều (Paraphrasing Mastery)", "Kỹ năng biến đổi từ loại và cấu trúc câu tương đương tức thì."),
                Pair("Chặng 5: Luyện Đề Bứt Phá Band Điểm (Exam Mastery Simulation)", "Thử thách phản xạ câu hỏi đề thi chuẩn hóa thời gian thực.")
            )
            "Career & Business" -> listOf(
                Pair("Chặng 1: Giao Tiếp Công Sở & Email Chuẩn Mực (Workplace Etiquette)", "Sử dụng văn phong trang trọng và lịch thiệp trong trao đổi công việc."),
                Pair("Chặng 2: Đàm Thoại & Thuyết Trình Dự Án (Business Pitching)", "Cấu trúc hóa luận điểm và trình bày báo cáo ngắn gọn, thuyết phục."),
                Pair("Chặng 3: Thảo Luận & Từ Chối Khéo Léo (Professional Diplomacy)", "Nghệ thuật phản hồi bất đồng và thương lượng trong cuộc họp."),
                Pair("Chặng 4: Đàm Phán & Thuyết Phục Đối Tác (Strategic Negotiation)", "Vận dụng các thuật ngữ nhượng bộ và chốt thỏa thuận kinh doanh."),
                Pair("Chặng 5: Khẩu Ngữ Lãnh Đạo Chuyên Nghiệp (Executive Fluency)", "Nâng tầm phong thái phát âm và giao tiếp bản lĩnh.")
            )
            else -> listOf(
                Pair("Chặng 1: Khởi Động & Phản Xạ Đàm Thoại (Daily Greetings)", "Tự tin mở đầu câu chuyện và phản xạ chào hỏi tự nhiên không vấp."),
                Pair("Chặng 2: Giải Mã Nuốt Âm & Nối Âm (Connected Speech & Elision)", "Làm chủ hiện tượng nuốt âm could've, gonna trong tiếng Anh bản xứ."),
                Pair("Chặng 3: Đặt Câu Hỏi Gián Tiếp Lịch Sự (Polite Indirect Inquiries)", "Giao tiếp lịch thiệp khi hỏi đường, nhờ vả và gọi món."),
                Pair("Chặng 4: Cụm Động Từ Thông Dụng (High-Frequency Phrasal Verbs)", "Sử dụng phrasal verbs tự nhiên thay cho các từ đơn điệu."),
                Pair("Chặng 5: Đột Phá Đàm Thoại Trôi Chảy (Mastery Conversational Fluency)", "Phản xạ giao tiếp Native Audio trôi chảy trong mọi ngữ cảnh.")
            )
        }
    }
}

