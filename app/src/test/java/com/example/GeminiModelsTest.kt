package com.example

import com.example.model.GeminiModels
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GeminiModelsTest {

    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testGenerateContentRequestSerialization_TextOnly() {
        val request = GeminiModels.GenerateContentRequest(
            contents = listOf(GeminiModels.createTextContent("user", "Hello Gemini 2.5 Flash")),
            systemInstruction = GeminiModels.createSystemInstruction("You are Kiki AI mentor"),
            generationConfig = GeminiModels.GenerationConfig(temperature = 0.7, maxOutputTokens = 1024)
        )

        val adapter = moshi.adapter(GeminiModels.GenerateContentRequest::class.java)
        val json = adapter.toJson(request)

        assertNotNull(json)
        assertTrue(json.contains("Hello Gemini 2.5 Flash"))
        assertTrue(json.contains("You are Kiki AI mentor"))
        assertTrue(json.contains("temperature"))
    }

    @Test
    fun testGenerateContentRequestSerialization_MultimodalVisionAndAudio() {
        val visionContent = GeminiModels.createVisionContent("user", "Look at this quiz", "base64JpegDataHere")
        val audioContent = GeminiModels.createAudioContent("user", "Spoken text", "base64WavDataHere", "audio/wav")

        val request = GeminiModels.GenerateContentRequest(
            contents = listOf(visionContent, audioContent)
        )

        val adapter = moshi.adapter(GeminiModels.GenerateContentRequest::class.java)
        val json = adapter.toJson(request)

        assertNotNull(json)
        assertTrue(json.contains("image/jpeg"))
        assertTrue(json.contains("base64JpegDataHere"))
        assertTrue(json.contains("audio/wav"))
        assertTrue(json.contains("base64WavDataHere"))
    }

    @Test
    fun testGenerateContentResponseDeserialization() {
        val sampleResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "role": "model",
                    "parts": [
                      {
                        "text": "[STEP] Here is the detailed explanation for question 1.\n[STEP] Next step..."
                      }
                    ]
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(GeminiModels.GenerateContentResponse::class.java)
        val response = adapter.fromJson(sampleResponseJson)

        assertNotNull(response)
        assertEquals(1, response?.candidates?.size)
        val candidate = response?.candidates?.first()
        assertEquals("model", candidate?.content?.role)
        assertEquals("STOP", candidate?.finishReason)

        val text = GeminiModels.extractResponseText(response)
        assertNotNull(text)
        assertTrue(text!!.contains("[STEP] Here is the detailed explanation"))
    }

    @Test
    fun testDefaultModelsConstants() {
        assertEquals("gemini-2.5-flash", GeminiModels.MODEL_GEMINI_2_5_FLASH)
        assertTrue(GeminiModels.SUPPORTED_MODELS.contains("gemini-2.5-flash"))
        assertTrue(GeminiModels.SUPPORTED_MODELS.contains("gemini-3.5-flash"))
    }
}
