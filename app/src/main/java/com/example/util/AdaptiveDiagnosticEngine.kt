package com.example.util

import com.example.model.AdaptiveDiagnosticItem
import com.example.model.DiagnosticSkillType
import com.example.model.LearnerPersonaProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Động cơ Đánh giá Thích ứng Chẩn đoán (Computerized Adaptive Testing - CAT)
 * Ứng dụng Thuyết Ứng Đáp Câu Hỏi (Item Response Theory - IRT) và Mô hình Rasch
 * để ước lượng tham số năng lực (Theta Ability) và xác định lộ trình học tối ưu.
 */
object AdaptiveDiagnosticEngine {

    // --- ITEM BANK HIỆU CHUẨN (CALIBRATED QUESTION BANK) ---
    val itemBank: List<AdaptiveDiagnosticItem> = listOf(
        // === STAGE 1: Core Grammar & Syntax Anchors ===
        AdaptiveDiagnosticItem(
            id = 101,
            skillType = DiagnosticSkillType.GRAMMAR,
            difficultyB = -0.8f,
            cefrTier = "A1-A2",
            questionPrompt = "She ______ English with Kiki every day to build confidence.",
            options = listOf("practices", "practice", "practicing", "practiced"),
            correctIndex = 0,
            explanation = "Chủ ngữ số ít 'She' ở thì Hiện tại đơn: động từ thêm -s/-es.",
            skillTag = "Present_Simple_Agreement"
        ),
        AdaptiveDiagnosticItem(
            id = 102,
            skillType = DiagnosticSkillType.GRAMMAR,
            difficultyB = 0.0f,
            cefrTier = "B1-B2",
            questionPrompt = "If I ______ more free time, I would travel around the world.",
            options = listOf("have", "had", "will have", "would have"),
            correctIndex = 1,
            explanation = "Câu điều kiện loại 2 giả định trái với hiện tại: If + S + V(quá khứ đơn), S + would + V.",
            skillTag = "Conditional_Type_2"
        ),
        AdaptiveDiagnosticItem(
            id = 103,
            skillType = DiagnosticSkillType.GRAMMAR,
            difficultyB = 1.1f,
            cefrTier = "B2-C1",
            questionPrompt = "Scarcely ______ entered the room when the emergency alarm rang.",
            options = listOf("had he", "he had", "did he", "has he"),
            correctIndex = 0,
            explanation = "Cấu trúc đảo ngữ nhấn mạnh với trạng từ phủ định: Scarcely + had + S + V3/ed... when...",
            skillTag = "Negative_Inversion"
        ),

        // === STAGE 2: Lexical Breadth & Collocations ===
        AdaptiveDiagnosticItem(
            id = 201,
            skillType = DiagnosticSkillType.VOCABULARY,
            difficultyB = -0.7f,
            cefrTier = "A1-A2",
            questionPrompt = "We decided to ______ dinner together at a nearby Italian restaurant.",
            options = listOf("have", "do", "make", "take"),
            correctIndex = 0,
            explanation = "Cụm từ tự nhiên chuẩn bản xứ (Collocation): 'have dinner' (dùng bữa tối).",
            skillTag = "Basic_Collocation"
        ),
        AdaptiveDiagnosticItem(
            id = 202,
            skillType = DiagnosticSkillType.VOCABULARY,
            difficultyB = 0.2f,
            cefrTier = "B1-B2",
            questionPrompt = "Could you please ______ by my office on your way home?",
            options = listOf("drop", "fall", "pass", "stand"),
            correctIndex = 0,
            explanation = "Phrasal verb thông dụng: 'drop by' (ghé tạt qua trong chốc lát).",
            skillTag = "Phrasal_Verbs"
        ),
        AdaptiveDiagnosticItem(
            id = 203,
            skillType = DiagnosticSkillType.VOCABULARY,
            difficultyB = 1.2f,
            cefrTier = "B2-C1",
            questionPrompt = "The laboratory announced a ______ breakthrough in quantum computing.",
            options = listOf("groundbreaking", "rough", "heavy", "tough"),
            correctIndex = 0,
            explanation = "Thuật ngữ học thuật nâng cao: 'groundbreaking breakthrough' (đột phá mang tính cách mạng).",
            skillTag = "Academic_Collocation"
        ),

        // === STAGE 3: Acoustic Decoding & Connected Speech (Listening) ===
        AdaptiveDiagnosticItem(
            id = 301,
            skillType = DiagnosticSkillType.LISTENING,
            difficultyB = -0.6f,
            cefrTier = "A1-A2",
            questionPrompt = "Nghe câu sau và chọn câu viết lại chính xác 🎧",
            audioSentence = "It is very nice to meet you this morning.",
            options = listOf(
                "It is very nice to meet you this morning.",
                "It is very nice meeting with you morning.",
                "It was nice to meet you yesterday morning.",
                "Is it nice to meet your morning?"
            ),
            correctIndex = 0,
            explanation = "Luyện nhận diện ngữ âm câu chào hỏi cơ bản trong giao tiếp hàng ngày.",
            skillTag = "Acoustic_Everyday_Greeting"
        ),
        AdaptiveDiagnosticItem(
            id = 302,
            skillType = DiagnosticSkillType.LISTENING,
            difficultyB = 0.3f,
            cefrTier = "B1-B2",
            questionPrompt = "Nghe âm thanh và nhận diện cụm từ nuốt âm (Connected Speech) 🎧",
            audioSentence = "We could've gone to the concert if we had booked earlier.",
            options = listOf(
                "We could've gone to the concert",
                "We could have go to the concert",
                "We can go to the concert",
                "We would be going to the concert"
            ),
            correctIndex = 0,
            explanation = "Hiện tượng nuốt âm (Weak form / Elision): 'could have' đọc lướt thành 'could've'.",
            skillTag = "Acoustic_Connected_Speech"
        ),
        AdaptiveDiagnosticItem(
            id = 303,
            skillType = DiagnosticSkillType.LISTENING,
            difficultyB = 1.3f,
            cefrTier = "B2-C1",
            questionPrompt = "Lắng nghe câu nghị luận học thuật và chọn nội dung chính xác 🎧",
            audioSentence = "Technological innovation has fundamentally reshaped modern global industries.",
            options = listOf(
                "Technological innovation has fundamentally reshaped modern global industries.",
                "Technology invented has fundamentally shaped all modern industries.",
                "Technological innovators have found mental shape in industries.",
                "Technological invention was fundamental for reshaped industries."
            ),
            correctIndex = 0,
            explanation = "Nhận diện từ vựng đa âm tiết ở tốc độ nói tự nhiên bản xứ.",
            skillTag = "Acoustic_Academic_Cadence"
        ),

        // === STAGE 4: Spoken Output & Pronunciation (Speaking) ===
        AdaptiveDiagnosticItem(
            id = 401,
            skillType = DiagnosticSkillType.SPEAKING,
            difficultyB = -0.5f,
            cefrTier = "A1-A2",
            questionPrompt = "Nhấn nút micro và luyện nói câu giao tiếp sau thật to, rõ ràng 🎙️",
            speakingSentence = "Good morning, how are you doing today?",
            options = listOf("Good morning, how are you doing today?"),
            correctIndex = 0,
            explanation = "Kiểm tra ngữ điệu mở đầu và trọng âm câu chào cơ bản.",
            skillTag = "Spoken_Greeting_Fluency"
        ),
        AdaptiveDiagnosticItem(
            id = 402,
            skillType = DiagnosticSkillType.SPEAKING,
            difficultyB = 0.4f,
            cefrTier = "B1-B2",
            questionPrompt = "Luyện nói câu hỏi lịch sự chuẩn ngữ điệu bản xứ 🎙️",
            speakingSentence = "Could you please tell me how to get to the station?",
            options = listOf("Could you please tell me how to get to the station?"),
            correctIndex = 0,
            explanation = "Kiểm tra ngữ điệu lên giọng cuối câu hỏi Yes/No và nối âm 'tell me how to'.",
            skillTag = "Spoken_Polite_Inquiry"
        ),
        AdaptiveDiagnosticItem(
            id = 403,
            skillType = DiagnosticSkillType.SPEAKING,
            difficultyB = 1.2f,
            cefrTier = "B2-C1",
            questionPrompt = "Luyện phát âm câu danh ngôn triết lý với nhịp điệu trôi chảy 🎙️",
            speakingSentence = "Practice makes perfect if you stay consistent every single day.",
            options = listOf("Practice makes perfect if you stay consistent every single day."),
            correctIndex = 0,
            explanation = "Kiểm tra nhịp điệu trọng âm (Stress-timed rhythm) và phát âm âm đuôi /ts/, /st/.",
            skillTag = "Spoken_Rhythm_Connected"
        )
    )

    /**
     * Lựa chọn câu hỏi tiếp theo dựa trên Năng lực hiện tại (Current Theta) và Kỹ năng cần đo
     */
    fun selectNextItem(
        stage: Int, // 1: Grammar, 2: Vocab, 3: Listening, 4: Speaking
        currentTheta: Float,
        answeredIds: Set<Int>
    ): AdaptiveDiagnosticItem {
        val targetSkill = when (stage) {
            1 -> DiagnosticSkillType.GRAMMAR
            2 -> DiagnosticSkillType.VOCABULARY
            3 -> DiagnosticSkillType.LISTENING
            else -> DiagnosticSkillType.SPEAKING
        }

        val candidates = itemBank.filter { it.skillType == targetSkill && it.id !in answeredIds }
        if (candidates.isEmpty()) return itemBank.first()

        // Chọn item có difficultyB gần nhất với currentTheta (Nguyên lý thông tin Fisher cực đại)
        return candidates.minByOrNull { kotlin.math.abs(it.difficultyB - currentTheta) } ?: candidates.first()
    }

    /**
     * Cập nhật tham số năng lực Theta theo mô hình Rasch xấp xỉ bậc nhất
     */
    fun updateTheta(
        currentTheta: Float,
        itemDifficulty: Float,
        isCorrect: Boolean,
        latencyMs: Long
    ): Float {
        // Trọng số điều chỉnh (Step size)
        val step = 0.6f

        // Xác suất trả lời đúng lý thuyết P = 1 / (1 + exp(-(theta - b)))
        val p = 1.0f / (1.0f + kotlin.math.exp(-(currentTheta - itemDifficulty)))
        val response = if (isCorrect) 1.0f else 0.0f

        // Điều chỉnh tốc độ phản xạ: Nếu trả lời đúng rất nhanh (<2.5s) -> cộng thêm bonus
        val speedFactor = if (isCorrect && latencyMs < 2500L) 1.15f else 1.0f

        val delta = step * (response - p) * speedFactor
        return (currentTheta + delta).coerceIn(-2.0f, 2.0f)
    }

    /**
     * Đánh giá tổng hợp và xuất bản Vector Hồ sơ Năng lực (Learner Persona Profile)
     */
    fun evaluateFinalProfile(
        userId: Long,
        goal: String,
        dailyTargetMinutes: Int,
        selfEfficacy: String,
        priorityModality: String,
        finalTheta: Float,
        answers: List<DiagnosticAnswerRecord>
    ): LearnerPersonaProfile {
        val evaluatedCefr = when {
            finalTheta < -0.3f -> "Beginner"
            finalTheta < 0.7f -> "Intermediate"
            else -> "Advanced"
        }

        // Tính điểm phần trăm từng kỹ năng
        val grammarCorrect = answers.filter { it.item.skillType == DiagnosticSkillType.GRAMMAR && it.isCorrect }.size
        val grammarTotal = max(1, answers.count { it.item.skillType == DiagnosticSkillType.GRAMMAR })
        val grammarAcc = (grammarCorrect * 100) / grammarTotal

        val vocabCorrect = answers.filter { it.item.skillType == DiagnosticSkillType.VOCABULARY && it.isCorrect }.size
        val vocabTotal = max(1, answers.count { it.item.skillType == DiagnosticSkillType.VOCABULARY })
        val vocabAcc = (vocabCorrect * 100) / vocabTotal

        val acousticCorrect = answers.filter { it.item.skillType == DiagnosticSkillType.LISTENING && it.isCorrect }.size
        val acousticTotal = max(1, answers.count { it.item.skillType == DiagnosticSkillType.LISTENING })
        val acousticAcc = (acousticCorrect * 100) / acousticTotal

        val speakingCorrect = answers.filter { it.item.skillType == DiagnosticSkillType.SPEAKING && it.isCorrect }.size
        val speakingTotal = max(1, answers.count { it.item.skillType == DiagnosticSkillType.SPEAKING })
        val speakingAcc = (speakingCorrect * 100) / speakingTotal

        // Thu thập các lỗ hổng kiến thức ban đầu
        val weakPoints = answers.filter { !it.isCorrect || it.latencyMs > 8000L }.map { it.item.skillTag }

        // Khởi tạo EF cho SM-2 thích ứng: Người có theta cao -> EF ban đầu cao hơn
        val initialEf = (2.5f + (finalTheta * 0.1f)).coerceIn(2.3f, 2.7f)

        return LearnerPersonaProfile(
            userId = userId,
            goal = goal,
            dailyTargetMinutes = dailyTargetMinutes,
            selfEfficacy = selfEfficacy,
            priorityModality = priorityModality,
            evaluatedCefr = evaluatedCefr,
            thetaAbility = finalTheta,
            grammarAccuracy = grammarAcc,
            vocabularyAccuracy = vocabAcc,
            acousticAccuracy = acousticAcc,
            speakingAccuracy = speakingAcc,
            initialEasinessFactor = initialEf,
            baselineWeakPoints = weakPoints,
            completedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        )
    }
}

data class DiagnosticAnswerRecord(
    val item: AdaptiveDiagnosticItem,
    val selectedIndex: Int,
    val isCorrect: Boolean,
    val latencyMs: Long,
    val speakingScore: Int = 0
)
