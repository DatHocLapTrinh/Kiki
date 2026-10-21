package com.kiki.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GroqClientService {

    private static final Logger log = LoggerFactory.getLogger(GroqClientService.class);

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String baseUrl;

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String model;

    @Value("${groq.temperature:0.7}")
    private double temperature;

    @Value("${groq.max-tokens:2500}")
    private int maxTokens;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroqClientService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build();
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your_")) {
            return apiKey.trim();
        }
        String[] envVars = {"GEMINI_API_KEY", "AI_API_KEY", "GROQ_API_KEY", "OPENAI_API_KEY", "ANTHROPIC_API_KEY"};
        for (String varName : envVars) {
            String val = System.getenv(varName);
            if (val != null && !val.trim().isEmpty() && !val.contains("your_")) {
                return val.trim();
            }
        }
        return "";
    }

    public String generateChatCompletion(String systemPrompt, String userPrompt) throws IOException {
        String activeKey = resolveApiKey();
        if (activeKey.isEmpty()) {
            log.warn("No AI API key configured (GEMINI_API_KEY / AI_API_KEY / GROQ_API_KEY). Using high-quality mock response.");
            return generateMockResponse(systemPrompt, userPrompt);
        }

        try {
            // Tự động nhận diện nhà cung cấp dựa trên tiền tố của API key (ưu tiên Gemini 2.5 Flash)
            if (activeKey.startsWith("AQ.") || activeKey.startsWith("AIza") || System.getenv("GEMINI_API_KEY") != null) {
                log.info("Detected Google Gemini API key. Routing to Gemini GenerativeLanguage API (Gemini 2.5 Flash Native Dialog).");
                return callGemini(activeKey, systemPrompt, userPrompt);
            } else if (activeKey.startsWith("sk-ant-")) {
                log.info("Detected Anthropic Claude API key. Routing to Anthropic Messages API.");
                return callClaude(activeKey, systemPrompt, userPrompt);
            } else if (activeKey.startsWith("sk-")) {
                log.info("Detected OpenAI API key. Routing to OpenAI Chat Completions API.");
                return callOpenAiCompatible("https://api.openai.com/v1/chat/completions", activeKey, "gpt-4o-mini", systemPrompt, userPrompt);
            } else {
                // Mặc định: Groq AI (Llama 3 / OSS)
                log.info("Detected Groq / OpenAI-compatible API key. Routing to Groq API.");
                return callOpenAiCompatible(baseUrl, activeKey, model, systemPrompt, userPrompt);
            }
        } catch (Exception e) {
            log.warn("AI generation failed with live provider: {}. Falling back to structured mock response.", e.getMessage());
            return generateMockResponse(systemPrompt, userPrompt);
        }
    }

    private String callOpenAiCompatible(String url, String key, String modelName, String systemPrompt, String userPrompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        );
        requestBody.put("messages", messages);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + key)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("OpenAI/Groq API error (" + response.code() + "): " + errorBody);
            }

            String responseString = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseString);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new IOException("Malformed OpenAI/Groq response: choices empty");
        }
    }

    private String callGemini(String key, String systemPrompt, String userPrompt) throws IOException {
        String[] candidateModels = {"gemini-2.5-flash", "gemini-flash-lite-latest", "gemini-2.0-flash"};
        String combined = (systemPrompt != null && !systemPrompt.isEmpty() ? systemPrompt + "\n\n" : "") + userPrompt;

        Map<String, Object> part = Map.of("text", combined);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        String jsonPayload = objectMapper.writeValueAsString(body);
        IOException lastException = null;

        for (String modelName : candidateModels) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + key;
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    lastException = new IOException("Gemini API error (" + response.code() + ") on " + modelName + ": " + errorBody);
                    if (response.code() == 503 || response.code() == 429) {
                        continue;
                    }
                    throw lastException;
                }

                String responseString = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseString);
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        return parts.get(0).path("text").asText();
                    }
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new IOException("Malformed Gemini response: parts empty");
    }

    private String callClaude(String key, String systemPrompt, String userPrompt) throws IOException {
        String url = "https://api.anthropic.com/v1/messages";

        Map<String, Object> userMessage = Map.of("role", "user", "content", userPrompt);
        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-3-5-sonnet-20241022");
        body.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(userMessage));

        String jsonPayload = objectMapper.writeValueAsString(body);

        Request request = new Request.Builder()
            .url(url)
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Claude API error (" + response.code() + "): " + errorBody);
            }

            String responseString = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseString);
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText();
            }
            throw new IOException("Malformed Claude response: content empty");
        }
    }

    private String generateMockResponse(String systemPrompt, String userPrompt) {
        if ((systemPrompt != null && systemPrompt.contains("Mentor")) ||
            (userPrompt != null && userPrompt.contains("Student question"))) {
            return """
            {
              "reply": "Resilient /rɪˈzɪliənt/ (adj)\\n• Nghĩa: Có khả năng phục hồi nhanh chóng sau khó khăn, kiên cường, bền bỉ.\\n• Ví dụ: She is resilient and bounced back quickly after the setback. (Cô ấy rất kiên cường và đã phục hồi nhanh chóng sau thất bại.)\\n• Từ đồng nghĩa: adaptable, flexible, tough, hardy\\n• Từ trái nghĩa: fragile, vulnerable, weak",
              "grammarTip": "Ghi nhớ từ vựng cùng từ đồng nghĩa và trái nghĩa giúp tăng tốc phản xạ ngôn ngữ.",
              "detectedVocabularies": [
                {
                  "word": "resilient",
                  "phonetic": "/rɪˈzɪliənt/",
                  "meaning": "kiên cường, có khả năng phục hồi nhanh",
                  "example": "She is resilient and bounced back quickly after the setback."
                }
              ]
            }
            """;
        }

        return """
        {
          "title": "Giao tiếp hàng ngày cơ bản",
          "description": "Chương học mẫu được tạo tự động khi chưa cấu hình AI API key.",
          "level": "A1",
          "subject": "Daily Communication",
          "orderNo": 1,
          "questions": [
            {
              "questionText": "How are you today?",
              "questionType": "MULTIPLE_CHOICE",
              "optionsJson": "[\\"I'm doing great, thanks!\\", \\"I am student\\", \\"Yes, please\\", \\"See you\\"]",
              "correctAnswer": "0",
              "explanation": "'I'm doing great' là cách trả lời tự nhiên nhất cho câu hỏi hỏi thăm sức khỏe.",
              "points": 10,
              "difficulty": "EASY"
            },
            {
              "questionText": "Sắp xếp các từ thành câu hoàn chỉnh: have / I / nice / a / day",
              "questionType": "SENTENCE_BUILDER",
              "optionsJson": "[\\"have\\", \\"I\\", \\"nice\\", \\"a\\", \\"day\\"]",
              "correctAnswer": "Have a nice day",
              "explanation": "Câu chúc lịch sự phổ biến: Have a nice day!",
              "points": 15,
              "difficulty": "MEDIUM"
            }
          ]
        }
        """;
    }
}
