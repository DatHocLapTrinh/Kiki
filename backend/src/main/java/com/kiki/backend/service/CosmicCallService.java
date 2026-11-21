package com.kiki.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiki.backend.dto.CosmicCallRequest;
import com.kiki.backend.dto.CosmicCallResponse;
import com.kiki.backend.dto.CosmicCallTurnDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CosmicCallService {

    private static final Logger log = LoggerFactory.getLogger(CosmicCallService.class);

    private final GroqClientService groqClientService;
    private final ObjectMapper objectMapper;
    private final VietnameseNameNormalizer vietnameseNameNormalizer;
    private final CosmicCallScorer cosmicCallScorer;

    public CosmicCallService(
        GroqClientService groqClientService,
        ObjectMapper objectMapper,
        VietnameseNameNormalizer vietnameseNameNormalizer,
        CosmicCallScorer cosmicCallScorer
    ) {
        this.groqClientService = groqClientService;
        this.objectMapper = objectMapper;
        this.vietnameseNameNormalizer = vietnameseNameNormalizer;
        this.cosmicCallScorer = cosmicCallScorer;
    }

    public CosmicCallResponse converseAndEvaluate(CosmicCallRequest request) {
        // 1. Kiểm tra nếu người dùng chưa nói gì / im lặng hoàn toàn -> Trả về 0 điểm ngay lập tức
        if (cosmicCallScorer.isEmptyOrSilence(request.userAnswer())) {
            log.info("Cosmic Call: User did not speak (empty/silence). Returning 0 points immediately.");
            return new CosmicCallResponse(
                0,
                "I didn't hear your voice. Please tap the mic and speak aloud!",
                "Kiki chưa nghe thấy câu trả lời của bạn. Hãy nhấn nút Micro và nói to, rõ ràng nhé! 🎙️",
                "",
                request.currentQuestion() != null ? request.currentQuestion() : "What is your name?",
                "Bấm giữ nút Micro và nói tiếng Anh.",
                "Hello Kiki,...",
                ""
            );
        }

        // 2. Khôi phục tên riêng & địa danh tiếng Việt bị ASR ép kiểu sang tiếng Anh (vd: "my name is john" -> "my name is Truong")
        String sanitizedUserAnswer = vietnameseNameNormalizer.normalize(
            request.userAnswer(),
            request.userName(),
            request.candidateTranscripts()
        );

        String learnerGivenName = vietnameseNameNormalizer.extractGivenName(request.userName());
        if (learnerGivenName == null) {
            learnerGivenName = "Learner";
        }

        // 3. Kiểm tra nếu người học nói "I don't know", "No I don't know either", "I have no idea"
        if (cosmicCallScorer.isDontKnowOrEvasion(sanitizedUserAnswer)) {
            log.info("Cosmic Call: Learner said 'I don't know' or evasion ('{}'). Scoring 25 points with supportive guidance.", sanitizedUserAnswer);
            String starter = deriveContextualStarter(request.currentQuestion() != null ? request.currentQuestion() : "");
            return new CosmicCallResponse(
                25,
                "That's okay if you don't know, " + learnerGivenName + "! Let's practice with the example starter together.",
                "Đừng lo lắng nếu bạn chưa rõ câu trả lời nhé! Kiki đã chuẩn bị câu gợi ý bên dưới để bạn học và luyện tập nào. 💡",
                "Try saying: \"" + starter + "\"",
                request.currentQuestion() != null ? request.currentQuestion() : "What is your favorite hobby to relax?",
                "Xem câu gợi ý mẫu và luyện nói lại nhé.",
                starter,
                sanitizedUserAnswer
            );
        }

        try {
            String systemPrompt = String.format("""
                You are Kiki, an enthusiastic, honest and pedagogical AI English Tutor in the Kiki Cosmic Universe.
                You are having a real-time 1-on-1 voice phone call with a Vietnamese English learner named %s.

                STRICT PEDAGOGICAL PRINCIPLES:
                1. HONEST & ACCURATE SCORING (0 to 100 scale):
                   - 0: Silence, no words spoken.
                   - 10 to 35: Completely incorrect, irrelevant, off-topic (e.g. asked for name but answered about weather/food), nonsense, evasion, or saying 'I don't know'.
                   - 40 to 59: Fragmented, severe grammar errors, or single isolated word without sentence structure.
                   - 60 to 79: Relevant and communicative; directly answers the question with minor grammar slips.
                   - 80 to 89: Good, fluent, natural and grammatically correct.
                   - 90 to 100: Excellent, elaborate, rich vocabulary, clear and natural expression.

                   CRITICAL RULE: DO NOT give a passing score (60+) to incorrect, irrelevant or nonsensical answers! If the learner said something wrong, score MUST be below 40.

                2. VIETNAMESE LEARNER & ASR AWARENESS:
                   - The learner's name is %s.
                   - NEVER address the learner as 'John', 'Sean', or any misheard ASR artifact! ALWAYS use their real name (%s).
                   - NEVER penalize learners for Vietnamese names or ASR phonetic artifacts! Understand their intended meaning seamlessly.

                3. PEDAGOGICAL VOICE FLOW:
                   - spokenFeedbackEn: A natural, concise English response (10-25 words) that Kiki will speak aloud through Text-To-Speech. It must sound conversational, warm, address the learner by name (%s), and honestly critique or praise them.
                   - feedbackVi: An encouraging Vietnamese explanation (1-2 sentences).
                     * IF score >= 65: Enthusiastic praise ('Tuyệt vời lắm %s! 🌟', 'Rất xuất sắc! 👏').
                     * IF score < 65: Constructive correction ('Câu trả lời chưa đúng trọng tâm hoặc sai ngữ pháp, bạn xem gợi ý bên dưới nhé! 💡'). NEVER praise a wrong or off-topic answer with 'Tuyệt vời lắm'!
                   - grammarCorrection: The most natural, native-sounding English version of their sentence.
                   - nextQuestionEn: Exactly 1 natural follow-up question that smoothly continues the current conversation topic. Keep it focused, clear, and relevant.
                   - nextHintVi: A short Vietnamese hint (5-10 words) to help the student formulate their answer.
                   - suggestedStarterEn: A contextual sentence starter (3-7 words) tailored directly to nextQuestionEn to show the user how to begin their sentence. (e.g. If next question is about coffee: 'My favorite coffee is...', If about travel: 'I usually travel to...'). DO NOT just return generic 'I think that...'!

                CRITICAL: Return ONLY a valid JSON object matching this schema with NO markdown wrapping, NO backticks:
                {
                  "score": 85,
                  "spokenFeedbackEn": "string (10-25 words in English to be spoken by Kiki TTS)",
                  "feedbackVi": "string (Pedagogical note in Vietnamese)",
                  "grammarCorrection": "string (Improved English sentence)",
                  "nextQuestionEn": "string (1 context-aware follow-up question in English)",
                  "nextHintVi": "string (Short Vietnamese hint for next question)",
                  "suggestedStarterEn": "string (Contextual 3-7 word starter specifically for nextQuestionEn)"
                }
                """, learnerGivenName, learnerGivenName, learnerGivenName, learnerGivenName, learnerGivenName);

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
                "Learner's Name: %s\n" +
                "Current Topic / Cosmic Planet: %s\n" +
                "Learner CEFR Level: %s\n" +
                "Kiki's Previous Question: \"%s\"\n" +
                "Learner's Spoken Answer (restored): \"%s\"\n" +
                "%s\n" +
                "Evaluate the learner's answer honestly using the strict 0-100 rubric, provide spoken English feedback, Vietnamese educational notes, grammar correction, and exactly 1 contextual follow-up question.",
                learnerGivenName,
                request.planetOrTopic() != null ? request.planetOrTopic() : "Cosmic English Conversation",
                request.userLevel() != null ? request.userLevel() : "A1-A2 Elementary",
                request.currentQuestion() != null ? request.currentQuestion() : "Can you introduce yourself?",
                sanitizedUserAnswer,
                historyBuilder.toString()
            );

            String rawResponse = groqClientService.generateChatCompletion(systemPrompt, userPrompt);
            String cleanJson = extractCleanJson(rawResponse);

            JsonNode root = objectMapper.readTree(cleanJson);
            int rawScore = root.path("score").asInt(50);

            // Kiểm định và hiệu chỉnh điểm số bằng Java (loại bỏ hoàn toàn tình trạng chấm 70-78 điểm cho câu nói sai hoặc lạc đề)
            int calibratedScore = cosmicCallScorer.calibrateScore(rawScore, request.currentQuestion(), sanitizedUserAnswer);

            String spokenFeedbackEn = root.path("spokenFeedbackEn").asText("").trim();
            if (spokenFeedbackEn.isEmpty()) {
                spokenFeedbackEn = calibratedScore >= 65
                    ? "Great job " + learnerGivenName + "! Your answer was very clear and expressive."
                    : "That didn't quite sound right, " + learnerGivenName + ". Let's check the correct sentence!";
            }

            String feedbackVi = root.path("feedbackVi").asText("").trim();
            if (calibratedScore < 60) {
                // Bảo vệ sư phạm: Không để AI khen "Tuyệt vời lắm" khi điểm dưới 60
                if (feedbackVi.contains("Tuyệt vời") || feedbackVi.contains("xuất sắc") || feedbackVi.contains("Rất tốt")) {
                    feedbackVi = "Câu trả lời chưa đúng trọng tâm hoặc sai ngữ pháp. Bạn xem câu gợi ý bên dưới để hoàn thiện nhé! 💡";
                }
            }
            if (feedbackVi.isEmpty()) {
                feedbackVi = calibratedScore >= 65
                    ? "Phản xạ rất tốt " + learnerGivenName + "! Câu trả lời thể hiện sự tự tin và diễn đạt tự nhiên."
                    : "Câu trả lời chưa chính xác hoặc chưa đúng trọng tâm. Hãy xem gợi ý câu chuẩn bên dưới nhé! 💡";
            }

            String grammarCorrection = root.path("grammarCorrection").asText("").trim();
            if (grammarCorrection.isEmpty()) {
                grammarCorrection = sanitizedUserAnswer;
            }

            // Phòng ngừa lỗi ASR: Nếu LLM vô tình sót lại tên John/Sean thay vì tên thật của học viên
            if (!"Learner".equals(learnerGivenName)) {
                grammarCorrection = grammarCorrection.replaceAll("(?i)\\b(John|Sean|Shawn)\\b", learnerGivenName);
                spokenFeedbackEn = spokenFeedbackEn.replaceAll("(?i)\\b(John|Sean|Shawn)\\b", learnerGivenName);
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
                calibratedScore,
                spokenFeedbackEn,
                feedbackVi,
                grammarCorrection,
                nextQuestionEn,
                nextHintVi,
                suggestedStarterEn,
                sanitizedUserAnswer
            );
        } catch (Exception e) {
            log.warn("AI generation or JSON parsing failed: {}. Falling back to resilient evaluation.", e.getMessage());
            return generateResilientFallback(request, sanitizedUserAnswer, learnerGivenName);
        }
    }

    private CosmicCallResponse generateResilientFallback(CosmicCallRequest request, String sanitizedUserAnswer, String learnerGivenName) {
        String answer = (sanitizedUserAnswer != null ? sanitizedUserAnswer : request.userAnswer()).trim();
        int score = cosmicCallScorer.computeDeterministicScore(request.currentQuestion(), answer);

        String spokenEn;
        String feedbackVi;

        if (score == 0) {
            spokenEn = "I didn't hear any words. Please press the mic and speak aloud!";
            feedbackVi = "Kiki chưa ghi nhận được câu trả lời. Hãy bấm nút Micro và nói to, rõ ràng nhé! 🎙️";
        } else if (score <= 35) {
            spokenEn = "That was not quite right for this question, " + learnerGivenName + ". Let's practice with the starter!";
            feedbackVi = "Câu trả lời chưa đúng trọng tâm câu hỏi hoặc từ ngữ chưa chính xác. Xem câu mẫu bên dưới nhé! 💡";
        } else if (score <= 59) {
            spokenEn = "You are on the right track " + learnerGivenName + ", but try to use a complete sentence!";
            feedbackVi = "Bạn đã có ý trả lời nhưng còn sai hoặc thiếu ngữ pháp. Xem câu chuẩn bên dưới nhé! ✨";
        } else if (score <= 84) {
            spokenEn = "Good job " + learnerGivenName + "! You answered clearly and confidently.";
            feedbackVi = "👏 Rất tốt " + learnerGivenName + "! Bạn phát âm rõ ràng, lần tới hãy thử bổ sung thêm 1 chi tiết nhỏ nhé.";
        } else {
            spokenEn = "Excellent reflection " + learnerGivenName + "! That was fluent and very natural.";
            feedbackVi = "🌟 Xuất sắc tuyệt vời! Bạn nói câu hoàn chỉnh, mạch lạc và phát âm rất tự tin.";
        }

        String nextQ = "Could you tell me what you like the most about learning English?";
        return new CosmicCallResponse(
            score,
            spokenEn,
            feedbackVi,
            answer,
            nextQ,
            "Nói về điều bạn thích nhất khi học tiếng Anh.",
            "I love learning English because...",
            sanitizedUserAnswer
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
