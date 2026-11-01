package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.Coil
import coil.request.ImageRequest
import com.example.model.KikiAnimationUrls
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyMentorApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        preloadMascotAnimations()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    private fun preloadMascotAnimations() {
        val loader = Coil.imageLoader(this)
        val urls = listOf(
            KikiAnimationUrls.GREETING,
            KikiAnimationUrls.READING_QUESTION,
            KikiAnimationUrls.CLAP_NORMAL,
            KikiAnimationUrls.CLAP_STREAK,
            KikiAnimationUrls.SAD
        )
        urls.forEach { url ->
            val request = ImageRequest.Builder(this)
                .data(url)
                .build()
            loader.enqueue(request)
        }
    }
}
