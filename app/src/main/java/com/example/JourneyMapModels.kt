package com.example

enum class JourneyStageState {
    COMPLETED,
    CURRENT,
    AVAILABLE,
    LOCKED
}

enum class PlanetVisualType {
    CYAN,
    PURPLE,
    BLUE,
    ORANGE,
    MASTER
}

data class JourneyStageUiModel(
    val id: String,
    val sequence: Int,
    val title: String,
    val description: String,
    val completedLessons: Int,
    val totalLessons: Int,
    val progress: Float,
    val state: JourneyStageState,
    val visualType: PlanetVisualType,
    val destinationId: String?,
    val rewardStars: Int,
    val isRecommended: Boolean
)

sealed interface JourneyMapUiState {
    data object Loading : JourneyMapUiState

    data class Success(
        val userName: String,
        val stages: List<JourneyStageUiModel>,
        val completedLessons: Int,
        val totalLessons: Int,
        val overallProgress: Float,
        val earnedStars: Int,
        val totalXp: Int,
        val rewardUnlocked: Boolean,
        val rewardClaimed: Boolean,
        val isOffline: Boolean = false,
        val isUsingCachedData: Boolean = false
    ) : JourneyMapUiState

    data object Empty : JourneyMapUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : JourneyMapUiState
}
