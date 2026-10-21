package com.kiki.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record MentorChatRequest(
    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String message,

    String userLevel,

    String currentChapter,

    List<String> recentWeakPoints
) {}
