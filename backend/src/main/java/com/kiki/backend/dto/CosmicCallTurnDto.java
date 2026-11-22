package com.kiki.backend.dto;

/**
 * Lượt hội thoại trong phiên đàm thoại AI 1-1
 */
public record CosmicCallTurnDto(
    String role,    // "kiki" hoặc "user"
    String text     // Nội dung đã nói
) {}
