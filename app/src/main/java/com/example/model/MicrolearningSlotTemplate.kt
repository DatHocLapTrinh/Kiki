package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Kiến trúc Khuôn Mẫu Rỗng (Slot-Based Question Templates) cho AI Microlearning
 * Ứng dụng đóng vai trò là Hệ thống Trình diễn Khuôn Mẫu (Template Presentation Engine).
 * AI Gemini đóng vai trò là Bộ Não Bơm Dữ Liệu Tức Thời (Real-time Payload Injector).
 */
object MicrolearningSlotArchetypes {
    const val CLOZE_CHOICE = "MULTIPLE_CHOICE"
    const val SENTENCE_BUILDER = "SENTENCE_BUILDER"
    const val LISTENING_CHALLENGE = "LISTENING_CHALLENGE"
    const val MATCHING_PAIRS = "MATCHING_PAIRS"
    const val SPEAKING_CHALLENGE = "SPEAKING_CHALLENGE"
}

internal fun listToJsonArray(items: List<String>): String {
    if (items.isEmpty()) return "[]"
    return items.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]") { item ->
        item.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}

@JsonClass(generateAdapter = true)
data class ClozeChoiceSlot(
    @param:Json(name = "promptQuestion") val promptQuestion: String,
    @param:Json(name = "options") val options: List<String>,
    @param:Json(name = "correctIndex") val correctIndex: Int = 0,
    @param:Json(name = "explanation") val explanation: String = "",
    @param:Json(name = "targetSkillTag") val targetSkillTag: String? = null
) {
    fun toQuestionItemDto(): QuestionItemDto {
        return QuestionItemDto(
            questionText = promptQuestion,
            questionType = MicrolearningSlotArchetypes.CLOZE_CHOICE,
            optionsJson = listToJsonArray(options),
            correctAnswer = correctIndex.toString(),
            explanation = explanation,
            points = 10,
            difficulty = "EASY"
        )
    }
}

@JsonClass(generateAdapter = true)
data class SentenceBuilderSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String,
    @param:Json(name = "targetSentence") val targetSentence: String,
    @param:Json(name = "wordTokens") val wordTokens: List<String>,
    @param:Json(name = "distractorTokens") val distractorTokens: List<String> = emptyList(),
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        val allTokens = (wordTokens + distractorTokens).shuffled()
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.SENTENCE_BUILDER,
            optionsJson = listToJsonArray(allTokens),
            correctAnswer = targetSentence,
            explanation = explanation,
            points = 15,
            difficulty = "MEDIUM"
        )
    }
}

@JsonClass(generateAdapter = true)
data class ListeningChallengeSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String = "Nghe câu sau và sắp xếp lại các từ cho đúng thứ tự 🎧",
    @param:Json(name = "audioSentence") val audioSentence: String,
    @param:Json(name = "wordTokens") val wordTokens: List<String> = emptyList(),
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        val tokens = if (wordTokens.isNotEmpty()) {
            wordTokens.shuffled()
        } else {
            audioSentence.split(Regex("\\s+"))
                .map { it.trim().trim('.', ',', '!', '?', ';', ':', '"') }
                .filter { it.isNotEmpty() }
                .shuffled()
        }
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.LISTENING_CHALLENGE,
            optionsJson = listToJsonArray(listOf(audioSentence)),
            correctAnswer = audioSentence,
            explanation = explanation,
            points = 10,
            difficulty = "MEDIUM"
        )
    }
}

@JsonClass(generateAdapter = true)
data class PairSlotItem(
    @param:Json(name = "english") val english: String,
    @param:Json(name = "vietnamese") val vietnamese: String
)

@JsonClass(generateAdapter = true)
data class MatchingPairsSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String = "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng 🧩",
    @param:Json(name = "pairs") val pairs: List<PairSlotItem>,
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        val formattedPairs = pairs.map { "${it.english}=${it.vietnamese}" }
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.MATCHING_PAIRS,
            optionsJson = listToJsonArray(formattedPairs),
            correctAnswer = "0",
            explanation = explanation,
            points = 10,
            difficulty = "MEDIUM"
        )
    }
}

@JsonClass(generateAdapter = true)
data class SpeakingChallengeSlot(
    @param:Json(name = "promptInstruction") val promptInstruction: String = "Luyện nói câu tiếng Anh sau chuẩn bản xứ 🎙️",
    @param:Json(name = "targetSentence") val targetSentence: String,
    @param:Json(name = "phoneticFocusWord") val phoneticFocusWord: String? = null,
    @param:Json(name = "explanation") val explanation: String = ""
) {
    fun toQuestionItemDto(): QuestionItemDto {
        return QuestionItemDto(
            questionText = promptInstruction,
            questionType = MicrolearningSlotArchetypes.SPEAKING_CHALLENGE,
            optionsJson = listToJsonArray(listOf(targetSentence)),
            correctAnswer = targetSentence,
            explanation = explanation,
            points = 20,
            difficulty = "HARD"
        )
    }
}

/**
 * Đơn vị Bài học Nguyên tử 3C gồm 5 Slot hoàn chỉnh được bơm dữ liệu động từ AI
 */
@JsonClass(generateAdapter = true)
data class InjectedAtomicLessonUnit(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "level") val level: String,
    @param:Json(name = "subject") val subject: String = "English",
    @param:Json(name = "slot1Cloze") val slot1Cloze: ClozeChoiceSlot,
    @param:Json(name = "slot2SentenceBuilder") val slot2SentenceBuilder: SentenceBuilderSlot,
    @param:Json(name = "slot3Listening") val slot3Listening: ListeningChallengeSlot,
    @param:Json(name = "slot4Matching") val slot4Matching: MatchingPairsSlot,
    @param:Json(name = "slot5Speaking") val slot5Speaking: SpeakingChallengeSlot
) {
    fun toQuestionsList(): List<QuestionItemDto> {
        return listOf(
            slot1Cloze.toQuestionItemDto(),
            slot2SentenceBuilder.toQuestionItemDto(),
            slot3Listening.toQuestionItemDto(),
            slot4Matching.toQuestionItemDto(),
            slot5Speaking.toQuestionItemDto()
        )
    }

    fun toChapterGeneratedResponse(orderNo: Int = 1): ChapterGeneratedResponse {
        return ChapterGeneratedResponse(
            title = title,
            description = description,
            level = level,
            subject = subject,
            orderNo = orderNo,
            questions = toQuestionsList()
        )
    }
}

/**
 * Kiến trúc Lộ Trình Xoắn Ốc Đa Tầng (Duolingo-Grade Spiral Curriculum & i+1 Scaffolding Blueprint).
 *
 * Nguyên lý thiết kế sư phạm:
 * 1. Mỗi Chặng (Chapter/Stage) gồm ĐÚNG 5 Bài học (5 Lessons x 5 Questions = 25 câu hỏi tuần tự).
 * 2. Trong mỗi Chặng, 5 Bài học tuân thủ nghiêm ngặt quy tắc Bắc cầu (Scaffolding 5 bước):
 *    - Bài 1 (Node 1): Core Vocabulary Recognition (Nhận diện từ đơn cốt lõi của chủ đề).
 *    - Bài 2 (Node 2): Lexical Expansion & Verbs/Modifiers (Mở rộng động từ, tính từ, từ phản hồi).
 *    - Bài 3 (Node 3): Phrase & Chunk Assembly (Ghép cụm 2-3 từ / Collocations theo chủ đề).
 *    - Bài 4 (Node 4): Complete Sentence Construction (Ghép câu hoàn chỉnh 3-6 từ).
 *    - Bài 5 (Node 5): Conversational Reflex & Mastery Synthesis (Phản xạ hội thoại thực tế & Ôn tập).
 * 3. Phân hóa theo TỪNG CHỦ ĐỀ (Topic-Aware) và TỪNG CẤP ĐỘ CEFR (A0 -> A1 -> A2 -> B1 -> B2 -> C1),
 *    đảm bảo người học đi qua các chặng sẽ học từ vựng và cấu trúc MỚI hoàn toàn, kết hợp ôn tập xoắn ốc (Spiral Recycling).
 */
object SpiralCurriculumBlueprint {

    enum class CurriculumTheme {
        BEGINNER_STAGE_1_GREETINGS,
        BEGINNER_STAGE_2_IDENTITY_JOBS,
        BEGINNER_STAGE_3_FOOD_ESSENTIALS,
        BEGINNER_STAGE_4_NUMBERS_TIME,
        BEGINNER_STAGE_5_FAMILY_HOBBIES,
        BEGINNER_STAGE_6_PLACES_ROUTINES,
        INTERMEDIATE_STAGE_1_CONNECTED_SPEECH,
        INTERMEDIATE_STAGE_2_POLITE_INQUIRIES,
        INTERMEDIATE_STAGE_3_PHRASAL_VERBS,
        INTERMEDIATE_STAGE_4_CONDITIONALS_SYNTAX,
        ADVANCED_STAGE_1_INVERSION_RHETORIC,
        ADVANCED_STAGE_2_NEGOTIATION_ACADEMIC
    }

    /**
     * Phân loại chủ đề (topic) và cấp độ (level) vào đúng mô-đun kiến thức chuyên biệt.
     */
    fun resolveTheme(level: String, topic: String): CurriculumTheme {
        val t = topic.lowercase()
        val isBeginner = level.equals("Beginner", ignoreCase = true) ||
                         level.equals("A1", ignoreCase = true) ||
                         level.equals("A0", ignoreCase = true)
        val isAdvanced = level.equals("Advanced", ignoreCase = true) ||
                         level.equals("C1", ignoreCase = true) ||
                         level.equals("B2", ignoreCase = true)

        if (isBeginner) {
            return when {
                t.contains("chặng 2") || t.contains("giới thiệu") || t.contains("identity") ||
                t.contains("chức danh") || t.contains("job") || t.contains("to be") ||
                t.contains("who i am") || t.contains("how are you") ->
                    CurriculumTheme.BEGINNER_STAGE_2_IDENTITY_JOBS

                t.contains("chặng 3") || t.contains("nhu cầu") || t.contains("đồ vật") ||
                t.contains("essential") || t.contains("office item") || t.contains("food") ||
                t.contains("món ăn") || t.contains("đồ uống") || t.contains("drink") ||
                t.contains("this, that") ->
                    CurriculumTheme.BEGINNER_STAGE_3_FOOD_ESSENTIALS

                t.contains("chặng 4") || t.contains("số đếm") || t.contains("thời gian") ||
                t.contains("giờ giấc") || t.contains("mua sắm") || t.contains("number") ||
                t.contains("time") || t.contains("schedule") || t.contains("dictation") ->
                    CurriculumTheme.BEGINNER_STAGE_4_NUMBERS_TIME

                t.contains("chặng 5") || t.contains("gia đình") || t.contains("family") ||
                t.contains("sở thích") || t.contains("hobbies") || t.contains("bạn bè") ||
                t.contains("đàm thoại ngắn") || t.contains("short daily") ||
                t.contains("workplace dialogue") || t.contains("yes/no") || t.contains("q&a") ->
                    CurriculumTheme.BEGINNER_STAGE_5_FAMILY_HOBBIES

                t.contains("hỏi đường") || t.contains("nơi chốn") || t.contains("place") ||
                t.contains("direction") || t.contains("màu sắc") || t.contains("trang phục") ||
                t.contains("color") || t.contains("action verb") || t.contains("tổng hợp") ||
                t.contains("milestone") || t.contains("stage 6") || t.contains("stage 7") ->
                    CurriculumTheme.BEGINNER_STAGE_6_PLACES_ROUTINES

                else -> CurriculumTheme.BEGINNER_STAGE_1_GREETINGS
            }
        }

        if (isAdvanced) {
            return when {
                t.contains("negotiation") || t.contains("đàm phán") || t.contains("business") ||
                t.contains("diplomacy") || t.contains("academic") || t.contains("ielts") ||
                t.contains("lexicon") || t.contains("học thuật") || t.contains("presentation") ||
                t.contains("executive") || t.contains("lãnh đạo") ->
                    CurriculumTheme.ADVANCED_STAGE_2_NEGOTIATION_ACADEMIC

                else -> CurriculumTheme.ADVANCED_STAGE_1_INVERSION_RHETORIC
            }
        }

        // Intermediate (B1-B2)
        return when {
            t.contains("gián tiếp") || t.contains("indirect") || t.contains("polite") ||
            t.contains("lịch sự") || t.contains("etiquette") || t.contains("công sở") ||
            t.contains("travel") ->
                CurriculumTheme.INTERMEDIATE_STAGE_2_POLITE_INQUIRIES

            t.contains("cụm động từ") || t.contains("phrasal") || t.contains("idiom") ||
            t.contains("vocabulary") || t.contains("paraphras") || t.contains("collocation") ->
                CurriculumTheme.INTERMEDIATE_STAGE_3_PHRASAL_VERBS

            t.contains("điều kiện") || t.contains("conditional") || t.contains("syntax") ||
            t.contains("cấu trúc") || t.contains("clause") || t.contains("ngữ pháp") ||
            t.contains("complex") || t.contains("pitching") ->
                CurriculumTheme.INTERMEDIATE_STAGE_4_CONDITIONALS_SYNTAX

            else -> CurriculumTheme.INTERMEDIATE_STAGE_1_CONNECTED_SPEECH
        }
    }

    /**
     * Trả về hướng dẫn từ vựng & cấu trúc chuyên biệt theo từng chủ đề để bơm vào Prompt của Gemini 2.5 Flash.
     */
    fun buildTopicSpecificGeminiGuidance(level: String, topic: String): String {
        return when (resolveTheme(level, topic)) {
            CurriculumTheme.BEGINNER_STAGE_1_GREETINGS -> """
                TOPIC FOCUS: STAGE 1 - BASIC GREETINGS & FIRST WORDS (A0.1)
                - Lesson 1 (Q1-Q5): Single greeting words ONLY (Hello, Hi, Goodbye, Thank you).
                - Lesson 2 (Q6-Q10): Single polite reaction words (Please, Yes, No, Sorry).
                - Lesson 3 (Q11-Q15): 2-word time greetings (Good morning, Good afternoon, Good evening, Good night).
                - Lesson 4 (Q16-Q20): 3-4 word self-introduction sentences (I am Alex, My name is Anna).
                - Lesson 5 (Q21-Q25): Short greeting exchange (How are you? I am fine thank you, Nice to meet you).
            """.trimIndent()

            CurriculumTheme.BEGINNER_STAGE_2_IDENTITY_JOBS -> """
                TOPIC FOCUS: STAGE 2 - PERSONAL IDENTITY, PRONOUNS, JOBS & ORIGIN (A0.2)
                - Lesson 1 (Q1-Q5): Core pronouns & 'to be' (I am, You are, He is, She is).
                - Lesson 2 (Q6-Q10): Basic identity/job/country nouns (student, teacher, doctor, friend, Vietnam).
                - Lesson 3 (Q11-Q15): 2-3 word identity phrases (a student, a teacher, my friend, from Vietnam).
                - Lesson 4 (Q16-Q20): 4-word identity sentences (I am a student, She is a doctor, I am from Vietnam).
                - Lesson 5 (Q21-Q25): Identity & origin Q&A dialogue (Where are you from? I am from Vietnam, Are you a teacher?).
            """.trimIndent()

            CurriculumTheme.BEGINNER_STAGE_3_FOOD_ESSENTIALS -> """
                TOPIC FOCUS: STAGE 3 - FOOD, DRINKS & EVERYDAY ESSENTIALS (A1.1)
                - Lesson 1 (Q1-Q5): Core food & drink nouns (water, coffee, tea, milk, bread, rice).
                - Lesson 2 (Q6-Q10): Core need verbs & modifiers (want, like, eat, drink, hot, cold).
                - Lesson 3 (Q11-Q15): 2-3 word food/drink chunks (hot coffee, cold water, drink tea, eat bread).
                - Lesson 4 (Q16-Q20): 4-5 word polite request sentences (I want some water please, I like hot coffee).
                - Lesson 5 (Q21-Q25): Café/dining request dialogue (Can I have a coffee please? Yes here you are).
            """.trimIndent()

            CurriculumTheme.BEGINNER_STAGE_4_NUMBERS_TIME -> """
                TOPIC FOCUS: STAGE 4 - NUMBERS, TIME, SCHEDULES & SHOPPING (A1.2)
                - Lesson 1 (Q1-Q5): Core numbers 1-10 (one, two, three, five, eight, ten).
                - Lesson 2 (Q6-Q10): Time & shopping words (time, clock, today, now, dollar, book).
                - Lesson 3 (Q11-Q15): 2-3 word time/price chunks (five dollars, eight o'clock, what time, how much).
                - Lesson 4 (Q16-Q20): 4-5 word time/price sentences (It is eight o'clock now, This book is five dollars).
                - Lesson 5 (Q21-Q25): Asking time & price dialogue (What time is it now? How much is this coffee?).
            """.trimIndent()

            CurriculumTheme.BEGINNER_STAGE_5_FAMILY_HOBBIES -> """
                TOPIC FOCUS: STAGE 5 - FAMILY, HOBBIES & SHORT DAILY DIALOGUES (A1.3)
                - Lesson 1 (Q1-Q5): Core family nouns (mother, father, brother, sister, family, home).
                - Lesson 2 (Q6-Q10): Feelings & hobby words (love, happy, kind, music, read, book).
                - Lesson 3 (Q11-Q15): 2-3 word family/hobby phrases (my mother, happy family, read books, love music).
                - Lesson 4 (Q16-Q20): 4-5 word family/hobby sentences (I love my family, My mother is very kind).
                - Lesson 5 (Q21-Q25): Daily life & hobby Q&A dialogue (Do you like music? Yes I love music very much).
            """.trimIndent()

            CurriculumTheme.BEGINNER_STAGE_6_PLACES_ROUTINES -> """
                TOPIC FOCUS: STAGE 6 - PLACES, DIRECTIONS & DAILY ROUTINES BRIDGE TO A2 (A2.1)
                - Lesson 1 (Q1-Q5): Core places & transport words (airport, hotel, station, bus, taxi, street).
                - Lesson 2 (Q6-Q10): Direction & routine verbs (go, work, turn left, turn right, straight, near).
                - Lesson 3 (Q11-Q15): 2-3 word navigation/routine phrases (go to work, at the station, by taxi, turn left).
                - Lesson 4 (Q16-Q20): 5-6 word sentences (I go to work by bus, Where is the train station?).
                - Lesson 5 (Q21-Q25): Practical travel & direction dialogue (Excuse me where is the hotel? Turn left and go straight).
            """.trimIndent()

            CurriculumTheme.INTERMEDIATE_STAGE_1_CONNECTED_SPEECH -> """
                TOPIC FOCUS: INTERMEDIATE STAGE 1 - CONNECTED SPEECH, ELISION & COLLOCATIONS (B1.1)
                - Progressive 5 lessons covering native reductions (could've, should've, gonna, wanna), natural collocations (make a decision, take a break), and fluent conversational listening/speaking.
            """.trimIndent()

            CurriculumTheme.INTERMEDIATE_STAGE_2_POLITE_INQUIRIES -> """
                TOPIC FOCUS: INTERMEDIATE STAGE 2 - POLITE INDIRECT INQUIRIES & WORKPLACE ETIQUETTE (B1.2)
                - Progressive 5 lessons covering polite modal openers (Could you tell me, Would you mind), embedded question word order (where the station is, what time the meeting starts), and diplomatic requests.
            """.trimIndent()

            CurriculumTheme.INTERMEDIATE_STAGE_3_PHRASAL_VERBS -> """
                TOPIC FOCUS: INTERMEDIATE STAGE 3 - HIGH-FREQUENCY PHRASAL VERBS & NATURAL IDIOMS (B1.3)
                - Progressive 5 lessons covering essential phrasal verbs (look forward to, figure out, run out of, put off, bring up, carry on, turn down) from recognition to full conversational usage.
            """.trimIndent()

            CurriculumTheme.INTERMEDIATE_STAGE_4_CONDITIONALS_SYNTAX -> """
                TOPIC FOCUS: INTERMEDIATE STAGE 4 - CONDITIONALS, COMPLEX CLAUSES & DISCOURSE (B2.1)
                - Progressive 5 lessons covering Second & Third Conditionals (If I were you, Had I known), concession connectors (despite, although, nevertheless), and structured persuasion.
            """.trimIndent()

            CurriculumTheme.ADVANCED_STAGE_1_INVERSION_RHETORIC -> """
                TOPIC FOCUS: ADVANCED STAGE 1 - SYNTACTIC INVERSION & EXECUTIVE RHETORIC (B2-C1)
                - Progressive 5 lessons covering negative adverbial inversion (Seldom have I seen, Not only did she..., Under no circumstances), emphatic cleft sentences, and executive debate cadence.
            """.trimIndent()

            CurriculumTheme.ADVANCED_STAGE_2_NEGOTIATION_ACADEMIC -> """
                TOPIC FOCUS: ADVANCED STAGE 2 - STRATEGIC NEGOTIATION, DIPLOMACY & ACADEMIC LEXICON (C1)
                - Progressive 5 lessons covering high-stakes negotiation vocabulary (mitigate, consensus, unprecedented, feasible, substantiate), diplomatic hedging, and IELTS/Executive synthesis.
            """.trimIndent()
        }
    }


    /**
     * Khởi tạo trọn vẹn 1 Chương học (5 Bài học x 5 Câu hỏi = 25 Câu hỏi tuần tự)
     * khớp chính xác với Chủ đề (topic), Cấp độ (level) và Điểm yếu (weakWords) của người học.
     */
    fun generateTopicAwareChapter(
        level: String,
        topic: String,
        weakWords: List<String> = emptyList(),
        isEng: Boolean = false
    ): ChapterGeneratedResponse {
        val displayLevel = if (level.isBlank()) "Beginner" else level
        val cleanTopic = if (topic.isBlank()) "Daily Conversations" else topic.trim()
        val theme = resolveTheme(displayLevel, cleanTopic)

        val (defaultTitleVi, defaultTitleEn, descVi, descEn, units) = when (theme) {
            CurriculumTheme.BEGINNER_STAGE_1_GREETINGS -> Quintuple(
                "Lộ trình AI: Chào Hỏi & Làm Quen Cơ Bản ($displayLevel)",
                "AI Roadmap: Hello & Daily Greetings ($displayLevel)",
                "Lộ trình 5 bài học từ số 0: Từ đơn chào hỏi -> Từ lịch sự -> Cụm chào theo buổi -> Câu giới thiệu tên -> Hội thoại làm quen.",
                "5-step zero-knowledge progression: Single greetings -> Polite words -> Time phrases -> Name sentences -> Greeting dialogue.",
                buildBeginnerStage1Greetings(displayLevel, isEng)
            )
            CurriculumTheme.BEGINNER_STAGE_2_IDENTITY_JOBS -> Quintuple(
                "Lộ trình AI: Giới Thiệu Bản Thân & Nghề Nghiệp ($displayLevel)",
                "AI Roadmap: Personal Identity & Jobs ($displayLevel)",
                "Lộ trình 5 bài học tuần tự: Đại từ & To Be -> Danh từ nghề nghiệp -> Cụm giới thiệu quê quán -> Câu hoàn chỉnh -> Hội thoại hỏi thăm.",
                "5-step progression: Pronouns & To Be -> Job nouns -> Origin phrases -> Identity sentences -> Personal Q&A.",
                buildBeginnerStage2Identity(displayLevel, isEng)
            )
            CurriculumTheme.BEGINNER_STAGE_3_FOOD_ESSENTIALS -> Quintuple(
                "Lộ trình AI: Đồ Ăn, Thức Uống & Nhu Cầu Thiết Yếu ($displayLevel)",
                "AI Roadmap: Food, Drinks & Essentials ($displayLevel)",
                "Lộ trình 5 bài học tuần tự: Danh từ đồ ăn/thức uống -> Động từ nhu cầu -> Cụm danh từ -> Câu gọi món lịch sự -> Hội thoại nhà hàng.",
                "5-step progression: Food/drink nouns -> Need verbs -> Noun chunks -> Polite requests -> Ordering dialogue.",
                buildBeginnerStage3FoodEssentials(displayLevel, isEng)
            )
            CurriculumTheme.BEGINNER_STAGE_4_NUMBERS_TIME -> Quintuple(
                "Lộ trình AI: Số Đếm, Thời Gian & Mua Sắm Cơ Bản ($displayLevel)",
                "AI Roadmap: Numbers, Time & Shopping ($displayLevel)",
                "Lộ trình 5 bài học tuần tự: Số đếm 1-10 -> Từ vựng thời gian/giá cả -> Cụm hỏi giờ & giá -> Câu thông báo giờ/giá -> Hội thoại mua sắm.",
                "5-step progression: Core numbers -> Time/price words -> Question chunks -> Time/price sentences -> Shopping dialogue.",
                buildBeginnerStage4NumbersTime(displayLevel, isEng)
            )
            CurriculumTheme.BEGINNER_STAGE_5_FAMILY_HOBBIES -> Quintuple(
                "Lộ trình AI: Gia Đình, Sở Thích & Đàm Thoại Đời Sống ($displayLevel)",
                "AI Roadmap: Family, Hobbies & Daily Dialogues ($displayLevel)",
                "Lộ trình 5 bài học tuần tự: Từ vựng gia đình -> Tính từ & sở thích -> Cụm sở hữu -> Câu mô tả người thân -> Đàm thoại đời sống.",
                "5-step progression: Family words -> Feelings & hobbies -> Possessive chunks -> Family sentences -> Daily life dialogue.",
                buildBeginnerStage5FamilyHobbies(displayLevel, isEng)
            )
            CurriculumTheme.BEGINNER_STAGE_6_PLACES_ROUTINES -> Quintuple(
                "Lộ trình AI: Nơi Chốn, Hỏi Đường & Hoạt Động Hàng Ngày ($displayLevel)",
                "AI Roadmap: Places, Directions & Daily Routines ($displayLevel)",
                "Lộ trình 5 bài học bắc cầu lên A2: Địa điểm & phương tiện -> Động từ chỉ hướng -> Cụm di chuyển -> Câu hỏi đường -> Giao tiếp du lịch.",
                "5-step A2 bridge progression: Places & transport -> Direction verbs -> Navigation chunks -> Routine sentences -> Travel dialogue.",
                buildBeginnerStage6PlacesRoutines(displayLevel, isEng)
            )
            CurriculumTheme.INTERMEDIATE_STAGE_1_CONNECTED_SPEECH -> Quintuple(
                "Lộ trình AI: Giải Mã Nuốt Âm & Phản Xạ Tự Nhiên ($displayLevel)",
                "AI Roadmap: Connected Speech & Natural Collocations ($displayLevel)",
                "Lộ trình 5 bài học B1: Collocations cốt lõi -> Nuốt âm (could've/gonna) -> Cụm phản xạ -> Câu giao tiếp tự nhiên -> Hội thoại bản xứ.",
                "5-step B1 progression: Core collocations -> Native reductions -> Conversational chunks -> Natural sentences -> Native flow.",
                buildIntermediateStage1ConnectedSpeech(displayLevel, isEng)
            )
            CurriculumTheme.INTERMEDIATE_STAGE_2_POLITE_INQUIRIES -> Quintuple(
                "Lộ trình AI: Câu Hỏi Gián Tiếp & Giao Tiếp Công Sở ($displayLevel)",
                "AI Roadmap: Polite Indirect Inquiries & Etiquette ($displayLevel)",
                "Lộ trình 5 bài học B1+: Mở đầu lịch sự -> Từ nối mệnh đề -> Cụm yêu cầu trang trọng -> Câu hỏi gián tiếp hoàn chỉnh -> Ngoại giao nơi làm việc.",
                "5-step B1+ progression: Polite openers -> Embedded connectors -> Formal request chunks -> Indirect questions -> Workplace etiquette.",
                buildIntermediateStage2PoliteInquiries(displayLevel, isEng)
            )
            CurriculumTheme.INTERMEDIATE_STAGE_3_PHRASAL_VERBS -> Quintuple(
                "Lộ trình AI: Cụm Động Từ Thông Dụng & Thành Ngữ ($displayLevel)",
                "AI Roadmap: High-Frequency Phrasal Verbs & Idioms ($displayLevel)",
                "Lộ trình 5 bài học B1-B2: Phrasal verbs thiết yếu -> Cụm động từ công việc -> Kết hợp tân ngữ -> Câu hoàn chỉnh -> Phản xạ hội thoại.",
                "5-step B1-B2 progression: Core phrasal verbs -> Work phrasal verbs -> Verb-object chunks -> Contextual sentences -> Fluent synthesis.",
                buildIntermediateStage3PhrasalVerbs(displayLevel, isEng)
            )
            CurriculumTheme.INTERMEDIATE_STAGE_4_CONDITIONALS_SYNTAX -> Quintuple(
                "Lộ trình AI: Câu Điều Kiện Giả Định & Cú Pháp Phức ($displayLevel)",
                "AI Roadmap: Conditionals & Complex Syntax ($displayLevel)",
                "Lộ trình 5 bài học B2: Liên từ nhân quả & nhượng bộ -> Điều kiện loại 2 -> Điều kiện loại 3 -> Mệnh đề rút gọn -> Tranh biện thuyết phục.",
                "5-step B2 progression: Discourse markers -> Second conditional -> Third conditional -> Reduced clauses -> Persuasive syntax.",
                buildIntermediateStage4ConditionalsSyntax(displayLevel, isEng)
            )
            CurriculumTheme.ADVANCED_STAGE_1_INVERSION_RHETORIC -> Quintuple(
                "Lộ trình AI: Đảo Ngữ, Cú Pháp Nâng Cao & Hùng Biện ($displayLevel)",
                "AI Roadmap: Syntactic Inversion & Executive Rhetoric ($displayLevel)",
                "Lộ trình 5 bài học B2-C1: Trạng từ phủ định -> Trợ động từ đảo ngữ -> Cấu trúc Song hành Not only... but also -> Câu đảo ngữ hoàn chỉnh -> Hùng biện lãnh đạo.",
                "5-step C1 progression: Negative adverbials -> Auxiliary inversion -> Correlative clauses -> Full inverted syntax -> Executive rhetoric.",
                buildAdvancedStage1Inversion(displayLevel, isEng)
            )
            CurriculumTheme.ADVANCED_STAGE_2_NEGOTIATION_ACADEMIC -> Quintuple(
                "Lộ trình AI: Đàm Phán Chiến Lược & Từ Vựng Học Thuật ($displayLevel)",
                "AI Roadmap: Strategic Negotiation & Academic Lexicon ($displayLevel)",
                "Lộ trình 5 bài học C1: Thuật ngữ học thuật/IELTS -> Động từ đàm phán -> Cụm rào đón ngoại giao -> Luận điểm chiến lược -> Đàm phán cấp cao.",
                "5-step C1 progression: Academic lexicon -> Negotiation verbs -> Diplomatic hedging -> Strategic arguments -> Executive capstone.",
                buildAdvancedStage2Negotiation(displayLevel, isEng)
            )
        }

        val finalTitle = if (cleanTopic.startsWith("Chặng", ignoreCase = true) ||
                             cleanTopic.startsWith("Lộ trình", ignoreCase = true) ||
                             cleanTopic.startsWith("AI Roadmap", ignoreCase = true) ||
                             cleanTopic.contains("(")) {
            cleanTopic
        } else {
            if (isEng) defaultTitleEn else defaultTitleVi
        }

        val weakSuffix = if (weakWords.isNotEmpty()) {
            val focus = weakWords.take(3).joinToString(", ")
            if (isEng) " (Remediating focus: $focus)" else " (Tích hợp củng cố: $focus)"
        } else ""

        val finalDesc = (if (isEng) descEn else descVi) + weakSuffix
        val allQuestions = units.flatMap { it.toQuestionsList() }.toMutableList()

        // Nếu người học có từ yếu (weakWords), đan cài ghi chú ôn tập thông minh vào bài tổng hợp cuối chương
        if (weakWords.isNotEmpty() && allQuestions.isNotEmpty()) {
            val lastIdx = allQuestions.lastIndex
            val lastQ = allQuestions[lastIdx]
            val focusWords = weakWords.take(3).joinToString(", ")
            allQuestions[lastIdx] = lastQ.copy(
                explanation = "${lastQ.explanation} [Kiki AI đã lồng ghép ôn tập điểm yếu của bạn: $focusWords]"
            )
        }

        return ChapterGeneratedResponse(
            title = finalTitle,
            description = finalDesc,
            level = displayLevel,
            subject = "English",
            orderNo = 0,
            questions = allQuestions
        )
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private fun buildBeginnerStage1Greetings(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Core Greetings (Hello & Hi)" else "Bài 1: Từ Vựng Chào Hỏi Căn Bản (Hello & Hi)",
            description = if (isEng) "Step 1: Single greeting words." else "Bước 1: Nhận diện các từ đơn chào hỏi cơ bản nhất.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào dùng để nói 'Xin chào' thân thiện và thông dụng nhất khi gặp nhau?",
                options = listOf("Hello", "Goodbye", "No", "Sorry"),
                correctIndex = 0,
                explanation = "'Hello' (/həˈloʊ/) là từ chào hỏi cơ bản, phổ biến và lịch sự nhất trong tiếng Anh."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành lời chào: 'Xin chào Anna' 🧩",
                targetSentence = "Hello Anna",
                wordTokens = listOf("Hello", "Anna"),
                distractorTokens = listOf("Goodbye"),
                explanation = "Ghép từ chào 'Hello' + tên người 'Anna' để chào trực tiếp."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe âm thanh và sắp xếp lại lời chào cho đúng 🎧",
                audioSentence = "Hello Anna",
                wordTokens = listOf("Hello", "Anna"),
                explanation = "Nhận diện âm thanh hai từ đơn giản: 'Hello Anna'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép 4 từ chào hỏi cơ bản với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("hello", "xin chào"),
                    PairSlotItem("hi", "chào thân mật"),
                    PairSlotItem("goodbye", "tạm biệt"),
                    PairSlotItem("thank you", "cảm ơn")
                ),
                explanation = "4 từ vựng nền tảng đầu tiên cho người mới bắt đầu từ số 0."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Bấm giữ micro và phát âm từ 'Xin chào': Hello 🎙️",
                targetSentence = "Hello",
                phoneticFocusWord = "Hello",
                explanation = "Phát âm 'Hello' (/həˈloʊ/) rõ âm /h/ ở đầu và nhấn trọng âm ở âm tiết thứ hai."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Polite Words (Please & Thank You)" else "Bài 2: Từ Vựng Lịch Sự (Please, Yes, No)",
            description = if (isEng) "Step 2: Polite single words." else "Bước 2: Mở rộng từ đơn lịch sự và phản hồi ngắn.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi được người khác giúp đỡ, bạn nói từ nào để 'Cảm ơn'?",
                options = listOf("Thank you", "Sorry", "Goodbye", "No"),
                correctIndex = 0,
                explanation = "'Thank you' có nghĩa là 'Cảm ơn bạn'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ để nói: 'Vâng, làm ơn' 🧩",
                targetSentence = "Yes please",
                wordTokens = listOf("Yes", "please"),
                distractorTokens = listOf("No"),
                explanation = "'Yes' (Vâng) kết hợp với 'please' (làm ơn) thể hiện sự đồng ý lịch sự."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại lời cảm ơn 🎧",
                audioSentence = "Thank you",
                wordTokens = listOf("Thank", "you"),
                explanation = "Luyện nghe cụm 2 từ 'Thank you'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ phản hồi lịch sự với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("please", "làm ơn / vui lòng"),
                    PairSlotItem("yes", "vâng / đúng"),
                    PairSlotItem("no", "không"),
                    PairSlotItem("sorry", "xin lỗi")
                ),
                explanation = "4 từ phản hồi thiết yếu nhất trong mọi cuộc hội thoại."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói lời cảm ơn: Thank you 🎙️",
                targetSentence = "Thank you",
                phoneticFocusWord = "Thank",
                explanation = "Lưu ý âm /θ/ đầu từ 'Thank' bằng cách đặt nhẹ đầu lưỡi giữa hai hàm răng."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Time Greetings (Good Morning)" else "Bài 3: Cụm Từ Chào Theo Buổi (Good Morning)",
            description = if (isEng) "Step 3: Combine 2 words into time greetings." else "Bước 3: Tập ghép 2 từ đơn thành cụm chào hỏi theo buổi.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để chào 'Chào buổi sáng' bằng tiếng Anh, bạn ghép từ 'Good' với từ nào?",
                options = listOf("Good morning", "Good night", "Thank you", "Goodbye"),
                correctIndex = 0,
                explanation = "'Good' (tốt lành) + 'morning' (buổi sáng) = 'Good morning' (Chào buổi sáng)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm 'Chào buổi sáng' 🧩",
                targetSentence = "Good morning",
                wordTokens = listOf("Good", "morning"),
                distractorTokens = listOf("night", "Sorry"),
                explanation = "Cụm từ chào buổi sáng chuẩn mực: 'Good morning'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại cụm từ chúc ngủ ngon 🎧",
                audioSentence = "Good night",
                wordTokens = listOf("Good", "night"),
                explanation = "'Good night' dùng để chúc ngủ ngon vào cuối ngày."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm chào hỏi theo buổi với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("good morning", "chào buổi sáng"),
                    PairSlotItem("good afternoon", "chào buổi chiều"),
                    PairSlotItem("good evening", "chào buổi tối"),
                    PairSlotItem("good night", "chúc ngủ ngon")
                ),
                explanation = "Nắm vững 4 cụm chào hỏi theo thời gian trong ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: Good morning 🎙️",
                targetSentence = "Good morning",
                phoneticFocusWord = "morning",
                explanation = "Phát âm rõ 2 từ 'Good morning' (/ɡʊd ˈmɔːrnɪŋ/)."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: First Sentences (I am Alex)" else "Bài 4: Tập Lắp Câu Ngắn Giới Thiệu Tên (I am...)",
            description = if (isEng) "Step 4: Build 3-4 word self-introduction sentences." else "Bước 4: Lắp ghép từ đã học thành câu giới thiệu tên 3-4 từ.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để giới thiệu 'Tôi là Alex', bạn dùng câu ngắn nào sau đây?",
                options = listOf("I am Alex", "Goodbye Alex", "Sorry Alex", "No Alex"),
                correctIndex = 0,
                explanation = "'I' (Tôi) + 'am' (là) + [Tên] là cấu trúc câu giới thiệu bản thân ngắn gọn nhất."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành câu: 'Tôi là Alex' 🧩",
                targetSentence = "I am Alex",
                wordTokens = listOf("I", "am", "Alex"),
                distractorTokens = listOf("no"),
                explanation = "Cấu trúc câu 3 từ đầu tiên: I + am + Alex."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu giới thiệu tên và sắp xếp lại các từ 🎧",
                audioSentence = "My name is Anna",
                wordTokens = listOf("My", "name", "is", "Anna"),
                explanation = "'My name is Anna' có nghĩa là 'Tên của tôi là Anna'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ trong câu giới thiệu bản thân với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("I am", "tôi là"),
                    PairSlotItem("my name", "tên của tôi"),
                    PairSlotItem("you are", "bạn là"),
                    PairSlotItem("my friend", "bạn của tôi")
                ),
                explanation = "Các mảnh ghép cốt lõi để tạo câu giới thiệu tên."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu giới thiệu: My name is Alex 🎙️",
                targetSentence = "My name is Alex",
                phoneticFocusWord = "name",
                explanation = "Đọc liền mạch câu 4 từ: 'My name is Alex'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Greeting Dialogue (How are you?)" else "Bài 5: Hội Thoại Làm Quen & Hỏi Thăm (How are you?)",
            description = if (isEng) "Step 5: Complete short greeting conversation." else "Bước 5: Kết hợp toàn bộ từ và cụm từ thành hội thoại làm quen hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi người khác hỏi thăm 'How are you?' (Bạn có khỏe không?), bạn trả lời như thế nào?",
                options = listOf("I am fine, thank you", "My name is Alex", "Good night", "No, sorry"),
                correctIndex = 0,
                explanation = "'I am fine, thank you' (Tôi khỏe, cảm ơn bạn) là câu trả lời lịch sự chuẩn mực."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu: 'Rất vui được gặp bạn' 🧩",
                targetSentence = "Nice to meet you",
                wordTokens = listOf("Nice", "to", "meet", "you"),
                distractorTokens = listOf("sorry"),
                explanation = "'Nice to meet you' dùng để nói khi lần đầu làm quen với một người bạn mới."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu trả lời hỏi thăm sức khỏe và sắp xếp lại 🎧",
                audioSentence = "I am fine thank you",
                wordTokens = listOf("I", "am", "fine", "thank", "you"),
                explanation = "Kết hợp 'I am fine' (Tôi khỏe) và 'thank you' (cảm ơn bạn)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại tổng hợp Chặng 1 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("How are you?", "Bạn có khỏe không?"),
                    PairSlotItem("I am fine", "Tôi khỏe"),
                    PairSlotItem("Nice to meet you", "Rất vui được gặp bạn"),
                    PairSlotItem("See you later", "Hẹn gặp lại sau")
                ),
                explanation = "Hoàn tất Chặng 1: Bạn đã làm chủ trọn vẹn hội thoại chào hỏi và làm quen!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu hoàn chỉnh: Nice to meet you 🎙️",
                targetSentence = "Nice to meet you",
                phoneticFocusWord = "meet",
                explanation = "Phát âm tự tin cả câu 4 từ: 'Nice to meet you' (/naɪs tə miːt juː/)."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildBeginnerStage2Identity(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Pronouns & To Be (I, You, He, She)" else "Bài 1: Đại Từ Nhân Xưng (I, You, He, She)",
            description = if (isEng) "Step 1: Learn core pronouns and 'to be'." else "Bước 1: Nhận diện các đại từ nhân xưng cốt lõi (Tôi, Bạn, Anh ấy, Cô ấy).",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trong tiếng Anh, từ nào có nghĩa là 'Cô ấy'?",
                options = listOf("She", "He", "I", "You"),
                correctIndex = 0,
                explanation = "'She' nghĩa là 'Cô ấy/Bà ấy', còn 'He' nghĩa là 'Anh ấy/Ông ấy'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ để nói: 'Cô ấy là...' 🧩",
                targetSentence = "She is",
                wordTokens = listOf("She", "is"),
                distractorTokens = listOf("am"),
                explanation = "Chủ ngữ 'She' (Cô ấy) luôn đi với động từ to be 'is'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại 2 từ chỉ 'Bạn là...' 🎧",
                audioSentence = "You are",
                wordTokens = listOf("You", "are"),
                explanation = "Chủ ngữ 'You' (Bạn) luôn đi cùng động từ to be 'are'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các đại từ nhân xưng với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("I am", "tôi là"),
                    PairSlotItem("you are", "bạn là"),
                    PairSlotItem("he is", "anh ấy là"),
                    PairSlotItem("she is", "cô ấy là")
                ),
                explanation = "Nắm vững quy tắc ghép đại từ nhân xưng với động từ To Be (am/is/are)."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm cụm đại từ: She is 🎙️",
                targetSentence = "She is",
                phoneticFocusWord = "She",
                explanation = "Phát âm 'She' (/ʃiː/) tròn môi nhẹ ở đầu âm /ʃ/."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Jobs & Roles (Student & Teacher)" else "Bài 2: Từ Vựng Nghề Nghiệp & Vai Trò (Student, Teacher)",
            description = if (isEng) "Step 2: Core job and identity nouns." else "Bước 2: Mở rộng danh từ chỉ nghề nghiệp và mối quan hệ gần gũi.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là 'Học sinh / Sinh viên'?",
                options = listOf("student", "teacher", "doctor", "water"),
                correctIndex = 0,
                explanation = "'student' (/ˈstuːdnt/) nghĩa là học sinh hoặc sinh viên."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'một giáo viên' 🧩",
                targetSentence = "a teacher",
                wordTokens = listOf("a", "teacher"),
                distractorTokens = listOf("are"),
                explanation = "Mạo từ 'a' (một) đứng trước danh từ đếm được số ít 'teacher' (giáo viên)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại cụm từ 'một bác sĩ' 🎧",
                audioSentence = "a doctor",
                wordTokens = listOf("a", "doctor"),
                explanation = "'a doctor' có nghĩa là một bác sĩ."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ vựng nghề nghiệp với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("student", "sinh viên / học sinh"),
                    PairSlotItem("teacher", "giáo viên"),
                    PairSlotItem("doctor", "bác sĩ"),
                    PairSlotItem("friend", "người bạn")
                ),
                explanation = "4 danh từ chỉ người và nghề nghiệp phổ biến nhất."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm từ vựng: teacher 🎙️",
                targetSentence = "teacher",
                phoneticFocusWord = "teacher",
                explanation = "Phát âm 'teacher' (/ˈtiːtʃər/) với âm /tʃ/ rõ ràng ở giữa từ."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Origin & Location Chunks (From Vietnam)" else "Bài 3: Cụm Từ Quê Quán & Nơi Sống (From Vietnam)",
            description = if (isEng) "Step 3: Build 2-3 word origin and location phrases." else "Bước 3: Ghép cụm giới từ chỉ quê quán và thành phố nơi ở.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để nói 'đến từ Việt Nam', bạn dùng cụm từ nào sau đây?",
                options = listOf("from Vietnam", "thank Vietnam", "sorry Vietnam", "no Vietnam"),
                correctIndex = 0,
                explanation = "Giới từ 'from' nghĩa là 'đến từ': 'from Vietnam' (đến từ Việt Nam)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'đến từ Việt Nam' 🧩",
                targetSentence = "from Vietnam",
                wordTokens = listOf("from", "Vietnam"),
                distractorTokens = listOf("hello"),
                explanation = "'from' + [Tên quốc gia] dùng để giới thiệu quê hương, đất nước."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ chỉ 'ở Hà Nội' 🎧",
                audioSentence = "in Hanoi",
                wordTokens = listOf("in", "Hanoi"),
                explanation = "Giới từ 'in' + [Tên thành phố] nghĩa là sống tại thành phố đó."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm từ quê quán & trạng thái với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("from Vietnam", "đến từ Việt Nam"),
                    PairSlotItem("in Hanoi", "ở Hà Nội"),
                    PairSlotItem("very happy", "rất vui"),
                    PairSlotItem("my job", "công việc của tôi")
                ),
                explanation = "Các cụm từ thiết yếu để mở rộng câu giới thiệu bản thân."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: from Vietnam 🎙️",
                targetSentence = "from Vietnam",
                phoneticFocusWord = "Vietnam",
                explanation = "Đọc rõ âm /frʌm ˌvjetˈnɑːm/."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Identity Sentences (I am a student)" else "Bài 4: Ghép Câu Giới Thiệu Nghề Nghiệp & Quê Quán",
            description = if (isEng) "Step 4: Construct 4-word identity sentences." else "Bước 4: Lắp ghép đại từ và cụm danh từ thành câu 4 từ hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu nào sau đây có nghĩa là 'Tôi là một sinh viên'?",
                options = listOf("I am a student", "I student a am", "She are student", "Goodbye a student"),
                correctIndex = 0,
                explanation = "Cấu trúc chuẩn: 'I am' (Tôi là) + 'a student' (một sinh viên)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu: 'Tôi là một sinh viên' 🧩",
                targetSentence = "I am a student",
                wordTokens = listOf("I", "am", "a", "student"),
                distractorTokens = listOf("is"),
                explanation = "Ghép 'I am' + 'a student' tạo thành câu giới thiệu nghề nghiệp hoàn chỉnh."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại câu giới thiệu quê quán 🎧",
                audioSentence = "I am from Vietnam",
                wordTokens = listOf("I", "am", "from", "Vietnam"),
                explanation = "'I am from Vietnam' nghĩa là 'Tôi đến từ Việt Nam'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu giới thiệu bản thân với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("I am a student", "Tôi là một sinh viên"),
                    PairSlotItem("She is a teacher", "Cô ấy là một giáo viên"),
                    PairSlotItem("He is a doctor", "Anh ấy là một bác sĩ"),
                    PairSlotItem("I am from Vietnam", "Tôi đến từ Việt Nam")
                ),
                explanation = "4 mẫu câu giới thiệu nghề nghiệp và quê quán thông dụng nhất."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: I am from Vietnam 🎙️",
                targetSentence = "I am from Vietnam",
                phoneticFocusWord = "from",
                explanation = "Phát âm trôi chảy câu 4 từ: 'I am from Vietnam'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Personal Q&A Dialogue (Where are you from?)" else "Bài 5: Phản Xạ Hỏi Đáp Bản Thân (Where are you from?)",
            description = if (isEng) "Step 5: Master origin & job Q&A conversations." else "Bước 5: Phản xạ hỏi và trả lời về quê quán, nghề nghiệp trong thực tế.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi bạn mới hỏi 'Where are you from?' (Bạn đến từ đâu?), bạn trả lời như thế nào?",
                options = listOf("I am from Vietnam", "Good night", "Thank you please", "Yes I sorry"),
                correctIndex = 0,
                explanation = "'Where are you from?' hỏi về quê quán, nên trả lời là 'I am from Vietnam'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu hỏi: 'Bạn đến từ đâu?' 🧩",
                targetSentence = "Where are you from",
                wordTokens = listOf("Where", "are", "you", "from"),
                distractorTokens = listOf("am"),
                explanation = "Câu hỏi quê quán kinh điển: 'Where are you from?'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hỏi nghề nghiệp và sắp xếp lại các từ 🎧",
                audioSentence = "Are you a teacher",
                wordTokens = listOf("Are", "you", "a", "teacher"),
                explanation = "Đảo 'Are' lên trước 'you' để tạo câu hỏi: 'Are you a teacher?' (Bạn có phải là giáo viên không?)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hỏi đáp Chặng 2 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Where are you from?", "Bạn đến từ đâu?"),
                    PairSlotItem("Are you a student?", "Bạn là sinh viên phải không?"),
                    PairSlotItem("Yes, I am", "Vâng, đúng vậy"),
                    PairSlotItem("He is my friend", "Anh ấy là bạn của tôi")
                ),
                explanation = "Chúc mừng bạn đã hoàn thành Chặng 2 và tự tin giới thiệu bản thân, nghề nghiệp, quê quán!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu hỏi: Where are you from? 🎙️",
                targetSentence = "Where are you from",
                phoneticFocusWord = "Where",
                explanation = "Lên giọng nhẹ ở cuối câu hoặc nhấn vào từ để hỏi 'Where'."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildBeginnerStage3FoodEssentials(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Food & Drink Words (Water & Coffee)" else "Bài 1: Từ Vựng Đồ Ăn & Thức Uống (Water, Coffee, Tea)",
            description = if (isEng) "Step 1: Essential food and drink nouns." else "Bước 1: Làm quen các danh từ chỉ đồ ăn và thức uống hàng ngày.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là 'Nước uống'?",
                options = listOf("water", "teacher", "goodbye", "morning"),
                correctIndex = 0,
                explanation = "'water' (/ˈwɔːtər/) là nước uống — từ vựng thiết yếu nhất trong đời sống."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ để gọi: 'Cà phê, làm ơn' 🧩",
                targetSentence = "Coffee please",
                wordTokens = listOf("Coffee", "please"),
                distractorTokens = listOf("sorry"),
                explanation = "Ghép [Tên đồ uống] + 'please' là cách gọi món ngắn gọn và lịch sự nhất."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại 2 từ gọi nước lịch sự 🎧",
                audioSentence = "Water please",
                wordTokens = listOf("Water", "please"),
                explanation = "'Water please' nghĩa là 'Làm ơn cho tôi nước uống'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ vựng đồ ăn thức uống với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("water", "nước lọc"),
                    PairSlotItem("coffee", "cà phê"),
                    PairSlotItem("tea", "trà"),
                    PairSlotItem("bread", "bánh mì")
                ),
                explanation = "4 danh từ ẩm thực cơ bản xuất hiện mỗi ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm từ vựng: water 🎙️",
                targetSentence = "water",
                phoneticFocusWord = "water",
                explanation = "Phát âm 'water' (/ˈwɔːtər/) rõ âm /w/ ở đầu từ."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Need & Taste Words (Want, Like, Eat, Drink)" else "Bài 2: Động Từ Nhu Cầu (Want, Like, Eat, Drink)",
            description = if (isEng) "Step 2: Core verbs for eating, drinking, and liking." else "Bước 2: Học các động từ chỉ nhu cầu ăn, uống, thích và muốn.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Động từ tiếng Anh nào có nghĩa là 'Uống'?",
                options = listOf("drink", "eat", "student", "hello"),
                correctIndex = 0,
                explanation = "'drink' nghĩa là uống, còn 'eat' nghĩa là ăn."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'uống nước' 🧩",
                targetSentence = "drink water",
                wordTokens = listOf("drink", "water"),
                distractorTokens = listOf("am"),
                explanation = "Động từ 'drink' (uống) + danh từ 'water' (nước) = 'drink water'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại cụm từ 'ăn bánh mì' 🎧",
                audioSentence = "eat bread",
                wordTokens = listOf("eat", "bread"),
                explanation = "'eat' (ăn) + 'bread' (bánh mì) = 'eat bread'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các động từ nhu cầu hàng ngày với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("want", "muốn"),
                    PairSlotItem("like", "thích"),
                    PairSlotItem("eat", "ăn"),
                    PairSlotItem("drink", "uống")
                ),
                explanation = "4 động từ cốt lõi giúp bạn diễn đạt mọi nhu cầu ăn uống hàng ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm động từ: drink water 🎙️",
                targetSentence = "drink water",
                phoneticFocusWord = "drink",
                explanation = "Phát âm rõ tổ hợp phụ âm /dr/ trong từ 'drink'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Food & Drink Phrases (Hot Coffee & Cold Water)" else "Bài 3: Cụm Danh Từ Mô Tả (Hot Coffee, Cold Water)",
            description = if (isEng) "Step 3: Combine adjectives and nouns into 2-3 word chunks." else "Bước 3: Ghép tính từ nóng/lạnh với đồ ăn thức uống.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm từ nào có nghĩa là 'Cà phê nóng'?",
                options = listOf("hot coffee", "cold water", "good night", "my teacher"),
                correctIndex = 0,
                explanation = "Tính từ 'hot' (nóng) đứng trước danh từ 'coffee' tạo thành 'hot coffee'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'nước lạnh' 🧩",
                targetSentence = "cold water",
                wordTokens = listOf("cold", "water"),
                distractorTokens = listOf("eat"),
                explanation = "'cold' (lạnh) + 'water' (nước) = 'cold water' (nước lạnh)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'trà nóng' 🎧",
                audioSentence = "hot tea",
                wordTokens = listOf("hot", "tea"),
                explanation = "'hot tea' có nghĩa là trà nóng."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm đồ ăn thức uống với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("hot coffee", "cà phê nóng"),
                    PairSlotItem("cold water", "nước lạnh"),
                    PairSlotItem("fresh milk", "sữa tươi"),
                    PairSlotItem("some rice", "một ít cơm")
                ),
                explanation = "Trong tiếng Anh, tính từ mô tả (hot, cold, fresh) luôn đứng trước danh từ."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: hot coffee 🎙️",
                targetSentence = "hot coffee",
                phoneticFocusWord = "coffee",
                explanation = "Đọc rõ 2 từ 'hot coffee' (/hɑːt ˈkɔːfi/)."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Polite Request Sentences (I want water please)" else "Bài 4: Tập Lắp Câu Gọi Món & Nhu Cầu (I like / I want)",
            description = if (isEng) "Step 4: Build 4-word sentences expressing needs and preferences." else "Bước 4: Lắp ghép chủ ngữ, động từ và danh từ thành câu 4 từ hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu nào có nghĩa là 'Tôi thích cà phê nóng'?",
                options = listOf("I like hot coffee", "I am hot coffee", "Goodbye hot coffee", "Where hot coffee"),
                correctIndex = 0,
                explanation = "'I' (Tôi) + 'like' (thích) + 'hot coffee' (cà phê nóng)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu: 'Tôi muốn nước, làm ơn' 🧩",
                targetSentence = "I want water please",
                wordTokens = listOf("I", "want", "water", "please"),
                distractorTokens = listOf("are"),
                explanation = "Cấu trúc câu yêu cầu lịch sự: I + want + [Đồ vật] + please."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại câu nói về sở thích đồ uống 🎧",
                audioSentence = "I like hot tea",
                wordTokens = listOf("I", "like", "hot", "tea"),
                explanation = "'I like hot tea' nghĩa là 'Tôi thích trà nóng'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu nhu cầu hàng ngày với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("I want water", "Tôi muốn nước"),
                    PairSlotItem("I like coffee", "Tôi thích cà phê"),
                    PairSlotItem("We eat bread", "Chúng tôi ăn bánh mì"),
                    PairSlotItem("She drinks milk", "Cô ấy uống sữa")
                ),
                explanation = "Các mẫu câu 3-4 từ diễn đạt nhu cầu ăn uống tự nhiên."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: I want water please 🎙️",
                targetSentence = "I want water please",
                phoneticFocusWord = "want",
                explanation = "Đọc rõ ràng cả câu 4 từ: 'I want water please'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Café & Dining Dialogue (Can I have...?)" else "Bài 5: Hội Thoại Gọi Món Thực Tế (Can I have coffee?)",
            description = if (isEng) "Step 5: Ordering food and drinks in a real conversation." else "Bước 5: Tự tin gọi đồ ăn thức uống và phản xạ tại quán cà phê, nhà hàng.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi vào quán cà phê, câu hỏi lịch sự nào dùng để gọi 'Cho tôi một ly cà phê được không?'",
                options = listOf("Can I have coffee, please?", "Where are you from?", "My name is coffee", "I am a teacher"),
                correctIndex = 0,
                explanation = "'Can I have [món đồ], please?' là mẫu câu gọi món lịch sự nhất trong giao tiếp thực tế."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 5 từ thành câu gọi món: 'Cho tôi trà nhé, làm ơn' 🧩",
                targetSentence = "Can I have tea please",
                wordTokens = listOf("Can", "I", "have", "tea", "please"),
                distractorTokens = listOf("from"),
                explanation = "'Can I have + [tên món] + please' giúp bạn gọi món tự nhiên ở bất kỳ đâu."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hỏi sở thích đồ uống và sắp xếp lại 🎧",
                audioSentence = "Do you like coffee",
                wordTokens = listOf("Do", "you", "like", "coffee"),
                explanation = "'Do you like coffee?' nghĩa là 'Bạn có thích cà phê không?'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu hội thoại gọi món Chặng 3 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Can I have coffee?", "Cho tôi cà phê được không?"),
                    PairSlotItem("Do you like tea?", "Bạn có thích trà không?"),
                    PairSlotItem("Here you are", "Của bạn đây"),
                    PairSlotItem("Yes, thank you", "Vâng, cảm ơn bạn")
                ),
                explanation = "Hoàn tất Chặng 3: Bạn đã làm chủ từ vựng và mẫu câu gọi đồ ăn, thức uống hàng ngày!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu gọi món: Can I have coffee please 🎙️",
                targetSentence = "Can I have coffee please",
                phoneticFocusWord = "coffee",
                explanation = "Lên giọng nhẹ ở từ 'please' cuối câu để thể hiện sự lịch thiệp."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildBeginnerStage4NumbersTime(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Core Numbers (One, Two, Three, Five, Ten)" else "Bài 1: Số Đếm Căn Bản (One, Two, Three, Five, Ten)",
            description = if (isEng) "Step 1: Recognize foundational numbers 1 to 10." else "Bước 1: Nhận diện các số đếm cốt lõi từ 1 đến 10.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là số 'Một' (1)?",
                options = listOf("one", "ten", "water", "hello"),
                correctIndex = 0,
                explanation = "'one' (/wʌn/) là số 1 trong tiếng Anh."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 số đếm theo thứ tự: 'Một, hai, ba' 🧩",
                targetSentence = "one two three",
                wordTokens = listOf("one", "two", "three"),
                distractorTokens = listOf("sorry"),
                explanation = "Thứ tự đếm cơ bản: one (1), two (2), three (3)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại 3 số đếm theo đúng thứ tự 🎧",
                audioSentence = "one two three",
                wordTokens = listOf("one", "two", "three"),
                explanation = "Luyện nghe phân biệt rõ 3 số đầu tiên: one, two, three."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các số đếm tiếng Anh với chữ số tương ứng 🧩",
                pairs = listOf(
                    PairSlotItem("one", "số 1 (một)"),
                    PairSlotItem("two", "số 2 (hai)"),
                    PairSlotItem("five", "số 5 (năm)"),
                    PairSlotItem("ten", "số 10 (mười)")
                ),
                explanation = "Nắm vững các mốc số đếm cơ bản nhất."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện đếm 3 số: one two three 🎙️",
                targetSentence = "one two three",
                phoneticFocusWord = "three",
                explanation = "Chú ý âm /θr/ ở đầu từ 'three' (/θriː/)."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Time & Shopping Words (Time, Now, Dollar)" else "Bài 2: Từ Vựng Thời Gian & Giá Cả (Time, Today, Dollar)",
            description = if (isEng) "Step 2: Core time and money words." else "Bước 2: Mở rộng từ vựng chỉ thời gian, ngày hôm nay và đơn vị tiền tệ.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là 'Thời gian / Giờ giấc'?",
                options = listOf("time", "bread", "student", "friend"),
                correctIndex = 0,
                explanation = "'time' (/taɪm/) có nghĩa là thời gian hoặc giờ."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'hai đô la' 🧩",
                targetSentence = "two dollars",
                wordTokens = listOf("two", "dollars"),
                distractorTokens = listOf("am"),
                explanation = "Ghép số đếm 'two' (2) + đơn vị tiền 'dollars' (đô la)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'năm đô la' 🎧",
                audioSentence = "five dollars",
                wordTokens = listOf("five", "dollars"),
                explanation = "'five dollars' có nghĩa là 5 đô la."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ vựng thời gian & mua sắm với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("time", "thời gian / giờ"),
                    PairSlotItem("now", "bây giờ"),
                    PairSlotItem("today", "hôm nay"),
                    PairSlotItem("dollar", "đô la")
                ),
                explanation = "4 từ vựng xuất hiện thường xuyên khi hỏi giờ và mua sắm."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: five dollars 🎙️",
                targetSentence = "five dollars",
                phoneticFocusWord = "five",
                explanation = "Phát âm rõ âm cuối /v/ trong 'five' và âm /z/ cuối từ 'dollars'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Time & Price Chunks (What Time & How Much)" else "Bài 3: Cụm Từ Hỏi Giờ & Hỏi Giá (What Time, How Much)",
            description = if (isEng) "Step 3: Build 2-word question and clock phrases." else "Bước 3: Ghép cụm từ để hỏi mấy giờ và giá bao nhiêu tiền.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm từ nào dùng để bắt đầu câu hỏi 'Mấy giờ'?",
                options = listOf("What time", "Good morning", "My name", "Thank you"),
                correctIndex = 0,
                explanation = "'What time' có nghĩa là 'Mấy giờ'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ để hỏi: 'Bao nhiêu tiền?' 🧩",
                targetSentence = "How much",
                wordTokens = listOf("How", "much"),
                distractorTokens = listOf("tea"),
                explanation = "'How much' là cụm từ chuẩn dùng để hỏi giá tiền khi mua sắm."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ chỉ 'tám giờ đúng' 🎧",
                audioSentence = "eight o'clock",
                wordTokens = listOf("eight", "o'clock"),
                explanation = "Số đếm + 'o'clock' dùng để chỉ giờ đúng (8 giờ đúng)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm hỏi giờ & giá tiền với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("what time", "mấy giờ"),
                    PairSlotItem("how much", "bao nhiêu tiền"),
                    PairSlotItem("eight o'clock", "tám giờ đúng"),
                    PairSlotItem("ten dollars", "mười đô la")
                ),
                explanation = "Các cụm từ nền tảng để lắp thành câu hỏi giờ và hỏi giá."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm hỏi giá: How much 🎙️",
                targetSentence = "How much",
                phoneticFocusWord = "much",
                explanation = "Phát âm rõ âm cuối /tʃ/ trong từ 'much' (/haʊ mʌtʃ/)."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Time & Price Sentences (It is five dollars)" else "Bài 4: Tập Lắp Câu Báo Giờ & Báo Giá (It is...)",
            description = if (isEng) "Step 4: Form 4-word time and price statements." else "Bước 4: Lắp ghép thành câu thông báo giờ giấc và giá tiền hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để nói 'Nó có giá 5 đô la', bạn dùng câu nào sau đây?",
                options = listOf("It is five dollars", "I am five dollars", "Hello five dollars", "Where five dollars"),
                correctIndex = 0,
                explanation = "'It is' (Nó là / Nó có giá) + 'five dollars' (5 đô la)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu: 'Nó giá mười đô la' 🧩",
                targetSentence = "It is ten dollars",
                wordTokens = listOf("It", "is", "ten", "dollars"),
                distractorTokens = listOf("are"),
                explanation = "Cấu trúc báo giá đơn giản: It + is + [Số tiền] + dollars."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu thông báo giờ và sắp xếp lại 🎧",
                audioSentence = "It is eight o'clock",
                wordTokens = listOf("It", "is", "eight", "o'clock"),
                explanation = "'It is eight o'clock' nghĩa là 'Bây giờ là 8 giờ đúng'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu thông báo giờ & giá với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("It is eight o'clock", "Bây giờ là 8 giờ"),
                    PairSlotItem("It is five dollars", "Nó giá 5 đô la"),
                    PairSlotItem("Two coffees, please", "Làm ơn cho 2 ly cà phê"),
                    PairSlotItem("Three books", "Ba quyển sách")
                ),
                explanation = "Ứng dụng số đếm trực tiếp vào câu giao tiếp đời sống."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: It is five dollars 🎙️",
                targetSentence = "It is five dollars",
                phoneticFocusWord = "dollars",
                explanation = "Đọc trôi chảy câu 4 từ: 'It is five dollars'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Shopping & Schedule Dialogue (What time is it?)" else "Bài 5: Hội Thoại Hỏi Giờ & Mua Sắm (What time is it?)",
            description = if (isEng) "Step 5: Real-world time and shopping Q&A." else "Bước 5: Phản xạ hỏi giờ và hỏi giá tiền khi đi mua sắm thực tế.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để hỏi người khác 'Bây giờ là mấy giờ?', bạn nói câu nào?",
                options = listOf("What time is it now?", "Where are you from?", "How are you today?", "My name is Alex"),
                correctIndex = 0,
                explanation = "'What time is it now?' là câu hỏi giờ thông dụng nhất trong tiếng Anh."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu hỏi: 'Mấy giờ rồi?' 🧩",
                targetSentence = "What time is it",
                wordTokens = listOf("What", "time", "is", "it"),
                distractorTokens = listOf("am"),
                explanation = "Cấu trúc hỏi giờ chuẩn: What time + is + it?"
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hỏi giá tiền và sắp xếp lại các từ 🎧",
                audioSentence = "How much is this",
                wordTokens = listOf("How", "much", "is", "this"),
                explanation = "'How much is this?' nghĩa là 'Cái này giá bao nhiêu tiền?'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại Chặng 4 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("What time is it?", "Bây giờ là mấy giờ?"),
                    PairSlotItem("How much is this?", "Cái này bao nhiêu tiền?"),
                    PairSlotItem("It is three dollars", "Nó giá 3 đô la"),
                    PairSlotItem("It is ten o'clock", "Bây giờ là 10 giờ đúng")
                ),
                explanation = "Hoàn tất Chặng 4: Bạn đã nắm vững số đếm, cách hỏi giờ và hỏi giá khi mua sắm!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu hỏi giá: How much is this 🎙️",
                targetSentence = "How much is this",
                phoneticFocusWord = "this",
                explanation = "Phát âm rõ câu hỏi 4 từ: 'How much is this?'."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildBeginnerStage5FamilyHobbies(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Family Words (Mother, Father, Family)" else "Bài 1: Từ Vựng Gia Đình (Mother, Father, Brother, Sister)",
            description = if (isEng) "Step 1: Core family member nouns." else "Bước 1: Nhận diện các từ vựng chỉ người thân trong gia đình.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là 'Mẹ'?",
                options = listOf("mother", "father", "water", "clock"),
                correctIndex = 0,
                explanation = "'mother' (/ˈmʌðər/) nghĩa là mẹ, còn 'father' nghĩa là bố/cha."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'gia đình tôi' 🧩",
                targetSentence = "my family",
                wordTokens = listOf("my", "family"),
                distractorTokens = listOf("no"),
                explanation = "'my' (của tôi) + 'family' (gia đình) = 'my family' (gia đình của tôi)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp lại cụm từ chỉ 'mẹ của tôi' 🎧",
                audioSentence = "my mother",
                wordTokens = listOf("my", "mother"),
                explanation = "'my mother' có nghĩa là mẹ của tôi."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ vựng gia đình với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("mother", "mẹ"),
                    PairSlotItem("father", "bố / cha"),
                    PairSlotItem("brother", "anh / em trai"),
                    PairSlotItem("sister", "chị / em gái")
                ),
                explanation = "4 danh từ chỉ các thành viên cốt lõi trong gia đình."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: my family 🎙️",
                targetSentence = "my family",
                phoneticFocusWord = "family",
                explanation = "Phát âm 'family' (/ˈfæməli/) rõ ràng và ấm áp."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Feelings & Hobbies (Love, Happy, Music, Read)" else "Bài 2: Từ Vựng Cảm Xúc & Sở Thích (Love, Happy, Music)",
            description = if (isEng) "Step 2: Positive adjectives and hobby words." else "Bước 2: Mở rộng từ vựng chỉ tình cảm và sở thích hàng ngày.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là 'Hạnh phúc / Vui vẻ'?",
                options = listOf("happy", "ten", "bread", "sorry"),
                correctIndex = 0,
                explanation = "'happy' (/ˈhæpi/) nghĩa là vui vẻ, hạnh phúc."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'đọc sách' 🧩",
                targetSentence = "read books",
                wordTokens = listOf("read", "books"),
                distractorTokens = listOf("am"),
                explanation = "Động từ 'read' (đọc) + danh từ 'books' (những cuốn sách)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'yêu âm nhạc' 🎧",
                audioSentence = "love music",
                wordTokens = listOf("love", "music"),
                explanation = "'love' (yêu thích) + 'music' (âm nhạc) = 'love music'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ vựng cảm xúc & sở thích với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("love", "yêu thương"),
                    PairSlotItem("happy", "hạnh phúc / vui vẻ"),
                    PairSlotItem("music", "âm nhạc"),
                    PairSlotItem("read", "đọc")
                ),
                explanation = "Từ vựng giúp bạn chia sẻ về gia đình và sở thích cá nhân."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: love music 🎙️",
                targetSentence = "love music",
                phoneticFocusWord = "music",
                explanation = "Phát âm 'music' (/ˈmjuːzɪk/) rõ âm /mjuː/."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Descriptive Family Phrases (A Happy Family)" else "Bài 3: Cụm Từ Mô Tả Gia Đình & Sở Thích",
            description = if (isEng) "Step 3: Build 2-3 word possessive and descriptive phrases." else "Bước 3: Ghép cụm từ sở hữu và tính từ mô tả người thân.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm từ nào có nghĩa là 'Một gia đình hạnh phúc'?",
                options = listOf("a happy family", "five dollars", "good night", "cold water"),
                correctIndex = 0,
                explanation = "'a' (một) + 'happy' (hạnh phúc) + 'family' (gia đình)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành cụm: 'một gia đình hạnh phúc' 🧩",
                targetSentence = "a happy family",
                wordTokens = listOf("a", "happy", "family"),
                distractorTokens = listOf("no"),
                explanation = "Tính từ 'happy' đứng trước danh từ 'family'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'anh trai của tôi' 🎧",
                audioSentence = "my brother",
                wordTokens = listOf("my", "brother"),
                explanation = "'my brother' nghĩa là anh trai hoặc em trai của tôi."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm từ gia đình & đời sống với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("my father", "bố của tôi"),
                    PairSlotItem("your sister", "chị/em gái của bạn"),
                    PairSlotItem("at home", "ở nhà"),
                    PairSlotItem("every day", "mỗi ngày")
                ),
                explanation = "Các cụm từ thiết yếu để kể về gia đình và sinh hoạt ở nhà."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: a happy family 🎙️",
                targetSentence = "a happy family",
                phoneticFocusWord = "happy",
                explanation = "Đọc liền mạch 3 từ: 'a happy family'."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Family & Hobby Sentences (I love my family)" else "Bài 4: Tập Lắp Câu Kể Về Gia Đình & Sở Thích",
            description = if (isEng) "Step 4: Build 4-word sentences about family and hobbies." else "Bước 4: Lắp ghép thành câu hoàn chỉnh 4 từ nói về gia đình và sở thích.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu nào có nghĩa là 'Tôi yêu gia đình của tôi'?",
                options = listOf("I love my family", "I am five dollars", "Where is my coffee", "Goodbye my name"),
                correctIndex = 0,
                explanation = "'I' (Tôi) + 'love' (yêu) + 'my family' (gia đình của tôi)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu: 'Tôi yêu gia đình tôi' 🧩",
                targetSentence = "I love my family",
                wordTokens = listOf("I", "love", "my", "family"),
                distractorTokens = listOf("are"),
                explanation = "Mẫu câu 4 từ giàu cảm xúc và dễ nhớ: 'I love my family'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu giới thiệu người thân và sắp xếp lại 🎧",
                audioSentence = "This is my mother",
                wordTokens = listOf("This", "is", "my", "mother"),
                explanation = "'This is my mother' dùng để giới thiệu 'Đây là mẹ của tôi'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu gia đình & sở thích với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("This is my father", "Đây là bố của tôi"),
                    PairSlotItem("I love my family", "Tôi yêu gia đình tôi"),
                    PairSlotItem("We are very happy", "Chúng tôi rất hạnh phúc"),
                    PairSlotItem("I read books", "Tôi đọc sách")
                ),
                explanation = "Bạn đã có thể tự tin giới thiệu các thành viên trong gia đình bằng câu hoàn chỉnh."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: I love my family 🎙️",
                targetSentence = "I love my family",
                phoneticFocusWord = "love",
                explanation = "Phát âm truyền cảm cả câu: 'I love my family'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Daily Life Dialogue Synthesis" else "Bài 5: Phản Xạ Đàm Thoại Đời Sống Tổng Hợp",
            description = if (isEng) "Step 5: Complete daily life Q&A conversations." else "Bước 5: Phản xạ hỏi đáp về gia đình, sở thích và tổng hợp giao tiếp hàng ngày.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi bạn bè hỏi 'Do you like music?' (Bạn có thích âm nhạc không?), bạn trả lời đồng ý như thế nào?",
                options = listOf("Yes, I love music!", "It is five dollars", "My name is water", "Good night teacher"),
                correctIndex = 0,
                explanation = "'Yes, I love music!' (Vâng, tôi rất yêu âm nhạc!) là câu trả lời tự nhiên và hào hứng."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu hỏi: 'Bạn có thích âm nhạc không?' 🧩",
                targetSentence = "Do you like music",
                wordTokens = listOf("Do", "you", "like", "music"),
                distractorTokens = listOf("is"),
                explanation = "Mẫu câu hỏi sở thích: 'Do you like + [Sở thích]?'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu trả lời và sắp xếp lại các từ 🎧",
                audioSentence = "Yes I love music",
                wordTokens = listOf("Yes", "I", "love", "music"),
                explanation = "'Yes I love music' nghĩa là 'Vâng, tôi yêu âm nhạc'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại tổng hợp Chặng 5 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Do you like music?", "Bạn có thích âm nhạc không?"),
                    PairSlotItem("This is my family", "Đây là gia đình của tôi"),
                    PairSlotItem("My mother is kind", "Mẹ tôi rất tốt bụng"),
                    PairSlotItem("We are happy at home", "Chúng tôi hạnh phúc ở nhà")
                ),
                explanation = "Tuyệt vời! Bạn đã hoàn thành trọn vẹn 5 chặng nền tảng A0-A1 với hơn 100 từ và mẫu câu thực tế!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu hỏi sở thích: Do you like music 🎙️",
                targetSentence = "Do you like music",
                phoneticFocusWord = "like",
                explanation = "Lên giọng nhẹ ở cuối câu hỏi Yes/No: 'Do you like music?'."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildBeginnerStage6PlacesRoutines(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Places & Transport (Hotel, Airport, Bus)" else "Bài 1: Từ Vựng Địa Điểm & Phương Tiện (Hotel, Station, Bus)",
            description = if (isEng) "Step 1: Essential travel and city nouns." else "Bước 1: Nhận diện từ vựng chỉ địa điểm và phương tiện giao thông.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ tiếng Anh nào có nghĩa là 'Khách sạn'?",
                options = listOf("hotel", "bread", "sister", "morning"),
                correctIndex = 0,
                explanation = "'hotel' (/hoʊˈtel/) có nghĩa là khách sạn."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'bằng xe buýt' 🧩",
                targetSentence = "by bus",
                wordTokens = listOf("by", "bus"),
                distractorTokens = listOf("am"),
                explanation = "Giới từ 'by' + phương tiện ('by bus', 'by taxi') chỉ cách thức di chuyển."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'bằng xe taxi' 🎧",
                audioSentence = "by taxi",
                wordTokens = listOf("by", "taxi"),
                explanation = "'by taxi' nghĩa là đi bằng xe taxi."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ vựng địa điểm & phương tiện với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("hotel", "khách sạn"),
                    PairSlotItem("airport", "sân bay"),
                    PairSlotItem("station", "nhà ga / trạm xe"),
                    PairSlotItem("bus", "xe buýt")
                ),
                explanation = "4 từ vựng không thể thiếu khi đi lại và du lịch."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm từ vựng: airport 🎙️",
                targetSentence = "airport",
                phoneticFocusWord = "airport",
                explanation = "Phát âm 'airport' (/ˈerpɔːrt/) với trọng âm rơi vào âm tiết đầu tiên."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Routine & Direction Verbs (Go, Work, Turn)" else "Bài 2: Động Từ Di Chuyển & Chỉ Hướng (Go, Work, Turn)",
            description = if (isEng) "Step 2: Verbs for daily routines and directions." else "Bước 2: Mở rộng động từ chỉ hoạt động hàng ngày và phương hướng.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm từ nào có nghĩa là 'Rẽ trái'?",
                options = listOf("turn left", "eat bread", "five dollars", "my mother"),
                correctIndex = 0,
                explanation = "'turn left' nghĩa là rẽ trái, còn 'turn right' nghĩa là rẽ phải."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 2 từ thành cụm: 'rẽ phải' 🧩",
                targetSentence = "turn right",
                wordTokens = listOf("turn", "right"),
                distractorTokens = listOf("tea"),
                explanation = "'turn' (rẽ) + 'right' (bên phải) = 'turn right'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'đi thẳng' 🎧",
                audioSentence = "go straight",
                wordTokens = listOf("go", "straight"),
                explanation = "'go straight' có nghĩa là đi thẳng."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm chỉ hướng & hoạt động với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("turn left", "rẽ trái"),
                    PairSlotItem("turn right", "rẽ phải"),
                    PairSlotItem("go straight", "đi thẳng"),
                    PairSlotItem("near here", "gần đây")
                ),
                explanation = "4 cụm từ chỉ đường cốt lõi trong giao tiếp thực tế."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm chỉ đường: turn left 🎙️",
                targetSentence = "turn left",
                phoneticFocusWord = "left",
                explanation = "Phát âm rõ âm cuối /ft/ trong từ 'left'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Routine & Location Chunks (Go to work)" else "Bài 3: Cụm Từ Sinh Hoạt & Vị Trí (Go to work, At the station)",
            description = if (isEng) "Step 3: Combine verbs and places into 3-word chunks." else "Bước 3: Ghép động từ với địa điểm thành cụm sinh hoạt hàng ngày.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm từ nào có nghĩa là 'Đi làm'?",
                options = listOf("go to work", "drink cold water", "good morning", "ten dollars"),
                correctIndex = 0,
                explanation = "'go to work' nghĩa là đi làm."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành cụm: 'đi làm' 🧩",
                targetSentence = "go to work",
                wordTokens = listOf("go", "to", "work"),
                distractorTokens = listOf("is"),
                explanation = "Cấu trúc 'go to + [địa điểm/hoạt động]': go to work, go to school."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'ở sân bay' 🎧",
                audioSentence = "at the airport",
                wordTokens = listOf("at", "the", "airport"),
                explanation = "'at the airport' nghĩa là ở tại sân bay."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm hoạt động hàng ngày với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("go to work", "đi làm"),
                    PairSlotItem("go to school", "đi học"),
                    PairSlotItem("at the hotel", "tại khách sạn"),
                    PairSlotItem("in the morning", "vào buổi sáng")
                ),
                explanation = "Các cụm từ giúp bạn mô tả lịch trình di chuyển mỗi ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: go to work 🎙️",
                targetSentence = "go to work",
                phoneticFocusWord = "work",
                explanation = "Đọc liền mạch 3 từ: 'go to work'."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Routine & Navigation Sentences" else "Bài 4: Tập Lắp Câu Hỏi Đường & Lịch Trình Hàng Ngày",
            description = if (isEng) "Step 4: Build 4-5 word navigation and routine sentences." else "Bước 4: Lắp ghép thành câu hỏi vị trí và câu kể thói quen hàng ngày.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để hỏi 'Khách sạn ở đâu?', bạn dùng câu nào sau đây?",
                options = listOf("Where is the hotel?", "I am a hotel", "How much is coffee?", "My name is hotel"),
                correctIndex = 0,
                explanation = "'Where is the + [Địa điểm]?' là cấu trúc chuẩn để hỏi vị trí của một nơi."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành câu hỏi: 'Nhà ga ở đâu?' 🧩",
                targetSentence = "Where is the station",
                wordTokens = listOf("Where", "is", "the", "station"),
                distractorTokens = listOf("are"),
                explanation = "'Where is the station?' nghĩa là 'Nhà ga ở đâu?'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu kể thói quen đi làm và sắp xếp lại 🎧",
                audioSentence = "I go to work",
                wordTokens = listOf("I", "go", "to", "work"),
                explanation = "'I go to work' nghĩa là 'Tôi đi làm'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu hỏi đường & sinh hoạt với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Where is the hotel?", "Khách sạn ở đâu?"),
                    PairSlotItem("Where is the airport?", "Sân bay ở đâu?"),
                    PairSlotItem("I go by bus", "Tôi đi bằng xe buýt"),
                    PairSlotItem("Turn left here", "Rẽ trái ở đây")
                ),
                explanation = "Làm chủ cấu trúc hỏi đường và nói về phương tiện đi lại."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu hỏi: Where is the hotel 🎙️",
                targetSentence = "Where is the hotel",
                phoneticFocusWord = "hotel",
                explanation = "Phát âm rõ câu hỏi 4 từ: 'Where is the hotel?'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Travel & Directions Mastery Bridge (A2)" else "Bài 5: Hội Thoại Hỏi Đường & Cầu Nối Lên Cấp Độ A2",
            description = if (isEng) "Step 5: Complete street navigation dialogue." else "Bước 5: Tự tin bắt chuyện lịch sự với 'Excuse me' để hỏi đường và di chuyển.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trước khi hỏi đường một người lạ trên phố, bạn nên mở lời lịch sự bằng cụm từ nào?",
                options = listOf("Excuse me, where is the station?", "Goodbye station", "I am ten dollars", "No thank you"),
                correctIndex = 0,
                explanation = "'Excuse me' (Xin lỗi cho tôi hỏi) là cụm mở lời lịch sự khi muốn hỏi đường."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 5 từ thành câu chỉ đường: 'Đi thẳng và rẽ trái' 🧩",
                targetSentence = "Go straight and turn left",
                wordTokens = listOf("Go", "straight", "and", "turn", "left"),
                distractorTokens = listOf("am"),
                explanation = "Kết hợp 2 hành động chỉ đường bằng từ nối 'and' (và)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hỏi đường lịch sự và sắp xếp lại 🎧",
                audioSentence = "Excuse me where is it",
                wordTokens = listOf("Excuse", "me", "where", "is", "it"),
                explanation = "'Excuse me, where is it?' nghĩa là 'Xin lỗi cho hỏi nó ở đâu ạ?'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại du lịch & chỉ đường với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Excuse me", "Xin lỗi (cho tôi hỏi)"),
                    PairSlotItem("Where is the station?", "Nhà ga ở đâu?"),
                    PairSlotItem("Go straight ahead", "Đi thẳng về phía trước"),
                    PairSlotItem("It is near here", "Nó ở gần đây")
                ),
                explanation = "Xuất sắc! Bạn đã hoàn thành cầu nối từ A1 lên A2 và sẵn sàng cho giao tiếp trung cấp!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu chỉ đường: Go straight and turn left 🎙️",
                targetSentence = "Go straight and turn left",
                phoneticFocusWord = "straight",
                explanation = "Đọc trôi chảy câu ghép 5 từ: 'Go straight and turn left'."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }
    private fun buildIntermediateStage1ConnectedSpeech(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Core Collocations" else "Bài 1: Cụm Từ Cố Định Cốt Lõi (Collocations)",
            description = if (isEng) "Step 1: Natural verb-noun collocations." else "Bước 1: Nhận diện các cụm động từ - danh từ tự nhiên của người bản xứ.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trong tiếng Anh tự nhiên, để nói 'đưa ra quyết định', người bản xứ dùng động từ nào đi với 'a decision'?",
                options = listOf("make a decision", "do a decision", "create a decision", "have a decision"),
                correctIndex = 0,
                explanation = "Collocation chuẩn: 'make a decision' (đưa ra quyết định), không dùng 'do a decision'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành cụm: 'nghỉ giải lao' 🧩",
                targetSentence = "take a break",
                wordTokens = listOf("take", "a", "break"),
                distractorTokens = listOf("make"),
                explanation = "'take a break' có nghĩa là nghỉ ngơi / giải lao."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'đưa ra quyết định' 🎧",
                audioSentence = "make a decision",
                wordTokens = listOf("make", "a", "decision"),
                explanation = "'make a decision' nghĩa là đưa ra quyết định."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các Collocations thông dụng với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("make a decision", "đưa ra quyết định"),
                    PairSlotItem("take a break", "nghỉ giải lao"),
                    PairSlotItem("pay attention", "chú ý / tập trung"),
                    PairSlotItem("save time", "tiết kiệm thời gian")
                ),
                explanation = "4 Collocations tần suất cao nhất trong giao tiếp B1."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói nối âm cụm từ: take a break 🎙️",
                targetSentence = "take a break",
                phoneticFocusWord = "break",
                explanation = "Nối âm cuối /k/ của 'take' sang 'a': /teɪkə breɪk/."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Connected Speech & Elision" else "Bài 2: Giải Mã Nuốt Âm & Nối Âm (Could've, Gonna)",
            description = if (isEng) "Step 2: Decode native speech reductions." else "Bước 2: Nhận diện hiện tượng nuốt âm và nối âm trong giao tiếp nhanh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi người bản xứ nói nhanh 'I could've helped you', cụm 'could've' là viết tắt của từ nào?",
                options = listOf("could have", "could of", "could had", "can have"),
                correctIndex = 0,
                explanation = "'could've' (/ˈkʊdəv/) là dạng rút gọn âm thanh của 'could have'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu hoàn chỉnh: 'Tôi lẽ ra đã có thể giúp bạn' 🧩",
                targetSentence = "I could have helped you",
                wordTokens = listOf("I", "could", "have", "helped", "you"),
                distractorTokens = listOf("did"),
                explanation = "Cấu trúc 'could have + V3/ed' diễn tả một khả năng trong quá khứ."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu có hiện tượng nối âm và sắp xếp lại 🎧",
                audioSentence = "I am going to call you",
                wordTokens = listOf("I", "am", "going", "to", "call", "you"),
                explanation = "'going to' thường được lướt âm thành 'gonna' trong văn nói hàng ngày."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các dạng rút gọn âm thanh với dạng đầy đủ 🧩",
                pairs = listOf(
                    PairSlotItem("could've", "could have"),
                    PairSlotItem("should've", "should have"),
                    PairSlotItem("gonna", "going to"),
                    PairSlotItem("wanna", "want to")
                ),
                explanation = "Hiểu các dạng lướt âm giúp bạn nghe hiểu người bản xứ dễ dàng hơn."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu tự nhiên: I could have helped you 🎙️",
                targetSentence = "I could have helped you",
                phoneticFocusWord = "helped",
                explanation = "Lướt nhẹ 'could have' thành /ˈkʊdəv/ để giọng nói mượt mà."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Conversational Chunks" else "Bài 3: Cụm Phản Xạ Mở Đầu Hội Thoại",
            description = if (isEng) "Step 3: Natural 3-4 word conversational chunks." else "Bước 3: Ghép các cụm từ phản xạ tự nhiên dùng trong đời sống.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để nói 'Cứ tự nhiên nhé / Đừng ngại' khi đón khách, bạn dùng cụm nào?",
                options = listOf("Make yourself at home", "Do yourself a house", "Take your home", "Go to your house"),
                correctIndex = 0,
                explanation = "'Make yourself at home' nghĩa là 'Cứ tự nhiên như ở nhà nhé'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành cụm: 'Giữ liên lạc nhé' 🧩",
                targetSentence = "Let us keep in touch",
                wordTokens = listOf("Let", "us", "keep", "in", "touch"),
                distractorTokens = listOf("on"),
                explanation = "'keep in touch' nghĩa là giữ liên lạc."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'Nghe có vẻ tuyệt đấy' 🎧",
                audioSentence = "That sounds like a plan",
                wordTokens = listOf("That", "sounds", "like", "a", "plan"),
                explanation = "'That sounds like a plan' dùng để đồng tình hào hứng với một lời đề nghị."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm phản xạ tự nhiên với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("keep in touch", "giữ liên lạc"),
                    PairSlotItem("make sense", "hợp lý / có lý"),
                    PairSlotItem("by the way", "nhân tiện"),
                    PairSlotItem("as soon as possible", "càng sớm càng tốt")
                ),
                explanation = "Những cụm từ nối giúp câu chuyện trôi chảy không bị ngắt quãng."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu phản xạ: That sounds like a plan 🎙️",
                targetSentence = "That sounds like a plan",
                phoneticFocusWord = "sounds",
                explanation = "Nối âm 'sounds like a' tự nhiên và nhấn vào từ 'plan'."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Natural Everyday Sentences" else "Bài 4: Dựng Câu Giao Tiếp Tự Nhiên Hàng Ngày",
            description = if (isEng) "Step 4: Build complete 6-word conversational sentences." else "Bước 4: Kết hợp các cụm từ thành câu giao tiếp hoàn chỉnh 5-6 từ.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi muốn hẹn gặp lại bạn bè vào cuối tuần, câu nào tự nhiên nhất?",
                options = listOf("Let's catch up this weekend", "We do a meet weekend", "I catch you Sunday", "Make a see you"),
                correctIndex = 0,
                explanation = "'catch up' nghĩa là gặp gỡ trò chuyện cập nhật tình hình sau một thời gian."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu: 'Hãy gặp nhau trò chuyện cuối tuần này nhé' 🧩",
                targetSentence = "Let us catch up this weekend",
                wordTokens = listOf("Let", "us", "catch", "up", "this", "weekend"),
                distractorTokens = listOf("for"),
                explanation = "'catch up' + mốc thời gian là cách rủ bạn bè đi cà phê rất tự nhiên."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu thông báo và sắp xếp lại các từ 🎧",
                audioSentence = "I will let you know soon",
                wordTokens = listOf("I", "will", "let", "you", "know", "soon"),
                explanation = "'I will let you know soon' nghĩa là 'Tôi sẽ báo cho bạn biết sớm nhé'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu giao tiếp hàng ngày với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Let's catch up soon", "Sớm gặp nhau trò chuyện nhé"),
                    PairSlotItem("I'll let you know", "Tôi sẽ báo cho bạn biết"),
                    PairSlotItem("It makes total sense", "Điều đó hoàn toàn hợp lý"),
                    PairSlotItem("Take your time", "Cứ từ từ, không cần vội")
                ),
                explanation = "4 mẫu câu cửa miệng của người bản xứ trong đời sống hàng ngày."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: I will let you know soon 🎙️",
                targetSentence = "I will let you know soon",
                phoneticFocusWord = "know",
                explanation = "Phát âm trôi chảy và giữ nhịp điệu tự nhiên ở cuối câu."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Native Conversational Mastery" else "Bài 5: Tổng Hợp Phản Xạ Đàm Thoại Tự Nhiên",
            description = if (isEng) "Step 5: Full B1 conversational synthesis." else "Bước 5: Tổng hợp kỹ năng nghe hiểu nuốt âm và phản xạ hội thoại thực tế.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi đồng nghiệp nói 'Sorry for the delay', cách đáp lại lịch sự và tự nhiên nhất là gì?",
                options = listOf("No worries at all, take your time", "Yes you are late", "Never mind the clock", "Please do a decision"),
                correctIndex = 0,
                explanation = "'No worries at all, take your time' thể hiện sự thông cảm và lịch thiệp."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu: 'Cảm ơn vì đã báo cho tôi biết' 🧩",
                targetSentence = "Thanks for letting me know",
                wordTokens = listOf("Thanks", "for", "letting", "me", "know"),
                distractorTokens = listOf("to"),
                explanation = "Sau giới từ 'for', động từ thêm '-ing': 'Thanks for letting me know'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu cảm ơn tự nhiên và sắp xếp lại 🎧",
                audioSentence = "Thanks for letting me know",
                wordTokens = listOf("Thanks", "for", "letting", "me", "know"),
                explanation = "Mẫu câu cảm ơn cực kỳ phổ biến trong cả công việc và đời sống."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại phản xạ B1 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Thanks for letting me know", "Cảm ơn vì đã báo cho tôi biết"),
                    PairSlotItem("No worries at all", "Không sao đâu mà"),
                    PairSlotItem("I could've called earlier", "Lẽ ra tôi đã có thể gọi sớm hơn"),
                    PairSlotItem("Let's take a short break", "Hãy nghỉ giải lao một lát nhé")
                ),
                explanation = "Hoàn tất Chặng 1 Trung Cấp: Bạn đã nắm vững Collocations và nối âm thực tế!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu cảm ơn: Thanks for letting me know 🎙️",
                targetSentence = "Thanks for letting me know",
                phoneticFocusWord = "letting",
                explanation = "Đọc liền mạch cụm 'letting me know' với ngữ điệu thân thiện."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildIntermediateStage2PoliteInquiries(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Polite Modal Openers" else "Bài 1: Cụm Mở Đầu Lịch Sự (Could you / Would you mind)",
            description = if (isEng) "Step 1: Polite modal starters for requests." else "Bước 1: Làm chủ các cụm từ mở đầu câu hỏi và nhờ vả lịch thiệp.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Để mở đầu một câu hỏi gián tiếp lịch sự, cụm từ nào trang trọng và tự nhiên nhất?",
                options = listOf("Could you tell me...", "Tell me now...", "What is you know...", "Do you mind tell..."),
                correctIndex = 0,
                explanation = "'Could you tell me...' (Bạn có thể cho tôi biết...) là cụm mở đầu chuẩn mực."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành cụm mở đầu: 'Bạn có thể cho tôi biết...' 🧩",
                targetSentence = "Could you tell me",
                wordTokens = listOf("Could", "you", "tell", "me"),
                distractorTokens = listOf("to"),
                explanation = "'Could you tell me' dùng để mở đầu câu hỏi gián tiếp."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ xin phép lịch sự 🎧",
                audioSentence = "Would you mind helping me",
                wordTokens = listOf("Would", "you", "mind", "helping", "me"),
                explanation = "Sau 'Would you mind' luôn là động từ dạng V-ing ('helping')."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm mở đầu lịch sự với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Could you tell me", "Bạn có thể cho tôi biết"),
                    PairSlotItem("Do you know", "Bạn có biết"),
                    PairSlotItem("Would you mind", "Bạn có phiền không nếu"),
                    PairSlotItem("I was wondering", "Tôi đang tự hỏi liệu")
                ),
                explanation = "4 trụ cột mở lời lịch sự trong môi trường công sở và quốc tế."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm nhờ vả: Would you mind helping me 🎙️",
                targetSentence = "Would you mind helping me",
                phoneticFocusWord = "helping",
                explanation = "Nối âm 'Would you' thành /ˈwʊdʒu/ tự nhiên."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Embedded Question Clauses" else "Bài 2: Trật Tự Từ Trong Mệnh Đề Gián Tiếp",
            description = if (isEng) "Step 2: Statement word order inside indirect questions." else "Bước 2: Nắm vững quy tắc không đảo trợ động từ ở mệnh đề phụ.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Chọn vế sau đúng ngữ pháp cho câu hỏi gián tiếp: 'Could you tell me ___?'",
                options = listOf("where the station is", "where is the station", "where does the station", "station is where"),
                correctIndex = 0,
                explanation = "Trong câu hỏi gián tiếp, mệnh đề sau từ để hỏi giữ trật tự câu kể: Subject + Verb ('the station is')."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành mệnh đề phụ: '...nhà ga ở đâu' 🧩",
                targetSentence = "where the station is",
                wordTokens = listOf("where", "the", "station", "is"),
                distractorTokens = listOf("does"),
                explanation = "Động từ 'is' đặt ở cuối sau chủ ngữ 'the station'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp mệnh đề phụ hỏi giờ mở cửa 🎧",
                audioSentence = "what time it opens",
                wordTokens = listOf("what", "time", "it", "opens"),
                explanation = "Không dùng trợ động từ 'does', động từ 'opens' chia trực tiếp theo chủ ngữ 'it'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hỏi trực tiếp với mệnh đề gián tiếp chuẩn ngữ pháp 🧩",
                pairs = listOf(
                    PairSlotItem("Where is the bank?", "...where the bank is"),
                    PairSlotItem("What time does it start?", "...what time it starts"),
                    PairSlotItem("Is she available?", "...if she is available"),
                    PairSlotItem("How much does it cost?", "...how much it costs")
                ),
                explanation = "Quy tắc vàng của câu hỏi gián tiếp: Bỏ đảo ngữ, giữ trật tự Chủ ngữ + Động từ."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói mệnh đề: where the station is 🎙️",
                targetSentence = "where the station is",
                phoneticFocusWord = "station",
                explanation = "Đọc rõ ràng trật tự 'where the station is'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Polite Workplace Requests" else "Bài 3: Cụm Yêu Cầu Lịch Sự Nơi Công Sở",
            description = if (isEng) "Step 3: Professional email and meeting chunks." else "Bước 3: Ghép các cụm từ trao đổi công việc và email chuyên nghiệp.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi viết email nhờ đồng nghiệp gửi báo cáo, cụm nào lịch sự nhất?",
                options = listOf("I would appreciate it if you could...", "Send me the report now", "You must give me the report", "Do the report for me"),
                correctIndex = 0,
                explanation = "'I would appreciate it if you could...' (Tôi sẽ rất cảm kích nếu bạn có thể...) là chuẩn mực email công sở."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 5 từ thành cụm: 'làm ơn gửi cho tôi bản báo cáo' 🧩",
                targetSentence = "please send me the report",
                wordTokens = listOf("please", "send", "me", "the", "report"),
                distractorTokens = listOf("to"),
                explanation = "Cấu trúc hai tân ngữ: send + [Người nhận] + [Vật]."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ kiểm tra lịch trình 🎧",
                audioSentence = "let me check my schedule",
                wordTokens = listOf("let", "me", "check", "my", "schedule"),
                explanation = "'let me check my schedule' nghĩa là 'để tôi kiểm tra lại lịch trình của mình'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm giao tiếp công sở với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("check my schedule", "kiểm tra lịch trình của tôi"),
                    PairSlotItem("send me the details", "gửi cho tôi thông tin chi tiết"),
                    PairSlotItem("reschedule the meeting", "dời lịch cuộc họp"),
                    PairSlotItem("get back to you", "phản hồi lại cho bạn")
                ),
                explanation = "4 cụm từ xuất hiện hàng ngày trong môi trường làm việc chuyên nghiệp."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm công sở: let me check my schedule 🎙️",
                targetSentence = "let me check my schedule",
                phoneticFocusWord = "schedule",
                explanation = "Phát âm từ 'schedule' chuẩn Mỹ: /ˈskedʒuːl/."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Complete Indirect Questions" else "Bài 4: Ghép Câu Hỏi Gián Tiếp Hoàn Chỉnh",
            description = if (isEng) "Step 4: Combine polite openers with embedded clauses." else "Bước 4: Lắp ghép cụm mở đầu lịch sự và mệnh đề phụ thành câu hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu hỏi gián tiếp nào sau đây hoàn toàn đúng ngữ pháp?",
                options = listOf(
                    "Could you tell me where the station is?",
                    "Could you tell me where is the station?",
                    "Could you tell me where does the station?",
                    "Could you telling me where the station is?"
                ),
                correctIndex = 0,
                explanation = "Kết hợp 'Could you tell me' + 'where the station is' (không đảo 'is' lên trước 'the station')."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu hỏi lịch sự: 'Bạn có thể cho tôi biết nhà ga ở đâu không?' 🧩",
                targetSentence = "Could you tell me where the station is",
                wordTokens = listOf("Could", "you", "tell", "me", "where", "the", "station", "is"),
                distractorTokens = listOf("does"),
                explanation = "Câu hỏi gián tiếp 8 từ hoàn chỉnh và chuẩn mực."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hỏi gián tiếp về giờ họp và sắp xếp lại 🎧",
                audioSentence = "Do you know when the meeting starts",
                wordTokens = listOf("Do", "you", "know", "when", "the", "meeting", "starts"),
                explanation = "'Do you know when the meeting starts?' (Bạn có biết khi nào cuộc họp bắt đầu không?)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu hỏi gián tiếp hoàn chỉnh với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Could you tell me where it is?", "Bạn có thể chỉ giúp tôi nó ở đâu không?"),
                    PairSlotItem("Do you know when it starts?", "Bạn có biết khi nào nó bắt đầu không?"),
                    PairSlotItem("Would you mind opening the window?", "Bạn có phiền mở giúp cửa sổ không?"),
                    PairSlotItem("I wonder if she is free", "Tôi tự hỏi liệu cô ấy có rảnh không")
                ),
                explanation = "Làm chủ hoàn toàn cấu trúc câu hỏi gián tiếp trong mọi ngữ cảnh."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: Could you tell me where the station is 🎙️",
                targetSentence = "Could you tell me where the station is",
                phoneticFocusWord = "station",
                explanation = "Lên giọng nhẹ ở cuối câu để giữ sắc thái lịch sự, nhẹ nhàng."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Diplomatic & Professional Mastery" else "Bài 5: Tổng Hợp Ngoại Giao & Ứng Xử Lịch Thiệp",
            description = if (isEng) "Step 5: Master polite refusal and inquiry dialogues." else "Bước 5: Phản xạ hỏi thông tin và từ chối khéo léo trong giao tiếp.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi không thể tham dự cuộc họp, cách từ chối lịch sự nhất là gì?",
                options = listOf("I am afraid I cannot make it today", "I do not go today", "No meeting for me", "You cancel the meeting"),
                correctIndex = 0,
                explanation = "'I am afraid I cannot make it' (Tôi e là mình không thể tham dự được) là cách từ chối rất khéo léo."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu: 'Tôi e là mình không thể tới được' 🧩",
                targetSentence = "I am afraid I cannot make it",
                wordTokens = listOf("I", "am", "afraid", "I", "cannot", "make", "it"),
                distractorTokens = listOf("do"),
                explanation = "Cụm 'make it' nghĩa là kịp tới hoặc tham dự thành công một sự kiện."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu hẹn phản hồi công việc và sắp xếp lại 🎧",
                audioSentence = "I will get back to you soon",
                wordTokens = listOf("I", "will", "get", "back", "to", "you", "soon"),
                explanation = "'get back to someone' nghĩa là liên hệ/phản hồi lại cho ai đó."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại ngoại giao công sở với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("I'm afraid I can't make it", "Tôi e là mình không thể tham dự được"),
                    PairSlotItem("I'll get back to you soon", "Tôi sẽ phản hồi lại bạn sớm"),
                    PairSlotItem("Could we reschedule for tomorrow?", "Chúng ta dời lịch sang ngày mai được không?"),
                    PairSlotItem("Thank you for your understanding", "Cảm ơn sự thông cảm của bạn")
                ),
                explanation = "Hoàn tất Chặng 2 Trung Cấp: Bạn đã làm chủ nghệ thuật đặt câu hỏi gián tiếp và giao tiếp lịch thiệp!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu phản hồi: I will get back to you soon 🎙️",
                targetSentence = "I will get back to you soon",
                phoneticFocusWord = "back",
                explanation = "Phát âm rõ ràng, tự tin và chuyên nghiệp."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildIntermediateStage3PhrasalVerbs(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Essential Daily Phrasal Verbs" else "Bài 1: Cụm Động Từ Thiết Yếu Hàng Ngày",
            description = if (isEng) "Step 1: High-frequency phrasal verbs." else "Bước 1: Nhận diện các cụm động từ (Phrasal Verbs) xuất hiện nhiều nhất.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm động từ nào có nghĩa là 'Tìm ra / Hiểu ra cách giải quyết'?",
                options = listOf("figure out", "put off", "run out", "give up"),
                correctIndex = 0,
                explanation = "'figure out' nghĩa là suy nghĩ để hiểu ra hoặc tìm ra giải pháp."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành cụm: 'hết / cạn kiệt (thứ gì đó)' 🧩",
                targetSentence = "run out of",
                wordTokens = listOf("run", "out", "of"),
                distractorTokens = listOf("in"),
                explanation = "'run out of + [danh từ]' nghĩa là dùng hết hoặc cạn kiệt thứ gì (VD: run out of time)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm động từ 'mong đợi điều gì' 🎧",
                audioSentence = "look forward to",
                wordTokens = listOf("look", "forward", "to"),
                explanation = "'look forward to' nghĩa là háo hức mong chờ."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các Phrasal Verbs cốt lõi với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("figure out", "tìm ra / hiểu ra"),
                    PairSlotItem("run out of", "hết / cạn kiệt"),
                    PairSlotItem("look forward to", "mong đợi"),
                    PairSlotItem("give up", "từ bỏ")
                ),
                explanation = "4 Phrasal Verbs nền tảng thay thế cho các động từ đơn điệu."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm động từ: figure out 🎙️",
                targetSentence = "figure out",
                phoneticFocusWord = "figure",
                explanation = "Nối âm cuối /r/ của 'figure' sang 'out': /ˈfɪɡjər aʊt/."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Work & Project Phrasal Verbs" else "Bài 2: Cụm Động Từ Trong Công Việc (Put off, Call off)",
            description = if (isEng) "Step 2: Phrasal verbs for meetings and tasks." else "Bước 2: Mở rộng các cụm động từ chỉ việc trì hoãn, hủy bỏ và đề cập vấn đề.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Cụm động từ nào có nghĩa là 'Trì hoãn / Dời lại' (tương đương với 'postpone')?",
                options = listOf("put off", "call off", "turn up", "look after"),
                correctIndex = 0,
                explanation = "'put off' nghĩa là trì hoãn, còn 'call off' nghĩa là hủy bỏ hoàn toàn."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành cụm: 'hủy cuộc họp' 🧩",
                targetSentence = "call off meetings",
                wordTokens = listOf("call", "off", "meetings"),
                distractorTokens = listOf("on"),
                explanation = "'call off' = 'cancel' (hủy bỏ)."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'tiếp tục làm việc' 🎧",
                audioSentence = "carry on working",
                wordTokens = listOf("carry", "on", "working"),
                explanation = "'carry on + V-ing' nghĩa là tiếp tục làm việc gì đó."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các Phrasal Verbs công việc với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("put off", "trì hoãn"),
                    PairSlotItem("call off", "hủy bỏ"),
                    PairSlotItem("bring up", "đề cập / nêu lên"),
                    PairSlotItem("turn down", "từ chối (lời mời/đề nghị)")
                ),
                explanation = "Nắm vững sự khác biệt giữa 'put off' (hoãn) và 'call off' (hủy)."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm động từ: carry on working 🎙️",
                targetSentence = "carry on working",
                phoneticFocusWord = "carry",
                explanation = "Đọc liền mạch 'carry on working'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Phrasal Verb + Object Chunks" else "Bài 3: Kết Hợp Cụm Động Từ Với Tân Ngữ",
            description = if (isEng) "Step 3: Build 3-4 word phrasal verb chunks." else "Bước 3: Ghép cụm động từ với danh từ hoặc V-ing theo đúng cấu trúc.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Sau cụm 'look forward to', động từ tiếp theo phải ở dạng nào?",
                options = listOf("V-ing (e.g., meeting you)", "Nguyên mẫu có to (to meet)", "Quá khứ đơn (met)", "Nguyên mẫu không to (meet)"),
                correctIndex = 0,
                explanation = "Từ 'to' trong 'look forward to' là giới từ, nên theo sau phải là danh từ hoặc V-ing ('meeting you')."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành cụm: 'mong được gặp bạn' 🧩",
                targetSentence = "look forward to meeting",
                wordTokens = listOf("look", "forward", "to", "meeting"),
                distractorTokens = listOf("meet"),
                explanation = "'look forward to + V-ing' là cấu trúc kinh điển trong cả giao tiếp và đề thi."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'hết thời gian' 🎧",
                audioSentence = "run out of time",
                wordTokens = listOf("run", "out", "of", "time"),
                explanation = "'run out of time' nghĩa là hết sạch thời gian."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm Phrasal Verb hoàn chỉnh với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("look forward to seeing you", "mong được gặp bạn"),
                    PairSlotItem("run out of battery", "hết sạch pin"),
                    PairSlotItem("figure out the answer", "tìm ra câu trả lời"),
                    PairSlotItem("put off the trip", "hoãn chuyến đi")
                ),
                explanation = "Ghép Phrasal Verb vào ngữ cảnh cụ thể giúp bạn nhớ lâu gấp 3 lần."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: run out of time 🎙️",
                targetSentence = "run out of time",
                phoneticFocusWord = "time",
                explanation = "Nối âm 'run out of' mượt mà: /rʌn aʊt əv taɪm/."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Phrasal Verb Sentences" else "Bài 4: Dựng Câu Hoàn Chỉnh Với Cụm Động Từ",
            description = if (isEng) "Step 4: Build full 5-6 word sentences using phrasal verbs." else "Bước 4: Lắp ghép thành câu giao tiếp hoàn chỉnh sử dụng Phrasal Verbs.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu nào diễn đạt tự nhiên nhất ý 'Đừng trì hoãn công việc của bạn đến ngày mai'?",
                options = listOf("Do not put off your work", "Do not call in your work", "Never look after your work", "Do not run out your work"),
                correctIndex = 0,
                explanation = "'put off your work' nghĩa là trì hoãn công việc."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 5 từ thành câu: 'Tôi rất mong được gặp bạn' 🧩",
                targetSentence = "I look forward to meeting",
                wordTokens = listOf("I", "look", "forward", "to", "meeting"),
                distractorTokens = listOf("meet"),
                explanation = "Cấu trúc 'I look forward to meeting (you)'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu thông báo và sắp xếp lại các từ 🎧",
                audioSentence = "We ran out of coffee",
                wordTokens = listOf("We", "ran", "out", "of", "coffee"),
                explanation = "'We ran out of coffee' nghĩa là 'Chúng ta đã hết cà phê rồi'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu chứa Phrasal Verbs với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("We ran out of time", "Chúng tôi đã hết thời gian"),
                    PairSlotItem("I will figure it out", "Tôi sẽ tìm ra cách giải quyết"),
                    PairSlotItem("Never give up your dream", "Đừng bao giờ từ bỏ ước mơ"),
                    PairSlotItem("She turned down the offer", "Cô ấy đã từ chối lời đề nghị")
                ),
                explanation = "4 câu giao tiếp thực tế sử dụng Phrasal Verbs chuẩn xác."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: I will figure it out 🎙️",
                targetSentence = "I will figure it out",
                phoneticFocusWord = "figure",
                explanation = "Lưu ý đặt đại từ 'it' vào giữa 'figure' và 'out': 'figure it out'."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Phrasal Verbs Mastery Dialogue" else "Bài 5: Phản Xạ Hội Thoại Với Cụm Động Từ",
            description = if (isEng) "Step 5: Spontaneous dialogue synthesis." else "Bước 5: Vận dụng linh hoạt các cụm động từ trong hội thoại thực tế.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi gặp một bài toán hoặc vấn đề khó trong dự án, câu khích lệ đồng đội tự nhiên nhất là gì?",
                options = listOf("Don't worry, we will figure it out together", "Let's run out of the problem", "We must look forward the issue", "Please turn down the solution"),
                correctIndex = 0,
                explanation = "'We will figure it out together' (Chúng ta sẽ cùng nhau tìm ra cách giải quyết)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 6 từ thành câu: 'Chúng ta sẽ cùng tìm ra cách' 🧩",
                targetSentence = "We will figure it out together",
                wordTokens = listOf("We", "will", "figure", "it", "out", "together"),
                distractorTokens = listOf("off"),
                explanation = "'figure it out together' thể hiện tinh thần hợp tác giải quyết vấn đề."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu lời khuyên và sắp xếp lại các từ 🎧",
                audioSentence = "Please do not give up now",
                wordTokens = listOf("Please", "do", "not", "give", "up", "now"),
                explanation = "'Please do not give up now' (Xin đừng từ bỏ lúc này)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép câu hội thoại tổng hợp Chặng 3 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("We'll figure it out", "Chúng ta sẽ tìm ra cách thôi"),
                    PairSlotItem("Don't put it off", "Đừng trì hoãn nó nữa"),
                    PairSlotItem("They called off the match", "Họ đã hủy trận đấu"),
                    PairSlotItem("I look forward to hearing from you", "Tôi mong sớm nhận được phản hồi từ bạn")
                ),
                explanation = "Hoàn tất Chặng 3 Trung Cấp: Bạn đã làm chủ các Phrasal Verbs thông dụng nhất!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: We will figure it out together 🎙️",
                targetSentence = "We will figure it out together",
                phoneticFocusWord = "together",
                explanation = "Đọc trôi chảy với ngữ điệu khích lệ, tích cực."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildIntermediateStage4ConditionalsSyntax(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Contrast & Cause Connectors" else "Bài 1: Liên Từ Nhượng Bộ & Nhân Quả (Despite, Although)",
            description = if (isEng) "Step 1: Distinguish Despite vs Although." else "Bước 1: Phân biệt cách dùng Although (+ mệnh đề) và Despite (+ cụm danh từ/V-ing).",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Từ nối nào đi trực tiếp trước một cụm danh từ ('the heavy rain') để mang nghĩa 'Mặc dù'?",
                options = listOf("Despite", "Although", "Because", "Unless"),
                correctIndex = 0,
                explanation = "'Despite' / 'In spite of' + Cụm danh từ/V-ing; còn 'Although' + Mệnh đề (S + V)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành cụm: 'mặc dù trời mưa to' 🧩",
                targetSentence = "despite the heavy rain",
                wordTokens = listOf("despite", "the", "heavy", "rain"),
                distractorTokens = listOf("of"),
                explanation = "Lưu ý 'despite' KHÔNG đi kèm giới từ 'of' ('despite the heavy rain')."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm trạng ngữ chỉ sự nhượng bộ 🎧",
                audioSentence = "despite the heavy rain",
                wordTokens = listOf("despite", "the", "heavy", "rain"),
                explanation = "'despite the heavy rain' nghĩa là bất chấp / mặc dù cơn mưa lớn."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các từ nối học thuật B2 với cách dùng chuẩn 🧩",
                pairs = listOf(
                    PairSlotItem("although", "mặc dù (+ mệnh đề S + V)"),
                    PairSlotItem("despite", "mặc dù (+ danh từ / V-ing)"),
                    PairSlotItem("unless", "trừ khi (if... not)"),
                    PairSlotItem("therefore", "do đó / vì vậy")
                ),
                explanation = "4 từ nối cốt lõi để xây dựng câu phức mạch lạc."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm từ: despite the heavy rain 🎙️",
                targetSentence = "despite the heavy rain",
                phoneticFocusWord = "despite",
                explanation = "Nhấn trọng âm vào âm tiết thứ hai của 'despite' (/dɪˈspaɪt/)."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Second Conditional (If I were you)" else "Bài 2: Câu Điều Kiện Loại 2 (Giả Định Hiện Tại)",
            description = if (isEng) "Step 2: Hypothetical present situations." else "Bước 2: Nắm vững cấu trúc giả định không có thật ở hiện tại: If I were you, I would...",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trong câu điều kiện loại 2 khuyên nhủ: 'If I ___ you, I would accept that offer', động từ chuẩn là gì?",
                options = listOf("were", "am", "will be", "have been"),
                correctIndex = 0,
                explanation = "Trong câu điều kiện loại 2 (Subjunctive mood), to be luôn chia là 'were' cho mọi ngôi."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành mệnh đề giả định: 'Nếu tôi là bạn' 🧩",
                targetSentence = "If I were you",
                wordTokens = listOf("If", "I", "were", "you"),
                distractorTokens = listOf("am"),
                explanation = "'If I were you' là cụm mở đầu lời khuyên lịch sự và chuẩn ngữ pháp."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe mệnh đề chính câu điều kiện loại 2 và sắp xếp lại 🎧",
                audioSentence = "I would take the job",
                wordTokens = listOf("I", "would", "take", "the", "job"),
                explanation = "Mệnh đề chính câu điều kiện loại 2 dùng 'would + V nguyên mẫu'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các vế câu điều kiện với chức năng ngữ pháp 🧩",
                pairs = listOf(
                    PairSlotItem("If I were you", "Mệnh đề If loại 2 (giả định hiện tại)"),
                    PairSlotItem("I would accept it", "Mệnh đề chính loại 2 (would + V)"),
                    PairSlotItem("If it rains tomorrow", "Mệnh đề If loại 1 (có thể xảy ra)"),
                    PairSlotItem("we will stay home", "Mệnh đề chính loại 1 (will + V)")
                ),
                explanation = "Phân biệt rõ câu điều kiện loại 1 (có thật) và loại 2 (giả định)."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói mệnh đề: If I were you 🎙️",
                targetSentence = "If I were you",
                phoneticFocusWord = "were",
                explanation = "Nối âm 'If I' thành /ɪfaɪ/ và đọc rõ 'were you'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Third Conditional & Past Regrets" else "Bài 3: Câu Điều Kiện Loại 3 (Giả Định Quá Khứ)",
            description = if (isEng) "Step 3: Expressing past counterfactuals." else "Bước 3: Diễn tả sự tiếc nuối hoặc giả định trái ngược với quá khứ (If + had V3 -> would have V3).",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trong câu điều kiện loại 3: 'If she had studied harder, she ___ passed the exam', cụm từ đúng là gì?",
                options = listOf("would have", "will have", "would had", "had"),
                correctIndex = 0,
                explanation = "Công thức câu điều kiện loại 3: If + S + had + V3/ed, S + would have + V3/ed."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành mệnh đề: 'nếu tôi đã biết sớm hơn' 🧩",
                targetSentence = "if I had known",
                wordTokens = listOf("if", "I", "had", "known"),
                distractorTokens = listOf("knew"),
                explanation = "Quá khứ hoàn thành 'had known' dùng trong mệnh đề If loại 3."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe mệnh đề đảo ngữ điều kiện loại 3 và sắp xếp lại 🎧",
                audioSentence = "Had I known the truth",
                wordTokens = listOf("Had", "I", "known", "the", "truth"),
                explanation = "'Had I known' là dạng đảo ngữ trang trọng của 'If I had known'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cấu trúc giả định nâng cao với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("If I had known", "Nếu (lúc đó) tôi mà biết"),
                    PairSlotItem("Had I known", "Giá mà tôi biết (đảo ngữ)"),
                    PairSlotItem("I would have called", "Lẽ ra tôi đã gọi điện rồi"),
                    PairSlotItem("Otherwise", "Nếu không thì")
                ),
                explanation = "Nắm vững cả dạng chuẩn và dạng đảo ngữ 'Had I known...'."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm đảo ngữ: Had I known the truth 🎙️",
                targetSentence = "Had I known the truth",
                phoneticFocusWord = "known",
                explanation = "Âm 'k' ở đầu từ 'known' (/noʊn/) là âm câm."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Complex Conditional Sentences" else "Bài 4: Lắp Ghép Câu Phức Đa Mệnh Đề",
            description = if (isEng) "Step 4: Construct complete 6-8 word conditional sentences." else "Bước 4: Lắp ghép mệnh đề điều kiện và mệnh đề chính thành câu hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu nào sau đây sử dụng đúng cấu trúc điều kiện loại 2 để đưa ra lời khuyên?",
                options = listOf(
                    "If I were you, I would take that opportunity",
                    "If I am you, I would take that opportunity",
                    "If I were you, I will take that opportunity",
                    "If I had been you, I take that opportunity"
                ),
                correctIndex = 0,
                explanation = "'If I were you, I would + V' là cấu trúc chuẩn mực nhất."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu hoàn chỉnh: 'Nếu tôi là bạn, tôi sẽ nhận công việc đó' 🧩",
                targetSentence = "If I were you I would take it",
                wordTokens = listOf("If", "I", "were", "you", "I", "would", "take", "it"),
                distractorTokens = listOf("will"),
                explanation = "Kết hợp mệnh đề giả định 'If I were you' và mệnh đề kết quả 'I would take it'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu phức chứa từ nối nhượng bộ và sắp xếp lại 🎧",
                audioSentence = "Although it rained we went out",
                wordTokens = listOf("Although", "it", "rained", "we", "went", "out"),
                explanation = "Lưu ý trong tiếng Anh có 'Although' thì KHÔNG dùng 'but' ở vế sau."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu phức B2 với ý nghĩa tương ứng 🧩",
                pairs = listOf(
                    PairSlotItem("If I were you, I'd wait", "Nếu tôi là bạn, tôi sẽ đợi"),
                    PairSlotItem("Had you asked, I'd have helped", "Giá mà bạn hỏi thì tôi đã giúp rồi"),
                    PairSlotItem("Although it was late, he stayed", "Dù đã muộn, anh ấy vẫn ở lại"),
                    PairSlotItem("Unless we hurry, we'll be late", "Trừ khi chúng ta khẩn trương, ta sẽ bị muộn")
                ),
                explanation = "Làm chủ 4 mẫu câu phức quan trọng nhất của trình độ B2."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: If I were you I would take it 🎙️",
                targetSentence = "If I were you I would take it",
                phoneticFocusWord = "would",
                explanation = "Ngắt nhịp nhẹ sau chữ 'you' để câu nói rõ ràng và có sức nặng."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: B2 Syntax & Discourse Mastery" else "Bài 5: Tổng Hợp Cú Pháp Phức & Lập Luận B2",
            description = if (isEng) "Step 5: Synthesize conditionals and connectors." else "Bước 5: Vận dụng câu điều kiện và liên từ nâng cao trong lập luận thực tế.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Chọn câu viết lại đồng nghĩa chuẩn nhất với: 'Because he did not prepare well, he failed the interview'?",
                options = listOf(
                    "Had he prepared well, he would have passed the interview",
                    "If he prepares well, he will pass the interview",
                    "Despite preparing well, he failed the interview",
                    "Unless he prepared well, he passed the interview"
                ),
                correctIndex = 0,
                explanation = "Sự việc đã xảy ra trong quá khứ nên dùng đảo ngữ điều kiện loại 3: 'Had he prepared well, he would have passed...'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp các từ thành câu đảo ngữ loại 3: 'Giá mà tôi biết thì tôi đã giúp bạn' 🧩",
                targetSentence = "Had I known I would have helped",
                wordTokens = listOf("Had", "I", "known", "I", "would", "have", "helped"),
                distractorTokens = listOf("will"),
                explanation = "'Had I known, I would have helped' là cấu trúc B2-C1 cực kỳ ăn điểm."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu điều kiện với 'Unless' và sắp xếp lại 🎧",
                audioSentence = "Unless you try you will never know",
                wordTokens = listOf("Unless", "you", "try", "you", "will", "never", "know"),
                explanation = "'Unless you try, you will never know' (Trừ khi bạn thử, bạn sẽ không bao giờ biết được)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cấu trúc cú pháp B2 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Provided that", "Với điều kiện là"),
                    PairSlotItem("As long as", "Miễn là"),
                    PairSlotItem("In case of emergency", "Trong trường hợp khẩn cấp"),
                    PairSlotItem("Regardless of the cost", "Bất kể chi phí ra sao")
                ),
                explanation = "Hoàn tất Chặng 4 Trung Cấp: Bạn đã sẵn sàng tiến lên ngưỡng Cao Cấp (Advanced C1)!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu châm ngôn: Unless you try you will never know 🎙️",
                targetSentence = "Unless you try you will never know",
                phoneticFocusWord = "never",
                explanation = "Nhấn giọng vào 'try' và 'never know' để tạo sự truyền cảm."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildAdvancedStage1Inversion(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Negative Adverbial Openers" else "Bài 1: Trạng Từ Phủ Định Mở Đầu Câu (Seldom, Rarely)",
            description = if (isEng) "Step 1: C1 negative and restrictive adverbials." else "Bước 1: Nhận diện các trạng từ phủ định/hạn chế kích hoạt cấu trúc đảo ngữ C1.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi đặt trạng từ phủ định 'Seldom' hoặc 'Never' lên đầu câu để nhấn mạnh, cấu trúc theo sau bắt buộc phải như thế nào?",
                options = listOf(
                    "Đảo trợ động từ lên trước chủ ngữ (Auxiliary + Subject + Verb)",
                    "Giữ nguyên trật tự S + V bình thường",
                    "Bỏ hoàn toàn động từ chính",
                    "Chuyển động từ sang dạng V-ing"
                ),
                correctIndex = 0,
                explanation = "Quy tắc Đảo ngữ (Negative Inversion): Trạng từ phủ định đứng đầu câu -> Trợ động từ + Chủ ngữ + Động từ."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành cụm mở đầu đảo ngữ: 'Hiếm khi nào tôi từng thấy...' 🧩",
                targetSentence = "Seldom have I seen",
                wordTokens = listOf("Seldom", "have", "I", "seen"),
                distractorTokens = listOf("saw"),
                explanation = "'Seldom' + trợ động từ 'have' + chủ ngữ 'I' + V3 'seen'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm đảo ngữ với 'Never before' 🎧",
                audioSentence = "Never before have we witnessed",
                wordTokens = listOf("Never", "before", "have", "we", "witnessed"),
                explanation = "'Never before have we witnessed...' (Chưa bao giờ trước đây chúng ta chứng kiến...)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các trạng từ mở đầu đảo ngữ C1 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Seldom / Rarely", "Hiếm khi"),
                    PairSlotItem("Under no circumstances", "Dưới bất kỳ hoàn cảnh nào cũng không"),
                    PairSlotItem("Little did she know", "Cô ấy hầu như không hề hay biết"),
                    PairSlotItem("Not only... but also", "Không những... mà còn")
                ),
                explanation = "4 cấu trúc đảo ngữ trọng tâm của cấp độ C1 và IELTS 8.0+."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm đảo ngữ: Seldom have I seen 🎙️",
                targetSentence = "Seldom have I seen",
                phoneticFocusWord = "Seldom",
                explanation = "Nhấn mạnh vào từ 'Seldom' ở đầu cụm."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Auxiliary Inversion Mechanics" else "Bài 2: Cơ Chế Đảo Trợ Động Từ (Little did, Hardly had)",
            description = if (isEng) "Step 2: Past and perfect auxiliary inversion." else "Bước 2: Thực hành đảo trợ động từ 'did', 'had', 'should' theo từng thì.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Chọn cụm từ đúng để hoàn thiện câu đảo ngữ: 'Little ___ that the decision would transform the entire industry.'",
                options = listOf("did they realize", "they realized", "do they realized", "have they realize"),
                correctIndex = 0,
                explanation = "Vì mệnh đề sau ở quá khứ ('would transform'), ta mượn trợ động từ quá khứ 'did' đảo lên trước 'they': 'Little did they realize'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 5 từ thành cụm: 'Anh ấy không hề nhận ra sự thật' 🧩",
                targetSentence = "Little did he realize the truth",
                wordTokens = listOf("Little", "did", "he", "realize", "the", "truth"),
                distractorTokens = listOf("realized"),
                explanation = "Sau trợ động từ 'did', động từ 'realize' trở về nguyên mẫu."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm đảo ngữ cấm đoán tuyệt đối 🎧",
                audioSentence = "Under no circumstances should you share",
                wordTokens = listOf("Under", "no", "circumstances", "should", "you", "share"),
                explanation = "'Under no circumstances should you...' (Trong bất kỳ hoàn cảnh nào bạn cũng không nên...)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cặp liên từ đảo ngữ thời gian với cấu trúc chuẩn 🧩",
                pairs = listOf(
                    PairSlotItem("Hardly had... when", "Vừa mới... thì"),
                    PairSlotItem("No sooner had... than", "Vừa mới... thì (đi với than)"),
                    PairSlotItem("Only after", "Chỉ sau khi (đảo ngữ mệnh đề chính)"),
                    PairSlotItem("Not until", "Mãi cho đến khi (đảo ngữ mệnh đề chính)")
                ),
                explanation = "Cặp kinh điển: Hardly... when và No sooner... than."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm đảo ngữ: Little did he realize the truth 🎙️",
                targetSentence = "Little did he realize the truth",
                phoneticFocusWord = "realize",
                explanation = "Giữ nhịp điệu dứt khoát và nhấn vào 'Little' và 'truth'."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Correlative Clauses (Not only... but also)" else "Bài 3: Cấu Trúc Song Hành (Not Only... But Also)",
            description = if (isEng) "Step 3: Parallel emphatic structures." else "Bước 3: Ghép mệnh đề song hành nhấn mạnh kép trong văn phong hùng biện.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi mở đầu câu bằng 'Not only', vế đầu tiên biến đổi như thế nào?",
                options = listOf(
                    "Not only did she exceed the target, but she also mentored the team",
                    "Not only she exceeded the target, but also mentored",
                    "Not only did she exceeded the target, but she also",
                    "Not only she did exceed the target, also she mentored"
                ),
                correctIndex = 0,
                explanation = "'Not only + did + S + V(nguyên mẫu), but + S + also + V(chia thì)'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 6 từ thành vế đảo ngữ: 'Không những nó tiết kiệm chi phí...' 🧩",
                targetSentence = "Not only does it reduce costs",
                wordTokens = listOf("Not", "only", "does", "it", "reduce", "costs"),
                distractorTokens = listOf("reduces"),
                explanation = "Mượn trợ động từ hiện tại đơn 'does' trước chủ ngữ 'it'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp vế đảo ngữ với 'Only by' 🎧",
                audioSentence = "Only by innovating can we succeed",
                wordTokens = listOf("Only", "by", "innovating", "can", "we", "succeed"),
                explanation = "Khi 'Only by + V-ing' đứng đầu câu, mệnh đề chính đảo 'can we succeed'."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các vế câu hùng biện C1 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Not only does it save time", "Không những nó tiết kiệm thời gian"),
                    PairSlotItem("but it also enhances quality", "mà nó còn nâng cao chất lượng"),
                    PairSlotItem("Only by working together", "Chỉ bằng cách hợp tác cùng nhau"),
                    PairSlotItem("can we overcome this challenge", "chúng ta mới có thể vượt qua thử thách này")
                ),
                explanation = "Cấu trúc 'Only by... can we...' tạo sức nặng cực lớn trong thuyết trình."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu hùng biện: Only by innovating can we succeed 🎙️",
                targetSentence = "Only by innovating can we succeed",
                phoneticFocusWord = "innovating",
                explanation = "Nhấn trọng âm rõ ở 'innovating' và 'succeed'."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Full Inverted Syntax Construction" else "Bài 4: Kiến Tạo Câu Đảo Ngữ Học Thuật Hoàn Chỉnh",
            description = if (isEng) "Step 4: Build complete C1 inverted sentences." else "Bước 4: Lắp ghép hoàn chỉnh các câu đảo ngữ phức hợp 7-8 từ.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Chọn câu đảo ngữ hoàn chỉnh và chuẩn xác nhất ở trình độ C1:",
                options = listOf(
                    "Seldom have I encountered such dedication in my career",
                    "Seldom I have encountered such dedication in my career",
                    "Seldom did I encountered such dedication in my career",
                    "Seldom have I encounter such dedication in my career"
                ),
                correctIndex = 0,
                explanation = "'Seldom + have + I + encountered (V3)' là cấu trúc hiện tại hoàn thành đảo ngữ chuẩn xác."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 7 từ thành câu đảo ngữ: 'Hiếm khi nào tôi thấy sự cống hiến như vậy' 🧩",
                targetSentence = "Seldom have I seen such remarkable dedication",
                wordTokens = listOf("Seldom", "have", "I", "seen", "such", "remarkable", "dedication"),
                distractorTokens = listOf("saw"),
                explanation = "Cấu trúc 'Seldom have I seen + such + Adj + Noun'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu đảo ngữ với 'Hardly' và sắp xếp lại 🎧",
                audioSentence = "Hardly had we arrived when it started",
                wordTokens = listOf("Hardly", "had", "we", "arrived", "when", "it", "started"),
                explanation = "'Hardly had we arrived when it started' (Chúng tôi vừa mới tới nơi thì nó bắt đầu)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu đảo ngữ C1 hoàn chỉnh với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("Seldom have I seen such talent", "Hiếm khi nào tôi thấy tài năng như vậy"),
                    PairSlotItem("Hardly had I left when she called", "Tôi vừa mới rời đi thì cô ấy gọi"),
                    PairSlotItem("Were I in your position, I'd accept", "Nếu tôi ở vị trí của bạn, tôi sẽ đồng ý"),
                    PairSlotItem("Should you need help, please contact me", "Nếu bạn cần hỗ trợ, xin hãy liên hệ tôi")
                ),
                explanation = "Bao quát cả đảo ngữ trạng từ phủ định và đảo ngữ câu điều kiện loại 1, 2, 3."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: Seldom have I seen such remarkable dedication 🎙️",
                targetSentence = "Seldom have I seen such remarkable dedication",
                phoneticFocusWord = "dedication",
                explanation = "Phát âm chuẩn xác, đĩnh đạc với phong thái diễn giả."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Executive Rhetoric & Debate Synthesis" else "Bài 5: Tổng Hợp Hùng Biện & Tranh Biện Cấp Cao",
            description = if (isEng) "Step 5: High-stakes rhetorical mastery." else "Bước 5: Tổng hợp kỹ năng đảo ngữ, câu chẻ nhấn mạnh và lập luận sắc bén.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trong thư tín thương mại cấp cao, cách viết đảo ngữ nào tương đương với 'If you require any further information'?",
                options = listOf(
                    "Should you require any further information, please do not hesitate to contact us",
                    "Would you require any further information, please contact us",
                    "Had you require any further information, please contact us",
                    "Were you require any further information, please contact us"
                ),
                correctIndex = 0,
                explanation = "'Should + S + V(nguyên mẫu)' là đảo ngữ câu điều kiện loại 1 cực kỳ trang trọng trong kinh doanh."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 7 từ thành mệnh đề đảo ngữ trang trọng: 'Nếu bạn cần thêm bất kỳ sự hỗ trợ nào' 🧩",
                targetSentence = "Should you require any further assistance today",
                wordTokens = listOf("Should", "you", "require", "any", "further", "assistance", "today"),
                distractorTokens = listOf("requires"),
                explanation = "'Should you require...' thể hiện đẳng cấp giao tiếp C1 chuyên nghiệp."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu kết luận hùng biện và sắp xếp lại 🎧",
                audioSentence = "Not only is this strategy viable it is essential",
                wordTokens = listOf("Not", "only", "is", "this", "strategy", "viable", "it", "is", "essential"),
                explanation = "'Not only is this strategy viable, it is essential' (Chiến lược này không những khả thi mà còn thiết yếu)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cấu trúc hùng biện C1 với sắc thái biểu đạt 🧩",
                pairs = listOf(
                    PairSlotItem("Should you require assistance", "Đảo ngữ điều kiện loại 1 trang trọng"),
                    PairSlotItem("Were we to invest now", "Đảo ngữ điều kiện loại 2 giả định chiến lược"),
                    PairSlotItem("Had we anticipated the risk", "Đảo ngữ điều kiện loại 3 phân tích quá khứ"),
                    PairSlotItem("What matters most is execution", "Câu chẻ (Wh-Cleft) nhấn mạnh trọng tâm")
                ),
                explanation = "Hoàn tất Chặng 1 Cao Cấp: Bạn đã làm chủ hoàn toàn hệ thống đảo ngữ và cú pháp hùng biện C1!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói phong thái lãnh đạo: Should you require any further assistance today 🎙️",
                targetSentence = "Should you require any further assistance today",
                phoneticFocusWord = "assistance",
                explanation = "Phát âm rõ âm /s/ trong 'assistance' và giữ ngữ điệu điềm tĩnh, lịch lãm."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }

    private fun buildAdvancedStage2Negotiation(level: String, isEng: Boolean): List<InjectedAtomicLessonUnit> {
        val u1 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 1: Academic & Executive Lexicon" else "Bài 1: Từ Vựng Học Thuật & Chiến Lược C1 (Mitigate, Consensus)",
            description = if (isEng) "Step 1: High-precision C1 vocabulary." else "Bước 1: Làm chủ các từ vựng học thuật và quản trị chiến lược cấp độ C1.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Trong quản trị rủi ro và IELTS Writing, động từ C1 nào có nghĩa là 'Giảm thiểu mức độ nghiêm trọng (của rủi ro/thiệt hại)'?",
                options = listOf("mitigate", "exacerbate", "proliferate", "speculate"),
                correctIndex = 0,
                explanation = "'mitigate' (/ˈmɪtɪɡeɪt/) nghĩa là làm dịu đi, giảm nhẹ rủi ro (mitigate risks); trái nghĩa với 'exacerbate' (làm trầm trọng thêm)."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 3 từ thành Collocation C1: 'đạt được sự đồng thuận' 🧩",
                targetSentence = "reach a consensus",
                wordTokens = listOf("reach", "a", "consensus"),
                distractorTokens = listOf("do"),
                explanation = "'reach a consensus' nghĩa là đạt được sự đồng thuận chung giữa các bên."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ học thuật 'giảm thiểu rủi ro tiềm ẩn' 🎧",
                audioSentence = "mitigate potential risks",
                wordTokens = listOf("mitigate", "potential", "risks"),
                explanation = "'mitigate potential risks' là cụm từ đắt giá trong cả kinh doanh lẫn bài luận IELTS."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép từ vựng học thuật C1 với nghĩa tiếng Việt chính xác 🧩",
                pairs = listOf(
                    PairSlotItem("mitigate", "giảm thiểu (rủi ro / tác động xấu)"),
                    PairSlotItem("consensus", "sự đồng thuận chung"),
                    PairSlotItem("unprecedented", "chưa từng có tiền lệ"),
                    PairSlotItem("feasible", "khả thi / có thể thực hiện được")
                ),
                explanation = "4 từ vựng trụ cột của trình độ C1 Academic & Business."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện phát âm cụm học thuật: mitigate potential risks 🎙️",
                targetSentence = "mitigate potential risks",
                phoneticFocusWord = "mitigate",
                explanation = "Phát âm 'mitigate' (/ˈmɪtɪɡeɪt/) rõ 3 âm tiết và âm cuối /ks/ trong 'risks'."
            )
        )

        val u2 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 2: Negotiation Verbs & Collocations" else "Bài 2: Thuật Ngữ Đàm Phán & Thuyết Phục (Substantiate, Leverage)",
            description = if (isEng) "Step 2: Verbs for argumentation and negotiation." else "Bước 2: Mở rộng động từ chứng minh luận điểm và tận dụng lợi thế trên bàn đàm phán.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Động từ học thuật nào có nghĩa là 'Chứng minh / Củng cố (một nhận định) bằng bằng chứng cụ thể'?",
                options = listOf("substantiate", "fluctuate", "deteriorate", "compromise"),
                correctIndex = 0,
                explanation = "'substantiate a claim' (/səbˈstænʃieɪt/) nghĩa là đưa ra bằng chứng xác thực để chứng minh một luận điểm."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 4 từ thành cụm: 'lợi thế chung cho đôi bên' 🧩",
                targetSentence = "mutually beneficial agreement",
                wordTokens = listOf("mutually", "beneficial", "agreement"),
                distractorTokens = listOf("mutual"),
                explanation = "Trạng từ 'mutually' bổ nghĩa cho tính từ 'beneficial', đứng trước danh từ 'agreement'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ 'chứng minh luận điểm bằng dữ liệu' 🎧",
                audioSentence = "substantiate the claim with data",
                wordTokens = listOf("substantiate", "the", "claim", "with", "data"),
                explanation = "'substantiate the claim with data' là cụm chuẩn mực trong nghiên cứu và báo cáo."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các thuật ngữ đàm phán chiến lược với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("substantiate", "chứng minh bằng bằng chứng"),
                    PairSlotItem("leverage", "tận dụng tối đa (lợi thế / nguồn lực)"),
                    PairSlotItem("compromise", "thỏa hiệp / nhượng bộ đôi bên"),
                    PairSlotItem("counterpart", "đối tác tương ứng")
                ),
                explanation = "Từ vựng giúp bạn nâng tầm khả năng tranh biện và đàm phán quốc tế."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm đàm phán: mutually beneficial agreement 🎙️",
                targetSentence = "mutually beneficial agreement",
                phoneticFocusWord = "beneficial",
                explanation = "Nhấn trọng âm vào âm tiết thứ ba của 'beneficial' (/ˌbenɪˈfɪʃl/)."
            )
        )

        val u3 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 3: Diplomatic Hedging Chunks" else "Bài 3: Nghệ Thuật Rào Đón Ngoại Giao (Diplomatic Hedging)",
            description = if (isEng) "Step 3: Soften disagreements professionally." else "Bước 3: Sử dụng các cụm từ rào đón để phản biện sắc sảo nhưng vẫn giữ sự tôn trọng tối đa.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Khi muốn phản bác ý kiến của đối tác trong cuộc họp cấp cao một cách ngoại giao nhất, bạn mở lời thế nào?",
                options = listOf(
                    "I see your point, however we must also consider...",
                    "Your point is wrong because...",
                    "I totally reject your idea...",
                    "You do not understand the market..."
                ),
                correctIndex = 0,
                explanation = "Ghi nhận quan điểm đối phương ('I see your point') trước khi chuyển ý ('however, we must also consider...') là đỉnh cao ngoại giao."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 5 từ thành cụm mở đầu phản biện lịch thiệp: 'Với tất cả sự tôn trọng, tôi...' 🧩",
                targetSentence = "With all due respect I",
                wordTokens = listOf("With", "all", "due", "respect", "I"),
                distractorTokens = listOf("for"),
                explanation = "'With all due respect' là cụm mở lời trang trọng trước khi đưa ra góc nhìn trái chiều."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe và sắp xếp cụm từ đề xuất phương án thay thế 🎧",
                audioSentence = "From a strategic perspective we propose",
                wordTokens = listOf("From", "a", "strategic", "perspective", "we", "propose"),
                explanation = "'From a strategic perspective, we propose...' (Từ góc nhìn chiến lược, chúng tôi đề xuất...)."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các cụm ngoại giao C1 với mục đích giao tiếp 🧩",
                pairs = listOf(
                    PairSlotItem("With all due respect", "Mở lời trước khi nêu ý kiến trái chiều"),
                    PairSlotItem("To a certain extent", "Đồng tình một phần (Ở một mức độ nhất định)"),
                    PairSlotItem("From a strategic standpoint", "Đưa ra góc nhìn mang tính chiến lược"),
                    PairSlotItem("Find common ground", "Tìm tiếng nói chung giữa hai bên")
                ),
                explanation = "Làm chủ ngôn ngữ ngoại giao giúp bạn thuyết phục mọi đối tác khó tính."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cụm ngoại giao: From a strategic perspective we propose 🎙️",
                targetSentence = "From a strategic perspective we propose",
                phoneticFocusWord = "perspective",
                explanation = "Phát âm 'perspective' (/pərˈspektɪv/) rõ ràng và đĩnh đạc."
            )
        )

        val u4 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 4: Strategic Argument Sentences" else "Bài 4: Kiến Tạo Luận Điểm Chiến Lược Hoàn Chỉnh",
            description = if (isEng) "Step 4: Build complete C1 negotiation sentences." else "Bước 4: Lắp ghép từ vựng C1 và cấu trúc ngoại giao thành câu hoàn chỉnh.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Câu nào diễn đạt chuẩn xác nhất ý 'Mục tiêu của chúng tôi là đạt được một thỏa thuận đôi bên cùng có lợi'?",
                options = listOf(
                    "Our objective is to reach a mutually beneficial agreement",
                    "Our objective is doing a mutual good paper",
                    "We want make a consensus beneficial together",
                    "Our target is reach agreement each other"
                ),
                correctIndex = 0,
                explanation = "'reach a mutually beneficial agreement' là cấu trúc chuẩn C1."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 7 từ thành câu: 'Chúng ta phải hành động ngay để giảm thiểu rủi ro' 🧩",
                targetSentence = "We must act now to mitigate risks",
                wordTokens = listOf("We", "must", "act", "now", "to", "mitigate", "risks"),
                distractorTokens = listOf("mitigating"),
                explanation = "Cấu trúc chỉ mục đích 'to + V(nguyên mẫu)': 'to mitigate risks'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu kết luận đàm phán và sắp xếp lại 🎧",
                audioSentence = "Let us find common ground today",
                wordTokens = listOf("Let", "us", "find", "common", "ground", "today"),
                explanation = "'find common ground' nghĩa là tìm điểm chung / tiếng nói chung."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các câu luận điểm C1 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("We must mitigate these risks", "Chúng ta phải giảm thiểu những rủi ro này"),
                    PairSlotItem("The evidence substantiates our claim", "Bằng chứng đã củng cố vững chắc nhận định của chúng ta"),
                    PairSlotItem("Both parties reached a consensus", "Cả hai bên đã đạt được sự đồng thuận"),
                    PairSlotItem("This proposal is highly feasible", "Đề xuất này có tính khả thi rất cao")
                ),
                explanation = "4 câu luận điểm sắc bén dùng trực tiếp trong họp chiến lược và IELTS Writing Task 2."
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói cả câu: We must act now to mitigate risks 🎙️",
                targetSentence = "We must act now to mitigate risks",
                phoneticFocusWord = "mitigate",
                explanation = "Nhấn giọng vào 'act now' và 'mitigate risks' để thể hiện sự quyết đoán."
            )
        )

        val u5 = InjectedAtomicLessonUnit(
            title = if (isEng) "Lesson 5: Executive Capstone & C1 Mastery" else "Bài 5: Đỉnh Cao Đàm Phán & Làm Chủ Cấp Độ C1",
            description = if (isEng) "Step 5: Complete C1 executive mastery synthesis." else "Bước 5: Tổng hợp toàn diện từ vựng học thuật, đảo ngữ và nghệ thuật đàm phán cấp cao.",
            level = level,
            slot1Cloze = ClozeChoiceSlot(
                promptQuestion = "Chọn câu tổng hợp kết hợp hoàn hảo giữa Đảo ngữ C1 và Từ vựng Đàm phán:",
                options = listOf(
                    "Only by finding common ground can we reach a sustainable consensus",
                    "Only by finding common ground we can do a consensus",
                    "Seldom we reach a mutually beneficial agreement",
                    "Despite the consensus, but we mitigate the risk"
                ),
                correctIndex = 0,
                explanation = "'Only by + V-ing + can we + V' kết hợp với 'find common ground' và 'reach a sustainable consensus'."
            ),
            slot2SentenceBuilder = SentenceBuilderSlot(
                promptInstruction = "Sắp xếp 8 từ thành câu tổng kết C1: 'Chỉ bằng cách hợp tác chúng ta mới đạt được đồng thuận' 🧩",
                targetSentence = "Only by collaborating can we reach a consensus",
                wordTokens = listOf("Only", "by", "collaborating", "can", "we", "reach", "a", "consensus"),
                distractorTokens = listOf("reaching"),
                explanation = "Đảo trợ động từ 'can' lên trước 'we' sau cụm 'Only by collaborating'."
            ),
            slot3Listening = ListeningChallengeSlot(
                promptInstruction = "Nghe câu phát biểu cấp cao và sắp xếp lại 🎧",
                audioSentence = "We look forward to a prosperous partnership",
                wordTokens = listOf("We", "look", "forward", "to", "a", "prosperous", "partnership"),
                explanation = "'a prosperous partnership' nghĩa là một mối quan hệ hợp tác thịnh vượng, thành công."
            ),
            slot4Matching = MatchingPairsSlot(
                promptInstruction = "Ghép các tuyên bố lãnh đạo C1 với nghĩa tiếng Việt 🧩",
                pairs = listOf(
                    PairSlotItem("reach a sustainable consensus", "đạt được sự đồng thuận bền vững"),
                    PairSlotItem("a prosperous partnership", "mối quan hệ hợp tác thịnh vượng"),
                    PairSlotItem("unprecedented growth", "sự tăng trưởng chưa từng có tiền lệ"),
                    PairSlotItem("strategic alignment", "sự đồng bộ về mặt chiến lược")
                ),
                explanation = "Chúc mừng bạn đã chinh phục trọn vẹn Lộ trình C1 Cao Cấp của Kiki AI!"
            ),
            slot5Speaking = SpeakingChallengeSlot(
                promptInstruction = "Luyện nói câu kết luận cấp cao: Only by collaborating can we reach a consensus 🎙️",
                targetSentence = "Only by collaborating can we reach a consensus",
                phoneticFocusWord = "consensus",
                explanation = "Phát âm đĩnh đạc, truyền cảm hứng như một nhà lãnh đạo thực thụ."
            )
        )

        return listOf(u1, u2, u3, u4, u5)
    }
}
