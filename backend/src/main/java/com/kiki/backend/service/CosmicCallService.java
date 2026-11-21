package com.kiki.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiki.backend.dto.CosmicCallRequest;
import com.kiki.backend.dto.CosmicCallResponse;
import com.kiki.backend.dto.CosmicCallTurnDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CosmicCallService {

    private static final Logger log = LoggerFactory.getLogger(CosmicCallService.class);

    private final GroqClientService groqClientService;
    private final ObjectMapper objectMapper;

    public CosmicCallService(GroqClientService groqClientService, ObjectMapper objectMapper) {
        this.groqClientService = groqClientService;
        this.objectMapper = objectMapper;
    }

    public CosmicCallResponse converseAndEvaluate(CosmicCallRequest request) {
        try {
            String systemPrompt = """
                You are Kiki, an enthusiastic and friendly AI English Tutor in the Kiki Cosmic Universe.
                You are having a real-time 1-on-1 voice phone call with a Vietnamese English learner.

                CORE CONVERSATIONAL PRINCIPLES:
                1. BE CONTEXTUAL & ENGAGING: Directly acknowledge what the learner actually said. Do NOT give generic, robotic answers. If they mentioned their name, city, hobbies, job, or opinions, incorporate those details naturally.
                2. VIETNAMESE LEARNER & ASR AWARENESS:
                   - The learner is Vietnamese. They may have Vietnamese names (e.g. Trường, Tuấn, Nam, Linh, Phúc, Mai, Hương) or mention Vietnamese places (Hà Nội, Sài Gòn, Đà Nẵng).
                   - Speech recognition (ASR) engines might mishear Vietnamese phonemes as English words (for example: learner said 'Trường' but ASR transcribed 'John' or 'Sean').
                   - NEVER penalize learners for Vietnamese names or ASR phonetic artifacts! Understand their intended meaning seamlessly.
                3. PEDAGOGICAL VOICE FLOW:
                   - spokenFeedbackEn: A natural, concise English response (10-25 words) that Kiki will speak aloud through Text-To-Speech. It must sound conversational, warm, and encourage the learner.
                   - feedbackVi: An encouraging Vietnamese explanation (1-2 sentences). MUST start with an enthusiastic compliment (e.g. 'Tuyệt vời lắm! 🌟', 'Rất xuất sắc! 👏', 'Khởi đầu rất tự tin! 💪'), followed by a specific note on grammar, vocabulary, or pronunciation.
                   - grammarCorrection: The most natural, native-sounding English version of their sentence.
                   - nextQuestionEn: Exactly 1 natural follow-up question that smoothly continues the current conversation topic. Keep it focused, clear, and relevant (do not wander off into unrelated topics).
                   - nextHintVi: A short Vietnamese hint (5-10 words) to help the student formulate their answer.
                   - suggestedStarterEn: A contextual sentence starter (3-7 words) tailored directly to nextQuestionEn to show the user how to begin their sentence. (e.g. If next question is about coffee: 'My favorite coffee is...', If about travel: 'I usually travel to...', If about hobbies: 'I relax by...'). DO NOT just return generic 'I think that...'!
                4. ACCURATE SCORING:
                   - Score between 65 and 100 based on communicative relevance, vocabulary richness, sentence structure, and communicative effort.

                CRITICAL: Return ONLY a valid JSON object matching this schema with NO markdown wrapping, NO backticks:
                {
                  "score": 85,
                  "spokenFeedbackEn": "string (10-25 words in English to be spoken by Kiki TTS)",
                  "feedbackVi": "string (Enthusiastic compliment and pedagogical note in Vietnamese)",
                  "grammarCorrection": "string (Improved English sentence)",
                  "nextQuestionEn": "string (1 context-aware follow-up question in English)",
                  "nextHintVi": "string (Short Vietnamese hint for next question)",
                  "suggestedStarterEn": "string (Contextual 3-7 word starter specifically for nextQuestionEn)"
                }
                """;

            StringBuilder historyBuilder = new StringBuilder();
            if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
                historyBuilder.append("\nRecent conversation turns in this call:\n");
                for (CosmicCallTurnDto turn : request.conversationHistory()) {
                    historyBuilder.append("- ")
                        .append("user".equalsIgnoreCase(turn.role()) ? "Learner" : "Kiki")
                        .append(": ")
                        .append(turn.text())
                        .append("\n");
                }
            }

            String userPrompt = String.format(
                "Current Topic / Cosmic Planet: %s\n" +
                "Learner CEFR Level: %s\n" +
                "Kiki's Previous Question: \"%s\"\n" +
                "Learner's Spoken Answer: \"%s\"\n" +
                "%s\n" +
                "Evaluate the learner's answer, provide encouraging English spoken feedback, Vietnamese educational notes, grammar correction, and exactly 1 contextual follow-up question.",
                request.planetOrTopic() != null ? request.planetOrTopic() : "Cosmic English Conversation",
                request.userLevel() != null ? request.userLevel() : "A1-A2 Elementary",
                request.currentQuestion() != null ? request.currentQuestion() : "Can you introduce yourself?",
                request.userAnswer(),
                historyBuilder.toString()
            );

            String rawResponse = groqClientService.generateChatCompletion(systemPrompt, userPrompt);
            String cleanJson = extractCleanJson(rawResponse);

            JsonNode root = objectMapper.readTree(cleanJson);
            int score = root.path("score").asInt(85);
            if (score < 60) score = 65;
            if (score > 100) score = 100;

            String spokenFeedbackEn = root.path("spokenFeedbackEn").asText("").trim();
            if (spokenFeedbackEn.isEmpty()) {
                spokenFeedbackEn = "Great job! Your answer was very clear and expressive.";
            }

            String feedbackVi = root.path("feedbackVi").asText("").trim();
            if (feedbackVi.isEmpty()) {
                feedbackVi = "Phản xạ rất tốt! Câu trả lời thể hiện sự tự tin và diễn đạt tự nhiên.";
            }

            String grammarCorrection = root.path("grammarCorrection").asText("").trim();
            if (grammarCorrection.isEmpty()) {
                grammarCorrection = request.userAnswer();
            }

            String nextQuestionEn = root.path("nextQuestionEn").asText("").trim();
            if (nextQuestionEn.isEmpty()) {
                nextQuestionEn = "What do you usually do during your free time?";
            }

            String nextHintVi = root.path("nextHintVi").asText("").trim();
            if (nextHintVi.isEmpty()) {
                nextHintVi = "Chia sẻ về thói quen hoặc sở thích lúc rảnh rỗi.";
            }

            String suggestedStarterEn = root.path("suggestedStarterEn").asText("").trim();
            if (suggestedStarterEn.isEmpty()) {
                suggestedStarterEn = deriveContextualStarter(nextQuestionEn);
            }

            return new CosmicCallResponse(
                score,
                spokenFeedbackEn,
                feedbackVi,
                grammarCorrection,
                nextQuestionEn,
                nextHintVi,
                suggestedStarterEn
            );
        } catch (Exception e) {
            log.warn("AI generation or JSON parsing failed: {}. Falling back to resilient evaluation.", e.getMessage());
            return generateResilientFallback(request);
        }
    }

    private CosmicCallResponse generateResilientFallback(CosmicCallRequest request) {
        String answer = request.userAnswer().trim();
        int wordCount = answer.split("\\s+").length;

        int score;
        String spokenEn;
        String feedbackVi;

        if (wordCount >= 6) {
            score = 92;
            spokenEn = "Excellent reflection! That was fluent and very natural.";
            feedbackVi = "🌟 Xuất sắc tuyệt vời! Bạn nói câu hoàn chỉnh, mạch lạc và phát âm rất tự tin.";
        } else if (wordCount >= 3) {
            score = 84;
            spokenEn = "Good job! Next time, try to add one more detail to your sentence.";
            feedbackVi = "👏 Rất tốt! Bạn phát âm rõ ràng, lần tới hãy thử bổ sung thêm 1 chi tiết nhỏ nhé.";
        } else if (wordCount >= 1) {
            score = 75;
            spokenEn = "Nice start! Keep building your confidence with me.";
            feedbackVi = "💪 Khởi đầu rất tự tin! Hãy luyện nói thành cả câu hoàn chỉnh để tăng phản xạ nhé.";
        } else {
            score = 65;
            spokenEn = "I didn't quite catch that. Please speak a little louder into the mic!";
            feedbackVi = "✨ Đừng ngại, hãy giữ nút Micro và nói to, rõ ràng hơn để Kiki lắng nghe nhé.";
        }

        String nextQ = "Could you tell me what you like the most about learning English?";
        return new CosmicCallResponse(
            score,
            spokenEn,
            feedbackVi,
            answer,
            nextQ,
            "Nói về điều bạn thích nhất khi học tiếng Anh.",
            "I love learning English because..."
        );
    }

    private String deriveContextualStarter(String question) {
        String lower = question.toLowerCase();
        if (lower.contains("favorite") || lower.contains("like") || lower.contains("enjoy")) {
            return "My favorite is...";
        } else if (lower.contains("what time") || lower.contains("routine") || lower.contains("usually")) {
            return "I usually...";
        } else if (lower.contains("where") || lower.contains("travel") || lower.contains("live")) {
            return "I live in...";
        } else if (lower.contains("opinion") || lower.contains("think") || lower.contains("view")) {
            return "In my opinion,...";
        } else if (lower.contains("how often")) {
            return "I often...";
        } else {
            return "For me,...";
        }
    }

    private String extractCleanJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
