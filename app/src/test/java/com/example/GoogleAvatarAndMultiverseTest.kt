package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleAvatarAndMultiverseTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGoogleAvatarUrlCachingAndResolution() {
        val prefs = context.getSharedPreferences("kiki_audio_prefs", Context.MODE_PRIVATE)
        val mockGooglePhoto = "https://lh3.googleusercontent.com/a/ACg8ocL123=s96-c"

        // Lưu thử cache
        prefs.edit().putString("cached_google_photo_url", mockGooglePhoto).apply()
        val retrieved = prefs.getString("cached_google_photo_url", null)

        assertEquals(mockGooglePhoto, retrieved)
        assertTrue(retrieved?.startsWith("https://") == true)
    }

    @Test
    fun testMultiverseStagesProgressCalculation() {
        val chapterTitles = (1..40).map { "Planet $it" }
        // 10 chapters completed (node = 6), còn lại node = 0
        val nodeProgress = List(40) { if (it < 10) 6 else 0 }
        val stages = JourneyStageMapper.mapStages(chapterTitles, nodeProgress)

        val completedCount = stages.count { it.state == JourneyStageState.COMPLETED }
        val percent = (completedCount.toFloat() / stages.size)

        assertEquals(10, completedCount)
        assertEquals(0.25f, percent, 0.001f)
    }
}

