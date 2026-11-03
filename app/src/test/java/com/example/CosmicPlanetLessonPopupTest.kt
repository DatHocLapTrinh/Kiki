package com.example

import com.example.ui.cleanLessonTitle
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
}

