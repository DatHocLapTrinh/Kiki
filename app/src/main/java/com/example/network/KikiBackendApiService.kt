package com.example.network

import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface KikiBackendApiService {

    @POST("api/v1/ai/generate-chapter")
    suspend fun generateChapter(
        @Header("Authorization") authHeader: String,
        @Body request: GenerateChapterRequest
    ): Response<ChapterGeneratedResponse>

    @POST("api/v1/ai/mentor-chat")
    suspend fun mentorChat(
        @Header("Authorization") authHeader: String,
        @Body request: MentorChatRequest
    ): Response<MentorChatResponse>

    companion object {
        // 10.0.2.2 trỏ về localhost của máy tính chạy backend khi dùng Android Emulator
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"

        fun create(baseUrl: String = DEFAULT_BASE_URL): KikiBackendApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(KikiBackendApiService::class.java)
        }
    }
}
