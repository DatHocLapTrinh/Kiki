package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.repeatCount
import com.example.util.DevicePerformanceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyMentorApplication : Application(), ImageLoaderFactory {

    private var imageLoaderInstance: ImageLoader? = null

    override fun newImageLoader(): ImageLoader {
        val isLowEnd = DevicePerformanceManager.isLowEnd(this)

        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add { chain ->
                    val request = chain.request
                    val modifiedRequest = request.newBuilder()
                        .repeatCount(0)
                        .build()
                    chain.proceed(modifiedRequest)
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // Trên máy yếu: giới hạn 12% heap để triệt tiêu nguy cơ OOM, máy mạnh: 25%
                    .maxSizePercent(if (isLowEnd) 0.12 else 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(if (isLowEnd) 30L * 1024 * 1024 else 120L * 1024 * 1024)
                    .build()
            }
            // Trên máy RAM yếu: Cho phép nén bitmap về RGB_565 (giảm 50% RAM/pixel, từ 4 bytes xuống 2 bytes)
            .allowRgb565(isLowEnd)
            .crossfade(!isLowEnd) // Tắt crossfade trên máy yếu để giảm GPU overdraw
            .build().also {
                imageLoaderInstance = it
            }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Khi hệ điều hành Android cảnh báo RAM căng thẳng, chủ động dọn dẹp bộ nhớ đệm
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            imageLoaderInstance?.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageLoaderInstance?.memoryCache?.clear()
    }
}
