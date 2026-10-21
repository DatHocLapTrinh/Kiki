package com.kiki.backend.dto;

import java.util.List;

public record MentorChatResponse(
    String reply,
    String grammarTip,
    List<VocabularyCard> detectedVocabularies
) {
    public record VocabularyCard(
        String word,
        String phonetic,
        String meaning,
        String example
    ) {}
}
