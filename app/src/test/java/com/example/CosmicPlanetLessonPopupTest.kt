package com.example

import com.example.ui.cleanLessonTitle
import com.example.ui.formatLessonSubtitle
import org.junit.Assert.assertEquals
import org.junit.Test

class CosmicPlanetLessonPopupTest {

    @Test
    fun testCleanLessonTitle_stripsBilingualParenthesesAndDuplicateChapterPrefix() {
        val raw1 = "Chặng 1: Sao Thủy - Chào Hỏi & Làm Quen Cơ Bản (Mercury: Greetings & Hello)"
        assertEquals("Chào Hỏi & Làm Quen Cơ Bản", cleanLessonTitle(raw1))

        val raw2 = "Chặng 1: Chào Hỏi & Làm Quen Cơ Bản (Hello & Daily Greetings)"
        assertEquals("Chào Hỏi & Làm Quen Cơ Bản", cleanLessonTitle(raw2))

        val raw3 = "Chặng 1: Củng Cố Cấu Trúc Cốt Lõi (Core Syntax)"
        assertEquals("Củng Cố Cấu Trúc Cốt Lõi", cleanLessonTitle(raw3))

        val raw4 = "Chặng 1: Giao Tiếp Công Sở & Email Chuẩn Mực"
        assertEquals("Giao Tiếp Công Sở & Email Chuẩn Mực", cleanLessonTitle(raw4))

        val raw5 = "Sử dụng thì hiện tại"
        assertEquals("Sử dụng thì hiện tại", cleanLessonTitle(raw5))
    }

    @Test
    fun testFormatLessonSubtitle_updatesAccordingToCompletedLessons() {
        // Chưa làm bài nào (completed = 0) -> Bài học 1 / 5
        assertEquals("Bài học 1 / 5", formatLessonSubtitle(completedLessons = 0, totalLessons = 5, isLocked = false, isCompleted = false))
        
        // Đã hoàn thành 3 bài học -> Phải hiển thị Bài học 4 / 5 (không bị kẹt ở Bài học 1 / 5)
        assertEquals("Bài học 4 / 5", formatLessonSubtitle(completedLessons = 3, totalLessons = 5, isLocked = false, isCompleted = false))
        
        // Đã hoàn thành toàn bộ chặng -> Đã hoàn thành (5/5)
        assertEquals("Đã hoàn thành (5/5)", formatLessonSubtitle(completedLessons = 5, totalLessons = 5, isLocked = false, isCompleted = true))

        // Chặng bị khóa -> Chưa mở khóa
        assertEquals("Chưa mở khóa", formatLessonSubtitle(completedLessons = 0, totalLessons = 5, isLocked = true, isCompleted = false))

        // Tiếng Anh
        assertEquals("Lesson 4 / 5", formatLessonSubtitle(completedLessons = 3, totalLessons = 5, isLocked = false, isCompleted = false, isEnglish = true))
    }
}

