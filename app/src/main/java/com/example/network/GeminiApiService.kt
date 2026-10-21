package com.example.network

import com.example.model.GeminiModels
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model", encoded = true) model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiModels.GenerateContentRequest
    ): Response<GeminiModels.GenerateContentResponse>

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"

        fun create(): GeminiApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(GeminiApiService::class.java)
        }
    }
}
