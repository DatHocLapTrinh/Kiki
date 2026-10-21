package com.example.model

data class ChapterUnlockEvent(
    val completedChapterTitle: String,
    val nextChapterTitle: String,
    val isAiGenerated: Boolean = false,
    val weakPointsAddressedCount: Int = 0
)
