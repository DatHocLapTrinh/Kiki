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
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class StudyMentorApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        preloadMascotAnimations()
    }

    override fun newImageLoader(): ImageLoader {
        // OkHttpClient với Interceptor tối ưu hóa GIF:
        // Tự động quét và vá delay frame 0 của GIF từ 240 (2.4s) xuống 4 (40ms),
        // loại bỏ hoàn toàn hiện tượng đơ giật 5 giây khi bắt đầu hoạt ảnh!
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val body = response.body
                val url = request.url.toString()
                if (response.isSuccessful && body != null && url.contains(".gif", ignoreCase = true)) {
                    val bytes = body.bytes()
                    val scanLimit = (bytes.size - 6).coerceAtMost(2048)
                    for (i in 0 until scanLimit) {
                        if ((bytes[i].toInt() and 0xFF) == 0x21 &&
                            (bytes[i + 1].toInt() and 0xFF) == 0xF9 &&
                            (bytes[i + 2].toInt() and 0xFF) == 0x04
                        ) {
                            val delay = (bytes[i + 4].toInt() and 0xFF) or ((bytes[i + 5].toInt() and 0xFF) shl 8)
                            if (delay > 10) {
                                bytes[i + 4] = 4.toByte() // 40ms = 25fps mượt mà
                                bytes[i + 5] = 0.toByte()
                                break // Chỉ can thiệp frame 0 mở đầu
                            }
                        }
                    }
                    val newBody = bytes.toResponseBody(body.contentType())
                    response.newBuilder().body(newBody).build()
                } else {
                    response
                }
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
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
