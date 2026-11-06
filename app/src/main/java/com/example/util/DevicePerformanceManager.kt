package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class DevicePerformanceTier {
    LOW_END,     // Thiết bị RAM yếu (<= 3.2GB), CPU <= 4 nhân, Android Go hoặc đang bật Tiết kiệm pin
    MID_RANGE,   // Thiết bị phổ thông (4GB - 6GB RAM, 6-8 nhân)
    FLAGSHIP     // Thiết bị cao cấp (> 6GB RAM, tần số quét 90/120Hz)
}

data class PerformanceProfile(
    val tier: DevicePerformanceTier,
    val isPowerSaveMode: Boolean,
    val isLowRamDevice: Boolean,
    val allowContinuousBackgroundAnimations: Boolean,
    val allowFullNebulaShader: Boolean,
    val allowHighResolutionParticles: Boolean,
    val maxConcurrentAnimations: Int
)

/**
 * Quản lý thích ứng hiệu năng theo cấu hình phần cứng thiết bị và chế độ nguồn điện (Kiki Adaptive Performance Engine).
 * Giúp app chạy mượt mà, ổn định trên cả các máy Android đời cũ, RAM yếu (2GB/3GB) và tiết kiệm tối đa pin.
 */
object DevicePerformanceManager {
    private const val PREFS_NAME = "kiki_performance_prefs"
    private const val KEY_PERF_MODE = "perf_mode" // "AUTO", "BATTERY_SAVER_LOW_END", "HIGH_FIDELITY"

    @Volatile
    private var cachedProfile: PerformanceProfile? = null

    fun getPerformanceProfile(context: Context, forceRefresh: Boolean = false): PerformanceProfile {
        if (!forceRefresh && cachedProfile != null) {
            return cachedProfile!!
        }

        val appContext = context.applicationContext
        val actManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

        val isPowerSave = powerManager?.isPowerSaveMode == true
        val isLowRam = actManager?.isLowRamDevice == true

        var totalRamBytes = 0L
        if (actManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            totalRamBytes = memInfo.totalMem
        }

        val totalRamGb = totalRamBytes / (1024.0 * 1024.0 * 1024.0)
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userPref = prefs.getString(KEY_PERF_MODE, "AUTO") ?: "AUTO"

        val tier = when (userPref) {
            "BATTERY_SAVER_LOW_END" -> DevicePerformanceTier.LOW_END
            "HIGH_FIDELITY" -> if (isLowRam || isPowerSave) DevicePerformanceTier.MID_RANGE else DevicePerformanceTier.FLAGSHIP
            else -> {
                // Tự động phân loại thông minh theo phần cứng và tình trạng pin
                if (isPowerSave || isLowRam || (totalRamGb in 0.1..3.3) || cpuCores <= 4) {
                    DevicePerformanceTier.LOW_END
                } else if (totalRamGb < 5.8) {
                    DevicePerformanceTier.MID_RANGE
                } else {
                    DevicePerformanceTier.FLAGSHIP
                }
            }
        }

        val isLow = tier == DevicePerformanceTier.LOW_END

        val profile = PerformanceProfile(
            tier = tier,
            isPowerSaveMode = isPowerSave,
            isLowRamDevice = isLowRam,
            allowContinuousBackgroundAnimations = !isLow,
            allowFullNebulaShader = !isLow,
            allowHighResolutionParticles = !isLow,
            maxConcurrentAnimations = if (isLow) 1 else 5
        )

        cachedProfile = profile
        return profile
    }

    fun isLowEnd(context: Context): Boolean {
        return getPerformanceProfile(context).tier == DevicePerformanceTier.LOW_END
    }

    fun setPerformanceMode(context: Context, mode: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PERF_MODE, mode).apply()
        cachedProfile = null
    }

    fun getPerformanceMode(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PERF_MODE, "AUTO") ?: "AUTO"
    }
}

val LocalPerformanceProfile = staticCompositionLocalOf {
    PerformanceProfile(
        tier = DevicePerformanceTier.FLAGSHIP,
        isPowerSaveMode = false,
        isLowRamDevice = false,
        allowContinuousBackgroundAnimations = true,
        allowFullNebulaShader = true,
        allowHighResolutionParticles = true,
        maxConcurrentAnimations = 5
    )
}

