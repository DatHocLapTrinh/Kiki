package com.example

import com.example.model.ClozeChoiceSlot
import com.example.model.InjectedAtomicLessonUnit
import com.example.model.ListeningChallengeSlot
import com.example.model.MatchingPairsSlot
import com.example.model.MicrolearningSlotArchetypes
import com.example.model.PairSlotItem
import com.example.model.SentenceBuilderSlot
import com.example.model.SpeakingChallengeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaffoldingCurriculumProgressiveRoadmapTest {

    private fun buildBeginner5LessonCurriculum(): List<InjectedAtomicLessonUnit> {
        // Lesson 1: Core single greetings (Hello, Hi, Goodbye, Thank you)
        val unit1 = InjectedAtomicLessonUnit(
            title = "Bài 1: Từ Vựng Chào Hỏi Căn Bản (Hello & Hi)",
            description = "Làm quen với các từ vựng chào hỏi đơn lẻ cơ bản nhất.",
            level = "Beginner",
            subject = "English",
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào dùng để nói 'Xin chào' thân thiện và thông dụng nhất khi gặp nhau?",
                options = listOf("Hello", "Goodbye", "No", "Sorry"),
                correctIndex = 0,
                explanation = "'Hello' (/həˈloʊ/) là từ chào hỏi cơ bản, phổ biến và lịch sự nhất trong tiếng Anh."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành câu chào hỏi: 'Xin chào Anna' 🧩",
                targetSentence = "Hello Anna",
                wordTokens = listOf("Hello", "Anna"),
                distractorTokens = listOf("Goodbye"),
                explanation = "Cấu trúc ghép câu chào đơn giản: Hello + [Tên người]."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe âm thanh và sắp xếp lại các từ chào hỏi cho đúng 🎧",
                audioSentence = "Hello Anna",
                wordTokens = listOf("Hello", "Anna"),
                explanation = "Luyện nhận diện âm thanh câu chào hỏi ngắn gọn: 'Hello Anna'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép cặp các từ vựng chào hỏi cơ bản với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("hello", "xin chào"),
                    PairSlotItem("hi", "chào thân mật"),
                    PairSlotItem("goodbye", "tạm biệt"),
                    PairSlotItem("thank you", "cảm ơn")
                ),
                explanation = "Ghi nhớ 4 từ vựng giao tiếp căn bản nhất bắt buộc phải biết khi mới học tiếng Anh."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm từ chào hỏi cơ bản chuẩn bản xứ 🎙️",
                targetSentence = "Hello",
                phoneticFocusWord = "Hello",
                explanation = "Phát âm rõ ràng âm /həˈloʊ/ với ngữ điệu tự tin, vui tươi khi bắt đầu giao tiếp."
            )
        )

        // Lesson 2: Polite single words & reactions (Please, Yes, No, Sorry)
        val unit2 = InjectedAtomicLessonUnit(
            title = "Bài 2: Từ Vựng Lịch Sự & Phản Hồi (Please, Yes, No)",
            description = "Học các từ vựng lịch sự để phản hồi trong giao tiếp.",
            level = "Beginner",
            subject = "English",
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi bạn muốn nói 'Làm ơn / Xin vui lòng' một cách lịch sự, bạn dùng từ nào?",
                options = listOf("Please", "Yes", "No", "Bye"),
                correctIndex = 0,
                explanation = "'Please' (/pliːz/) nghĩa là làm ơn, xin vui lòng, thể hiện sự nhã nhặn và lịch thiệp."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành câu phản hồi lịch sự: 'Vâng, làm ơn' 🧩",
                targetSentence = "Yes please",
                wordTokens = listOf("Yes", "please"),
                distractorTokens = listOf("No"),
                explanation = "Cách đồng ý lời mời hoặc đề nghị lịch sự: 'Yes, please'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe lời cảm ơn và sắp xếp lại các từ cho đúng 🎧",
                audioSentence = "Thank you",
                wordTokens = listOf("Thank", "you"),
                explanation = "Luyện nhận diện âm thanh lời cảm ơn: 'Thank you'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép cặp các từ phản hồi lịch sự với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("please", "làm ơn / xin vui lòng"),
                    PairSlotItem("yes", "vâng / có"),
                    PairSlotItem("no", "không"),
                    PairSlotItem("sorry", "xin lỗi")
                ),
                explanation = "Các từ phản hồi ngắn gọn, lịch sự dùng hàng ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm lời cảm ơn chuẩn bản xứ 🎙️",
                targetSentence = "Thank you",
                phoneticFocusWord = "Thank",
                explanation = "Đặt nhẹ đầu lưỡi giữa hai hàm răng phát âm âm /θ/ trong 'Thank you'."
            )
        )

        // Lesson 3: 2-3 word phrase building (Good morning, Good night)
        val unit3 = InjectedAtomicLessonUnit(
            title = "Bài 3: Lắp Ghép Cụm Chào Theo Buổi (Good morning)",
            description = "Tập ghép 2-3 từ để chào hỏi theo các buổi trong ngày.",
            level = "Beginner",
            subject = "English",
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Buổi sáng khi gặp bạn bè hoặc đồng nghiệp, bạn nên chào như thế nào?",
                options = listOf("Good morning", "Good night", "Goodbye", "No, thanks"),
                correctIndex = 0,
                explanation = "'Good morning' là lời chào buổi sáng trang trọng và thân thiện."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành lời chào: 'Chào buổi sáng bạn của tôi' 🧩",
                targetSentence = "Good morning friend",
                wordTokens = listOf("Good", "morning", "friend"),
                distractorTokens = listOf("night"),
                explanation = "Ghép cụm từ chào buổi sáng với từ 'friend' (người bạn)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe cụm từ chào buổi sáng và sắp xếp lại các từ 🎧",
                audioSentence = "Good morning",
                wordTokens = listOf("Good", "morning"),
                explanation = "Luyện nghe nhận diện lời chào buổi sáng: 'Good morning'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép cặp lời chào theo buổi với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("good morning", "chào buổi sáng"),
                    PairSlotItem("good night", "chúc ngủ ngon"),
                    PairSlotItem("how are you", "bạn khỏe không"),
                    PairSlotItem("fine", "khỏe / tốt")
                ),
                explanation = "Các cụm từ chào hỏi và hỏi thăm thông dụng theo thời điểm trong ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm lời chào buổi sáng tự nhiên 🎙️",
                targetSentence = "Good morning",
                phoneticFocusWord = "morning",
                explanation = "Nói liền mạch cụm 'Good morning' với ngữ điệu niềm nở."
            )
        )

        // Lesson 4: Short self-introduction 3-4 words (I am Alex, My name is Anna)
        val unit4 = InjectedAtomicLessonUnit(
            title = "Bài 4: Giới Thiệu Bản Thân Ngắn Gọn (I am Alex)",
            description = "Học câu ngắn 3-4 từ để tự giới thiệu tên mình.",
            level = "Beginner",
            subject = "English",
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi tự giới thiệu tên mình là Alex, câu nào sau đây đúng cấu trúc nhất?",
                options = listOf("I am Alex", "You is Alex", "He am Alex", "They is Alex"),
                correctIndex = 0,
                explanation = "Đại từ 'I' (tôi) đi với to be 'am' -> 'I am [Tên]'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu: 'Xin chào, tôi là Alex' 🧩",
                targetSentence = "Hello I am Alex",
                wordTokens = listOf("Hello", "I", "am", "Alex"),
                distractorTokens = listOf("are"),
                explanation = "Cấu trúc giới thiệu tên ngắn gọn: Hello + I am + [Tên]."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu giới thiệu tên sau và sắp xếp lại các từ 🎧",
                audioSentence = "My name is Anna",
                wordTokens = listOf("My", "name", "is", "Anna"),
                explanation = "Luyện nhận diện âm thanh câu giới thiệu tên: 'My name is Anna'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép cặp các từ giới thiệu bản thân với nghĩa tương ứng 🧩",
                pairs = listOf(
                    PairSlotItem("I", "tôi"),
                    PairSlotItem("my name", "tên tôi"),
                    PairSlotItem("am", "là / thì"),
                    PairSlotItem("friend", "người bạn")
                ),
                explanation = "Các từ cấu tạo nên câu tự giới thiệu bản thân cơ bản."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm câu giới thiệu bản thân ngắn gọn 🎙️",
                targetSentence = "Hello I am Alex",
                phoneticFocusWord = "Alex",
                explanation = "Phát âm rõ ràng từng từ: 'Hello, I am Alex'."
            )
        )

        // Lesson 5: Conversational reflex & wrap-up
        val unit5 = InjectedAtomicLessonUnit(
            title = "Bài 5: Phản Xạ Hội Thoại & Vượt Chặng",
            description = "Luyện phản xạ đối đáp và hoàn thành thử thách vượt chặng.",
            level = "Beginner",
            subject = "English",
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi ai đó hỏi bạn 'How are you?' (Bạn khỏe không?), câu trả lời lịch sự và tự nhiên nhất là:",
                options = listOf("I am fine thank you", "Goodbye see you never", "No I am Alex", "My name is fine"),
                correctIndex = 0,
                explanation = "'I am fine, thank you' (Tôi khỏe, cảm ơn bạn) là câu phản hồi lịch thiệp và thông dụng nhất."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu đối đáp: 'Tôi khỏe, cảm ơn bạn' 🧩",
                targetSentence = "I am fine thank you",
                wordTokens = listOf("I", "am", "fine", "thank", "you"),
                distractorTokens = listOf("not"),
                explanation = "Cấu trúc phản xạ giao tiếp tự nhiên: I + am + fine + thank you."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hội thoại làm quen và sắp xếp lại các từ 🎧",
                audioSentence = "Nice to meet you",
                wordTokens = listOf("Nice", "to", "meet", "you"),
                explanation = "Luyện nhận diện câu chào khi làm quen bạn mới: 'Nice to meet you'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép cặp các câu đối đáp hội thoại làm quen 🧩",
                pairs = listOf(
                    PairSlotItem("how are you", "bạn khỏe không"),
                    PairSlotItem("I am fine", "tôi khỏe"),
                    PairSlotItem("nice to meet you", "rất vui được gặp bạn"),
                    PairSlotItem("see you again", "hẹn gặp lại")
                ),
                explanation = "Cặp câu đối đáp phản xạ thông dụng khi làm quen với bạn bè quốc tế."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói trôi chảy câu giao tiếp kết chặng 🎙️",
                targetSentence = "Hello nice to meet you",
                phoneticFocusWord = "meet",
                explanation = "Nói tự tin, rõ ràng: 'Hello, nice to meet you' để hoàn thành chặng học xuất sắc!"
            )
        )

        return listOf(unit1, unit2, unit3, unit4, unit5)
    }

    @Test
    fun testCompleteChapterCurriculumHasExactly25ProgressiveQuestions() {
        val units = buildBeginner5LessonCurriculum()
        assertEquals(5, units.size)

        val allQuestions = units.flatMap { it.toQuestionsList() }
        assertEquals(25, allQuestions.size)
    }

    @Test
    fun testLesson1ZeroKnowledgeScaffoldingIntegrity() {
        val units = buildBeginner5LessonCurriculum()
        val lesson1Questions = units[0].toQuestionsList()
        assertEquals(5, lesson1Questions.size)

        // Multiple choice question tests single word 'Hello'
        assertEquals(MicrolearningSlotArchetypes.CLOZE_CHOICE, lesson1Questions[0].questionType)
        assertTrue(lesson1Questions[0].optionsJson.contains("Hello"))
        assertTrue(lesson1Questions[0].explanation?.contains("Hello") == true)

        // Sentence builder: exactly 2 tokens (Hello Anna) - suitable for zero-knowledge beginner
        assertEquals(MicrolearningSlotArchetypes.SENTENCE_BUILDER, lesson1Questions[1].questionType)
        assertEquals("Hello Anna", lesson1Questions[1].correctAnswer)

        // Listening challenge: exactly 2 tokens
        assertEquals(MicrolearningSlotArchetypes.LISTENING_CHALLENGE, lesson1Questions[2].questionType)
        assertEquals("Hello Anna", lesson1Questions[2].correctAnswer)

        // Matching pairs: single basic words
        assertEquals(MicrolearningSlotArchetypes.MATCHING_PAIRS, lesson1Questions[3].questionType)
        assertTrue(lesson1Questions[3].optionsJson.contains("hello=xin chào"))
        assertTrue(lesson1Questions[3].optionsJson.contains("goodbye=tạm biệt"))

        // Speaking: single word 'Hello'
        assertEquals(MicrolearningSlotArchetypes.SPEAKING_CHALLENGE, lesson1Questions[4].questionType)
        assertEquals("Hello", lesson1Questions[4].correctAnswer)
    }

    @Test
    fun testProgressivePaginationPreventsQuestionRepetitionAcrossLessons() {
        val units = buildBeginner5LessonCurriculum()
        val allQuestions = units.flatMap { it.toQuestionsList() }
        assertEquals(25, allQuestions.size)

        val pageSize = 5

        // Lesson 1 pagination: [0..4]
        val l1Start = (1 - 1) * pageSize
        val l1Questions = allQuestions.subList(l1Start, l1Start + pageSize)

        // Lesson 2 pagination: [5..9]
        val l2Start = (2 - 1) * pageSize
        val l2Questions = allQuestions.subList(l2Start, l2Start + pageSize)

        // Lesson 3 pagination: [10..14]
        val l3Start = (3 - 1) * pageSize
        val l3Questions = allQuestions.subList(l3Start, l3Start + pageSize)

        // Lesson 4 pagination: [15..19]
        val l4Start = (4 - 1) * pageSize
        val l4Questions = allQuestions.subList(l4Start, l4Start + pageSize)

        // Lesson 5 pagination: [20..24]
        val l5Start = (5 - 1) * pageSize
        val l5Questions = allQuestions.subList(l5Start, l5Start + pageSize)

        // Verify each lesson has exactly 5 questions
        assertEquals(5, l1Questions.size)
        assertEquals(5, l2Questions.size)
        assertEquals(5, l3Questions.size)
        assertEquals(5, l4Questions.size)
        assertEquals(5, l5Questions.size)

        // Verify Lesson 1 and Lesson 2 DO NOT SHARE questions (Zero repetition)
        assertNotEquals(l1Questions[0].questionText, l2Questions[0].questionText)
        assertNotEquals(l1Questions[1].correctAnswer, l2Questions[1].correctAnswer)
        assertNotEquals(l1Questions[2].correctAnswer, l2Questions[2].correctAnswer)
        assertNotEquals(l1Questions[3].optionsJson, l2Questions[3].optionsJson)
        assertNotEquals(l1Questions[4].correctAnswer, l2Questions[4].correctAnswer)

        // Verify progression:
        // L1: "Hello"
        assertEquals("Hello Anna", l1Questions[1].correctAnswer)
        // L2: "Yes please"
        assertEquals("Yes please", l2Questions[1].correctAnswer)
        // L3: "Good morning friend"
        assertEquals("Good morning friend", l3Questions[1].correctAnswer)
        // L4: "Hello I am Alex"
        assertEquals("Hello I am Alex", l4Questions[1].correctAnswer)
        // L5: "I am fine thank you"
        assertEquals("I am fine thank you", l5Questions[1].correctAnswer)
    }
}
