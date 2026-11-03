package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.repeatCount
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyMentorApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
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
            .crossfade(true)
            .build()
    }
}
