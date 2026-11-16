package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.DevicePerformanceManager
import com.example.util.DevicePerformanceTier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevicePerformanceManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset về mặc định
        DevicePerformanceManager.setPerformanceMode(context, "AUTO")
    }

    @Test
    fun testDefaultPerformanceProfileCreation() {
        val profile = DevicePerformanceManager.getPerformanceProfile(context, forceRefresh = true)
        assertNotNull(profile)
        assertNotNull(profile.tier)
    }

    @Test
    fun testUserOverrideLowEndMode() {
        DevicePerformanceManager.setPerformanceMode(context, "BATTERY_SAVER_LOW_END")
        val profile = DevicePerformanceManager.getPerformanceProfile(context, forceRefresh = true)

        assertEquals(DevicePerformanceTier.LOW_END, profile.tier)
        assertFalse(profile.allowContinuousBackgroundAnimations)
        assertFalse(profile.allowFullNebulaShader)
        assertEquals(1, profile.maxConcurrentAnimations)
        assertTrue(DevicePerformanceManager.isLowEnd(context))
    }

    @Test
    fun testUserOverrideHighFidelityMode() {
        DevicePerformanceManager.setPerformanceMode(context, "HIGH_FIDELITY")
        val profile = DevicePerformanceManager.getPerformanceProfile(context, forceRefresh = true)

        assertNotEquals(DevicePerformanceTier.LOW_END, profile.tier)
        assertTrue(profile.allowContinuousBackgroundAnimations)
        assertTrue(profile.allowFullNebulaShader)
        assertFalse(DevicePerformanceManager.isLowEnd(context))
    }
}

