package com.kiki.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record GenerateChapterRequest(
    String userId,

    @NotBlank(message = "Trình độ học viên không được để trống (vd: A1, A2, B1, B2)")
    String level,

    @NotBlank(message = "Chủ đề chương học không được để trống")
    String topic,

    List<String> weakWords,

    Integer questionCount
) {
    public GenerateChapterRequest {
        if (questionCount == null || questionCount <= 0) {
            questionCount = 5;
        }
    }
}
