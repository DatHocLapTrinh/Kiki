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
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your_groq_api_key_here")) {
            return apiKey.trim();
        }
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        return "";
    }

    public String generateChatCompletion(String systemPrompt, String userPrompt) throws IOException {
        String activeKey = resolveApiKey();
        if (activeKey.isEmpty()) {
            log.warn("GROQ_API_KEY is not set. Generating mock response for local testing.");
            return generateMockResponse(systemPrompt, userPrompt);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        );
        requestBody.put("messages", messages);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer " + activeKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("Groq API error ({}) : {}", response.code(), errorBody);
                throw new IOException("Groq API error: " + response.code() + " - " + errorBody);
            }

            String responseString = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseString);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }

            throw new IOException("Malformed Groq API response: choices not found");
        }
    }

    private String generateMockResponse(String systemPrompt, String userPrompt) {
        if ((systemPrompt != null && systemPrompt.contains("Mentor")) ||
            (userPrompt != null && userPrompt.contains("Student question"))) {
            return """
            {
              "reply": "Chào bạn! Mình là Kiki AI Mentor. Để học tiếng Anh hiệu quả, hãy luyện tập thường xuyên với các ví dụ thực tế.",
              "grammarTip": "Mỗi ngày ghi nhớ 1 cấu trúc ngữ pháp sẽ giúp phản xạ nói của bạn tự nhiên hơn.",
              "detectedVocabularies": [
                {
                  "word": "persistent",
                  "phonetic": "/pərˈsɪstənt/",
                  "meaning": "kiên trì, bền bỉ",
                  "example": "Be persistent in practicing English every day."
                }
              ]
            }
            """;
        }

        return """
        {
          "title": "Giao tiếp hàng ngày cơ bản",
          "description": "Chương học mẫu được tạo tự động khi chưa cấu hình GROQ_API_KEY.",
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
