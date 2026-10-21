package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Mô hình dữ liệu Hồ sơ Cá nhân hóa & Năng lực Học viên (Learner Persona Profile)
 * Thu thập qua quy trình Khảo sát (Intake) và Bài kiểm tra thích ứng (Adaptive Diagnostic Test - CAT).
 */
@JsonClass(generateAdapter = true)
data class LearnerPersonaProfile(
    @param:Json(name = "userId") val userId: Long = 0L,
    @param:Json(name = "goal") val goal: String = "Everyday Fluency",
    @param:Json(name = "dailyTargetMinutes") val dailyTargetMinutes: Int = 5,
    @param:Json(name = "selfEfficacy") val selfEfficacy: String = "ELEMENTARY",
    @param:Json(name = "priorityModality") val priorityModality: String = "AUDIO_DIALOG",
    @param:Json(name = "evaluatedCefr") val evaluatedCefr: String = "Intermediate",
    @param:Json(name = "thetaAbility") val thetaAbility: Float = 0.0f,
    @param:Json(name = "grammarAccuracy") val grammarAccuracy: Int = 75,
    @param:Json(name = "vocabularyAccuracy") val vocabularyAccuracy: Int = 75,
    @param:Json(name = "acousticAccuracy") val acousticAccuracy: Int = 75,
    @param:Json(name = "speakingAccuracy") val speakingAccuracy: Int = 75,
    @param:Json(name = "initialEasinessFactor") val initialEasinessFactor: Float = 2.5f,
    @param:Json(name = "baselineWeakPoints") val baselineWeakPoints: List<String> = emptyList(),
    @param:Json(name = "completedAt") val completedAt: String = ""
)

enum class DiagnosticSkillType {
    GRAMMAR,
    VOCABULARY,
    LISTENING,
    SPEAKING
}

data class AdaptiveDiagnosticItem(
    val id: Int,
    val skillType: DiagnosticSkillType,
    val difficultyB: Float, // Tham số độ khó b trong mô hình Rasch/IRT (-1.2 .. +1.2)
    val cefrTier: String,   // A1-A2, B1-B2, B2-C1
    val questionPrompt: String,
    val audioSentence: String = "",
    val speakingSentence: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val skillTag: String = ""
)

data class IntakeGoalOption(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val icon: String,
    val descriptionVi: String,
    val descriptionEn: String
)

data class IntakePacingOption(
    val minutes: Int,
    val titleVi: String,
    val titleEn: String,
    val icon: String,
    val descriptionVi: String,
    val descriptionEn: String
)

data class IntakeSelfEfficacyOption(
    val id: String,
    val initialTheta: Float,
    val titleVi: String,
    val titleEn: String,
    val icon: String,
    val subtitleVi: String,
    val subtitleEn: String
)

data class IntakeModalityOption(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val icon: String,
    val descVi: String,
    val descEn: String
)
