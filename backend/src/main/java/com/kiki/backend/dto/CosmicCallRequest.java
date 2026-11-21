package com.kiki.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Yêu cầu đàm thoại 1-1 với Kiki
 */
public record CosmicCallRequest(
    @NotBlank(message = "userAnswer is required")
    String userAnswer,

    String currentQuestion,
    String planetOrTopic,
    String userLevel,
    List<CosmicCallTurnDto> conversationHistory
) {}
