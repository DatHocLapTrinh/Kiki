package com.kiki.backend.dto;

public record TranscriptNormalizationResponse(
    String rawTranscript,
    String normalizedTranscript
) {}
