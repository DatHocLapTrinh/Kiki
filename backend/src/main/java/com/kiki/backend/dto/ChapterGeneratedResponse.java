package com.kiki.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChapterGeneratedResponse(
    String title,
    String description,
    String level,
    String subject,
    Integer orderNo,
    List<QuestionItem> questions
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionItem(
        String questionText,
        String questionType,
        String optionsJson,
        List<String> options,
        String correctAnswer,
        String explanation,
        int points,
        String difficulty
    ) {
        public QuestionItem(
            @JsonProperty("questionText") String questionText,
            @JsonProperty("questionType") String questionType,
            @JsonProperty("optionsJson") String optionsJson,
            @JsonProperty("options") List<String> options,
            @JsonProperty("correctAnswer") String correctAnswer,
            @JsonProperty("explanation") String explanation,
            @JsonProperty("points") int points,
            @JsonProperty("difficulty") String difficulty
        ) {
            this.questionText = questionText;
            this.questionType = questionType != null ? questionType : "MULTIPLE_CHOICE";
            this.correctAnswer = correctAnswer != null ? correctAnswer : "0";
            this.explanation = explanation;
            this.points = points > 0 ? points : 10;
            this.difficulty = difficulty != null ? difficulty : "MEDIUM";
            this.options = options;

            String resolved = optionsJson;
            if ((resolved == null || resolved.isBlank()) && options != null && !options.isEmpty()) {
                try {
                    resolved = MAPPER.writeValueAsString(options);
                } catch (Exception e) {
                    resolved = "[]";
                }
            }
            this.optionsJson = resolved != null ? resolved : "[]";
        }
    }
}
