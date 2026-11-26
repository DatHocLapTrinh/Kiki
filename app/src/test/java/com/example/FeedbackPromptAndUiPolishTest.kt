package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.FullscreenOverlayType
import com.example.util.FeedbackPromptManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeedbackPromptAndUiPolishTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun defaultSurveyUrlMatchesExpectedGoogleFormHeaderUrl() {
        val expectedUrl =
            "https://docs.google.com/forms/d/e/1FAIpQLSfhMLfox0eDKY62pfJ8SvRxvFQ3ymiPVneEK1E-_Lo8ANyDhA/viewform?usp=header"
        assertEquals(expectedUrl, FeedbackPromptManager.DEFAULT_SURVEY_URL)
        assertEquals(expectedUrl, FeedbackPromptManager.getSurveyUrl(context))
    }

    @Test
    fun openSurveyInBrowserRejectsBlankAndNonHttpUrlsWithoutThrowing() {
        assertFalse(FeedbackPromptManager.openSurveyInBrowser(context, ""))
        assertFalse(FeedbackPromptManager.openSurveyInBrowser(context, "   "))
        assertFalse(FeedbackPromptManager.openSurveyInBrowser(context, "javascript:alert(1)"))
    }

    @Test
    fun fullscreenOverlayTypesCoverVaultAndClinic() {
        val types = FullscreenOverlayType.entries
        assertTrue(types.contains(FullscreenOverlayType.VOCABULARY_VAULT))
        assertTrue(types.contains(FullscreenOverlayType.WEAK_POINTS_CLINIC))
    }
}
