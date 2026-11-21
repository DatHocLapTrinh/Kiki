package com.kiki.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiki.backend.dto.CosmicCallRequest;
import com.kiki.backend.dto.CosmicCallResponse;
import com.kiki.backend.service.CosmicCallScorer;
import com.kiki.backend.service.CosmicCallService;
import com.kiki.backend.service.GroqClientService;
import com.kiki.backend.service.VietnameseNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CosmicCallServiceTest {

    private GroqClientService groqClientService;
    private ObjectMapper objectMapper;
    private VietnameseNameNormalizer vietnameseNameNormalizer;
    private CosmicCallScorer cosmicCallScorer;
    private CosmicCallService cosmicCallService;

    @BeforeEach
    void setUp() {
        groqClientService = Mockito.mock(GroqClientService.class);
        objectMapper = new ObjectMapper();
        vietnameseNameNormalizer = new VietnameseNameNormalizer();
        cosmicCallScorer = new CosmicCallScorer();
        cosmicCallService = new CosmicCallService(groqClientService, objectMapper, vietnameseNameNormalizer, cosmicCallScorer);
    }

    @Test
    void testConverseAndEvaluate_EmptyUserAnswerReturnsZeroPointsImmediately() {
        CosmicCallRequest request = new CosmicCallRequest(
            "",
            "What is your name?",
            "Mercury: Greetings",
            "A1 Beginner",
            Collections.emptyList()
        );

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);

        assertNotNull(response);
        assertEquals(0, response.score());
        assertTrue(response.feedbackVi().contains("chưa nghe thấy"));

        // Không tốn gọi API AI khi người dùng chưa nói
        verifyNoInteractions(groqClientService);
    }

    @Test
    void testConverseAndEvaluate_WrongAnswerIsCappedAtLowScoreEvenIfAiReturned72() throws IOException {
        // Mô phỏng AI bị lỗi sinh ra điểm 72 cho câu trả lời sai/lạc đề
        String mockAiJson = """
            {
              "score": 72,
              "spokenFeedbackEn": "I heard you talking about weather.",
              "feedbackVi": "Câu trả lời bị lạc đề hoàn toàn.",
              "grammarCorrection": "My name is Truong.",
              "nextQuestionEn": "What is your name?",
              "nextHintVi": "Nói tên của bạn.",
              "suggestedStarterEn": "My name is..."
            }
            """;
        when(groqClientService.generateChatCompletion(anyString(), anyString())).thenReturn(mockAiJson);

        CosmicCallRequest request = new CosmicCallRequest(
            "the weather is rain coffee hotel",
            "What is your name and who are you?",
            "Mercury: Greetings",
            "A1 Beginner",
            Collections.emptyList(),
            "Truong",
            null
        );

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);

        assertNotNull(response);
        // Scorer hiệu chỉnh điểm: câu sai/lạc đề KHÔNG được chấm 72 mà phải bị hạ xuống <= 25 điểm
        assertTrue(response.score() <= 25, "Score must be <= 25 for off-topic answer, but was " + response.score());
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
            Collections.emptyList(),
            "Nguyễn Văn Trường",
            List.of("Hello, my name is Truong and I live in Hanoi")
        );

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);

        assertNotNull(response);
        assertEquals(90, response.score());
        assertTrue(response.spokenFeedbackEn().contains("Truong"));
        assertTrue(response.feedbackVi().contains("Rất tốt"));
        assertNotNull(response.nextQuestionEn());
        assertTrue(response.nextQuestionEn().contains("weekends"));
        assertEquals("On the weekends, I enjoy...", response.suggestedStarterEn());
        assertNotNull(response.sanitizedUserAnswer());
        assertTrue(response.sanitizedUserAnswer().contains("Truong"));
    }

    @Test
    void testConverseAndEvaluate_RestoresJohnToTruongInAnswer() throws IOException {
        String mockAiJson = """
            {
              "score": 88,
              "spokenFeedbackEn": "Great to meet you, Truong!",
              "feedbackVi": "🌟 Tuyệt vời lắm Trường! Giới thiệu bản thân rất rõ ràng.",
              "grammarCorrection": "My name is Truong.",
              "nextQuestionEn": "What is your favorite hobby, Truong?",
              "nextHintVi": "Kể về sở thích của bạn.",
              "suggestedStarterEn": "My favorite hobby is..."
            }
            """;
        when(groqClientService.generateChatCompletion(anyString(), anyString())).thenReturn(mockAiJson);

        // ASR erroneously produced "my name is john"
        CosmicCallRequest request = new CosmicCallRequest(
            "my name is john",
            "What is your name?",
            "Mercury: Greetings",
            "A1 Beginner",
            Collections.emptyList(),
            "Trường",
            List.of("my name is john", "my name is tron")
        );

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);

        assertNotNull(response);
        assertEquals("my name is Truong", response.sanitizedUserAnswer());
        assertTrue(response.spokenFeedbackEn().contains("Truong"));
        assertFalse(response.spokenFeedbackEn().contains("John"));
        assertEquals("My name is Truong.", response.grammarCorrection());
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
