package com.example

import kotlin.math.roundToInt

object JourneyStageMapper {
    const val LESSONS_PER_STAGE = 5
    const val CHAPTER_COMPLETION_BONUS_STARS = 3

    fun mapStages(
        chapterTitles: List<String>,
        nodeProgress: List<Int>
    ): List<JourneyStageUiModel> {
        var currentAssigned = false

        return chapterTitles.mapIndexed { index, rawTitle ->
            val node = nodeProgress.getOrNull(index)?.coerceIn(1, LESSONS_PER_STAGE + 1) ?: if (index == 0) 1 else 0
            val completedLessons = (node - 1).coerceIn(0, LESSONS_PER_STAGE)
            val isCompleted = node >= LESSONS_PER_STAGE + 1
            val previousCompleted = index == 0 || nodeProgress.getOrNull(index - 1)?.let {
                it >= LESSONS_PER_STAGE + 1
            } == true

            val state = when {
                isCompleted -> JourneyStageState.COMPLETED
                previousCompleted && !currentAssigned -> {
                    currentAssigned = true
                    JourneyStageState.CURRENT
                }
                previousCompleted -> JourneyStageState.AVAILABLE
                else -> JourneyStageState.LOCKED
            }

            val title = rawTitle.trim().ifEmpty { "Chapter ${index + 1}" }
            JourneyStageUiModel(
                id = title,
                sequence = index + 1,
                title = title,
                description = when (state) {
                    JourneyStageState.COMPLETED -> "Bạn đã hoàn thành chặng học này."
                    JourneyStageState.CURRENT -> "Tiếp tục bài học được đề xuất."
                    JourneyStageState.AVAILABLE -> "Chặng học đã sẵn sàng để khám phá."
                    JourneyStageState.LOCKED -> "Hoàn thành chặng trước để mở khóa."
                },
                completedLessons = completedLessons,
                totalLessons = LESSONS_PER_STAGE,
                progress = (completedLessons.toFloat() / LESSONS_PER_STAGE).coerceIn(0f, 1f),
                state = state,
                visualType = when (index % 5) {
                    0 -> PlanetVisualType.CYAN
                    1 -> PlanetVisualType.PURPLE
                    2 -> PlanetVisualType.BLUE
                    3 -> PlanetVisualType.ORANGE
                    else -> PlanetVisualType.MASTER
                },
                destinationId = title,
                rewardStars = CHAPTER_COMPLETION_BONUS_STARS,
                isRecommended = state == JourneyStageState.CURRENT
            )
        }
    }

    fun completedLessons(stages: List<JourneyStageUiModel>): Int =
        stages.sumOf { it.completedLessons }

    fun totalLessons(stages: List<JourneyStageUiModel>): Int =
        stages.sumOf { it.totalLessons }

    fun overallProgress(stages: List<JourneyStageUiModel>): Float {
        val total = totalLessons(stages)
        if (total == 0) return 0f
        return (completedLessons(stages).toFloat() / total).coerceIn(0f, 1f)
    }

    fun earnedStars(stages: List<JourneyStageUiModel>): Int =
        completedLessons(stages) + stages.count { it.state == JourneyStageState.COMPLETED } * CHAPTER_COMPLETION_BONUS_STARS

    fun percentage(progress: Float): Int = (progress.coerceIn(0f, 1f) * 100).roundToInt()

    fun rewardUnlocked(stages: List<JourneyStageUiModel>): Boolean =
        stages.isNotEmpty() && stages.all { it.state == JourneyStageState.COMPLETED }
}
