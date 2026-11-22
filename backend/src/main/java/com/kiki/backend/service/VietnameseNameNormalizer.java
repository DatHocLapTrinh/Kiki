package com.kiki.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xử lý khôi phục tên riêng & địa danh tiếng Việt bị ASR ép kiểu sang tên tiếng Anh (ASR Phonetic Alias Restoration)
 * Ví dụ: Người dùng nói "my name is Truong" bị Google SpeechRecognizer (chế độ en-US) ép kiểu thành "my name is john"
 */
@Component
public class VietnameseNameNormalizer {

    private static final Logger log = LoggerFactory.getLogger(VietnameseNameNormalizer.class);

    // Mẫu câu giới thiệu tên phổ biến trong đàm thoại tiếng Anh
    private static final Pattern INTRO_PATTERN = Pattern.compile(
        "(?i)\\b(my name is|i am|i'm|this is|call me)\\s+([a-zA-Z]+)\\b"
    );

    // Bảng quy đổi âm vị học (Phonetic Confusion Matrix) giữa âm tiếng Việt và từ tiếng Anh ASR hay nghe nhầm
    private static final Map<String, String> PHONETIC_NAME_MAP = Map.ofEntries(
        Map.entry("john", "Truong"),
        Map.entry("sean", "Truong"),
        Map.entry("shawn", "Truong"),
        Map.entry("tron", "Truong"),
        Map.entry("drawn", "Truong"),
        Map.entry("strong", "Truong"),
        Map.entry("wrong", "Truong"),
        Map.entry("choi", "Truong"),
        Map.entry("trong", "Truong"),
        Map.entry("trung", "Truong"),
        Map.entry("duck", "Duc"),
        Map.entry("dock", "Duc"),
        Map.entry("doc", "Duc"),
        Map.entry("dark", "Duc"),
        Map.entry("link", "Linh"),
        Map.entry("lynn", "Linh"),
        Map.entry("lean", "Linh"),
        Map.entry("fong", "Phong"),
        Map.entry("fung", "Phong"),
        Map.entry("phone", "Phong"),
        Map.entry("main", "Minh"),
        Map.entry("mine", "Minh"),
        Map.entry("mean", "Minh"),
        Map.entry("men", "Minh"),
        Map.entry("nam", "Nam"),
        Map.entry("numb", "Nam"),
        Map.entry("twin", "Tuan"),
        Map.entry("too", "Tuan"),
        Map.entry("hung", "Hung"),
        Map.entry("hong", "Hung"),
        Map.entry("charm", "Trang"),
        Map.entry("tram", "Trang"),
        Map.entry("why", "Vy"),
        Map.entry("vie", "Vy"),
        Map.entry("high", "Hai"),
        Map.entry("height", "Hai"),
        Map.entry("win", "Nguyen"),
        Map.entry("when", "Nguyen"),
        Map.entry("gwen", "Nguyen"),
        Map.entry("my", "Mai"),
        Map.entry("mai", "Mai"),
        Map.entry("land", "Lan"),
        Map.entry("lan", "Lan"),
        Map.entry("dat", "Dat"),
        Map.entry("that", "Dat"),
        Map.entry("dad", "Dat"),
        Map.entry("towel", "Thao"),
        Map.entry("thao", "Thao"),
        Map.entry("wha", "Hoa"),
        Map.entry("hoa", "Hoa"),
        Map.entry("one", "Quan"),
        Map.entry("quan", "Quan")
    );

    /**
     * Chuẩn hoá câu nói của người dùng, khôi phục tên riêng hoặc địa danh tiếng Việt
     *
     * @param rawTranscript        Câu trả lời gốc do ASR nhận diện (vd: "my name is john")
     * @param userName             Tên hiển thị người dùng trên app (vd: "Nguyễn Văn Trường" hoặc "Truong")
     * @param candidateTranscripts Danh sách các phương án nhận diện khác từ Recognizer
     * @return Câu nói đã được khôi phục chính xác (vd: "my name is Truong")
     */
    public String normalize(String rawTranscript, String userName, List<String> candidateTranscripts) {
        if (rawTranscript == null || rawTranscript.isBlank()) {
            return rawTranscript;
        }

        String targetGivenName = extractGivenName(userName);
        String cleaned = rawTranscript.trim();

        // 1. Kiểm tra trong danh sách ứng viên (candidateTranscripts) xem có ứng viên nào chứa tên thật không
        if (targetGivenName != null && candidateTranscripts != null && !candidateTranscripts.isEmpty()) {
            String lowerTarget = removeAccents(targetGivenName).toLowerCase(Locale.ROOT);
            for (String candidate : candidateTranscripts) {
                if (candidate != null && removeAccents(candidate).toLowerCase(Locale.ROOT).contains(lowerTarget)) {
                    log.info("Found matching candidate with learner name: '{}'", candidate);
                    cleaned = candidate;
                    break;
                }
            }
        }

        // 2. Nhận diện mẫu giới thiệu tên: "my name is [john]" / "i am [john]"
        Matcher matcher = INTRO_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            String prefix = matcher.group(1);
            String capturedName = matcher.group(2).toLowerCase(Locale.ROOT);

            String restoredName = null;
            if (targetGivenName != null) {
                // Người dùng có tên profile xác định (vd: Truong / Trường)
                restoredName = removeAccents(targetGivenName);
            } else if (PHONETIC_NAME_MAP.containsKey(capturedName)) {
                // Tra cứu từ điển âm vị
                restoredName = PHONETIC_NAME_MAP.get(capturedName);
            }

            if (restoredName != null) {
                String replacement = prefix + " " + restoredName;
                cleaned = matcher.replaceFirst(replacement);
                log.info("Restored learner name in intro from '{}' to '{}'", capturedName, restoredName);
            }
        }

        // 3. Chuẩn hoá các địa danh Việt Nam phổ biến
        cleaned = cleaned.replaceAll("(?i)\\bha\\s+noi\\b", "Hanoi")
                         .replaceAll("(?i)\\bhanno\\b", "Hanoi")
                         .replaceAll("(?i)\\bsai\\s+gon\\b", "Saigon")
                         .replaceAll("(?i)\\bda\\s+nang\\b", "Da Nang")
                         .replaceAll("(?i)\\bviet\\s+nam\\b", "Vietnam");

        return cleaned;
    }

    /**
     * Tách tên chính (Given Name) từ họ tên người dùng
     */
    public String extractGivenName(String fullName) {
        if (fullName == null || fullName.isBlank() || "Adventurer".equalsIgnoreCase(fullName)) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    /**
     * Loại bỏ dấu tiếng Việt (vd: "Trường" -> "Truong")
     */
    public String removeAccents(String text) {
        if (text == null) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                         .replace("đ", "d")
                         .replace("Đ", "D");
    }
}
