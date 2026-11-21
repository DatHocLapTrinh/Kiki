package com.kiki.backend.dto;

import java.util.List;

public record TranscriptNormalizationRequest(
    String rawTranscript,
    String userName,
    List<String> candidateTranscripts
) {}
