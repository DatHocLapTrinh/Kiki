package com.kiki.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Bộ chấm điểm sư phạm chặt chẽ cho đàm thoại 1-1 bằng Java:
 * - 0 điểm: Người dùng chưa nói / im lặng / chuỗi rỗng / chỉ có tiếng thở
 * - 15 - 35 điểm: Nói sai câu hỏi, lạc đề, từ vô nghĩa, hoặc tiếng Việt không liên quan
 * - 40 - 59 điểm: Nói ngắt quãng, sai ngữ pháp nặng hoặc câu cụt 1 từ
 * - 60 - 79 điểm: Nói đúng trọng tâm, hiểu được, còn vài lỗi ngữ pháp nhỏ
 * - 80 - 89 điểm: Nói đúng, lưu loát, tự nhiên
 * - 90 - 100 điểm: Xuất sắc, cấu trúc câu đa dạng, phát âm rõ ràng
 */
@Component
public class CosmicCallScorer {

    private static final Logger log = LoggerFactory.getLogger(CosmicCallScorer.class);

    // Tập hợp các từ đệm/tiếng ồn vô nghĩa
    private static final Set<String> FILLER_OR_NOISE_WORDS = Set.of(
        "uh", "um", "ah", "er", "hmm", "eh", "oh", "ha", "huh", "asdf", "blabla", "...", "abc"
    );

    /**
     * Kiểm tra xem câu trả lời có hoàn toàn trống / im lặng không
     */
    public boolean isEmptyOrSilence(String text) {
        if (text == null) return true;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return true;
        // Loại bỏ ký tự đặc biệt, kiểm tra xem có ký tự chữ cái/số thực sự không
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]", "");
        return cleaned.isEmpty();
    }

    /**
     * Kiểm tra xem câu trả lời có chỉ chứa từ đệm vô nghĩa hoặc rác âm thanh không
     */
    public boolean isGibberishOrPureNoise(String text) {
        if (isEmptyOrSilence(text)) return true;
        String[] words = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length == 0) return true;

        int meaningfulCount = 0;
        for (String w : words) {
            String clean = w.replaceAll("[^a-zA-Z]", "");
            if (!clean.isEmpty() && !FILLER_OR_NOISE_WORDS.contains(clean)) {
                meaningfulCount++;
            }
        }
        return meaningfulCount == 0;
    }

    /**
     * Hiệu chỉnh và bảo vệ điểm số, loại bỏ hoàn toàn tình trạng chấm nới 70-72 điểm cho câu nói sai hoặc không nói gì
     *
     * @param rawAiScore Điểm do AI sinh ra (nếu có)
     * @param question   Câu hỏi của Kiki
     * @param answer     Câu trả lời đã được chuẩn hoá của học viên
     * @return Điểm số sau khi kiểm định sư phạm chặt chẽ (0 - 100)
     */
    public int calibrateScore(int rawAiScore, String question, String answer) {
        // 1. Nếu không nói gì hoặc chỉ có tiếng thở/im lặng -> CHÍNH XÁC 0 ĐIỂM
        if (isEmptyOrSilence(answer)) {
            log.info("CosmicCallScorer: Empty or silence detected -> 0 points");
            return 0;
        }

        // 2. Nếu chỉ có tiếng ồn hoặc từ đệm rỗng -> 15 điểm
        if (isGibberishOrPureNoise(answer)) {
            log.info("CosmicCallScorer: Gibberish or pure noise detected -> 15 points");
            return 15;
        }

        String[] words = answer.trim().split("\\s+");
        String lowerAns = answer.toLowerCase(Locale.ROOT);
        String lowerQ = question != null ? question.toLowerCase(Locale.ROOT) : "";

        // 3. Kiểm tra tính liên quan (Relevance) với câu hỏi:
        boolean isOffTopic = checkOffTopic(lowerQ, lowerAns);
        if (isOffTopic) {
            log.info("CosmicCallScorer: Off-topic answer detected ('{}' vs Q: '{}') -> capped at 25 points", answer, question);
            return Math.min(rawAiScore, 25);
        }

        // 4. Nếu câu trả lời quá ngắn (chỉ 1 từ cụt lủn):
        if (words.length == 1) {
            if (rawAiScore > 50) {
                log.info("CosmicCallScorer: Single isolated word answer -> capped at 45 points");
                return 45;
            }
        }

        // 5. Đảm bảo điểm số nằm trong khoảng [0, 100] không bị ép sàn
        return Math.max(0, Math.min(100, rawAiScore));
    }

    /**
     * Kiểm tra xem câu trả lời có bị lạc đề hoàn toàn không
     */
    public boolean checkOffTopic(String lowerQ, String lowerAns) {
        if (lowerQ.isEmpty()) return false;

        // Câu hỏi về tên: "what is your name", "who are you", "introduce yourself"
        if (lowerQ.contains("your name") || lowerQ.contains("who are you")) {
            if (lowerAns.contains("weather") || lowerAns.contains("rain") || lowerAns.contains("coffee") || lowerAns.contains("hotel")) {
                if (!lowerAns.contains("name") && !lowerAns.contains("am") && !lowerAns.contains("i'm")) {
                    return true;
                }
            }
        }

        // Câu hỏi về nơi ở: "where do you live", "where are you from"
        if (lowerQ.contains("where do you live") || lowerQ.contains("where are you from")) {
            if (lowerAns.contains("football") || lowerAns.contains("music") || lowerAns.contains("sleep")) {
                if (!lowerAns.contains("live") && !lowerAns.contains("from") && !lowerAns.contains("in")) {
                    return true;
                }
            }
        }

        // Câu hỏi về đồ ăn: "favorite food", "what do you eat"
        if (lowerQ.contains("favorite food") || lowerQ.contains("what do you eat")) {
            if (lowerAns.contains("car") || lowerAns.contains("drive") || lowerAns.contains("computer")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Chấm điểm ngoại tuyến dựa trên cấu trúc & độ hoàn thiện (Deterministic Fallback)
     */
    public int computeDeterministicScore(String question, String answer) {
        if (isEmptyOrSilence(answer)) return 0;
        if (isGibberishOrPureNoise(answer)) return 15;

        String lowerAns = answer.toLowerCase(Locale.ROOT);
        String lowerQ = question != null ? question.toLowerCase(Locale.ROOT) : "";

        if (checkOffTopic(lowerQ, lowerAns)) {
            return 25; // Trả lời sai/lạc đề
        }

        int wordCount = answer.trim().split("\\s+").length;
        if (wordCount == 1) {
            return 45; // Quá cộc lốc
        } else if (wordCount <= 3) {
            return 65; // Đủ ý cơ bản
        } else if (wordCount <= 6) {
            return 80; // Khá tốt, thành câu
        } else {
            return 90; // Đầy đủ, lưu loát
        }
    }
}
