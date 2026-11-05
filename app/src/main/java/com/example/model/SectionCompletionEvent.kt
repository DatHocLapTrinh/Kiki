package com.example.model

/**
 * Sự kiện hoàn thành một Chặng lớn (1 Chặng = 10 hành tinh).
 * Kích hoạt khi người học vượt qua bài học cuối cùng của Hành tinh số 10, 20, hoặc 30.
 */
data class SectionCompletionEvent(
    val completedSectionNumber: Int,
    val nextSectionNumber: Int,
    val completedSectionTitle: String,
    val nextSectionTitle: String,
    val rewardXp: Int = 100,
    val rewardStars: Int = 50
)

