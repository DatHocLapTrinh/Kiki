package com.kiki.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiki.backend.dto.CosmicCallRequest;
import com.kiki.backend.dto.CosmicCallResponse;
import com.kiki.backend.service.CosmicCallService;
import com.kiki.backend.service.GroqClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CosmicCallServiceTest {

    private GroqClientService groqClientService;
    private ObjectMapper objectMapper;
    private CosmicCallService cosmicCallService;

    @BeforeEach
    void setUp() {
        groqClientService = Mockito.mock(GroqClientService.class);
        objectMapper = new ObjectMapper();
        cosmicCallService = new CosmicCallService(groqClientService, objectMapper);
    }

    @Test
    void testConverseAndEvaluate_SuccessWithVietnameseName() throws IOException {
        String mockAiJson = """
            {
              "score": 90,
              "spokenFeedbackEn": "Nice to meet you, Truong! It is great that you are from Hanoi.",
              "feedbackVi": "Rất tốt! Câu giới thiệu rõ ràng, tên tiếng Việt được giữ nguyên vẹn.",
              "grammarCorrection": "Hello, my name is Truong and I live in Hanoi.",
              "nextQuestionEn": "What do you like to do on the weekends in Hanoi?",
              "nextHintVi": "Chia sẻ về hoạt động bạn thích làm cuối tuần ở Hà Nội.",
              "suggestedStarterEn": "On the weekends, I enjoy..."
            }
            """;
        when(groqClientService.generateChatCompletion(anyString(), anyString())).thenReturn(mockAiJson);

        CosmicCallRequest request = new CosmicCallRequest(
            "Hello, my name is Truong and I live in Hanoi",
            "Can you introduce yourself?",
            "Mercury: Greetings",
            "A1 Beginner",
            Collections.emptyList()
        );

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);

        assertNotNull(response);
        assertEquals(90, response.score());
        assertTrue(response.spokenFeedbackEn().contains("Truong"));
        assertTrue(response.feedbackVi().contains("Rất tốt"));
        assertNotNull(response.nextQuestionEn());
        assertTrue(response.nextQuestionEn().contains("weekends"));
        assertEquals("On the weekends, I enjoy...", response.suggestedStarterEn());
    }

    @Test
    void testConverseAndEvaluate_ResilientFallbackWhenExceptionOccurs() throws IOException {
        when(groqClientService.generateChatCompletion(anyString(), anyString())).thenThrow(new IOException("Network timeout"));

        CosmicCallRequest request = new CosmicCallRequest(
            "My name is Truong and I like learning English with Kiki",
            "Can you introduce yourself?",
            "Earth: Daily Essentials",
            "A1 Beginner",
            Collections.emptyList()
        );

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);

        assertNotNull(response);
        assertTrue(response.score() >= 60 && response.score() <= 100);
        assertNotNull(response.spokenFeedbackEn());
        assertNotNull(response.feedbackVi());
        assertNotNull(response.nextQuestionEn());
        assertNotNull(response.suggestedStarterEn());
        assertFalse(response.suggestedStarterEn().isEmpty());
    }
}
