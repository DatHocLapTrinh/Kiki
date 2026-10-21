package com.kiki.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiki.backend.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AiGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationService.class);

    private final GroqClientService groqClientService;
    private final ObjectMapper objectMapper;

    public AiGenerationService(GroqClientService groqClientService, ObjectMapper objectMapper) {
        this.groqClientService = groqClientService;
        this.objectMapper = objectMapper;
    }

    public ChapterGeneratedResponse generateChapter(GenerateChapterRequest request) throws IOException {
        String systemPrompt = """
            You are Kiki, an elite AI English Curriculum Architect.
            Your task is to generate an engaging English learning chapter with DIVERSE interactive quiz questions strictly adhering to CEFR proficiency levels.
            
            IMPORTANT: You MUST return ONLY valid JSON with NO markdown formatting, NO backticks (```json), NO conversational intro or outro.
            
            QUESTION TYPE RULES - You MUST include all 5 types, distributed across the 5 questions:
            - MULTIPLE_CHOICE: Standard 4-option question. "options" = 4 strings, "correctAnswer" = "0","1","2","3" (index).
            - SENTENCE_BUILDER: Fill-blank sentence. "questionText" MUST contain ___ placeholder. "options" = [correct_word, distractor1, distractor2, distractor3], "correctAnswer" = "0".
            - LISTENING_CHALLENGE: Audio listening and word reordering. "questionText" = "Nghe cau sau va sap xep lai cac tu cho dung thu tu", "options" = [full English sentence to listen to and arrange], "correctAnswer" = "0".
            - MATCHING_PAIRS: Word-meaning pairs. "questionText" = "Ghep cap tu vung voi nghia tieng Viet tuong ung", "options" = ["word1=meaning1","word2=meaning2","word3=meaning3","word4=meaning4"], "correctAnswer" = "0".
            - SPEAKING_CHALLENGE: Pronunciation practice. "questionText" = "Luyen noi cau tieng Anh sau chuan ban xu", "options" = [full English sentence to speak], "correctAnswer" = "0".
            
            DISTRIBUTION RULE: For 5 questions use:
            Q1 = MULTIPLE_CHOICE
            Q2 = SENTENCE_BUILDER
            Q3 = LISTENING_CHALLENGE
            Q4 = MATCHING_PAIRS
            Q5 = SPEAKING_CHALLENGE
            
            JSON Schema:
            {
              "title": "string (Short Vietnamese title for the chapter)",
              "description": "string (1-2 sentence description in Vietnamese)",
              "level": "string (e.g. Beginner, Intermediate, Advanced)",
              "subject": "string (Topic category, e.g. Travel, Work, Daily Life, English)",
              "orderNo": 1,
              "questions": [
                {
                  "questionText": "string (Question prompt or sentence with ___ blank)",
                  "questionType": "MULTIPLE_CHOICE | SENTENCE_BUILDER | LISTENING_CHALLENGE | MATCHING_PAIRS | SPEAKING_CHALLENGE",
                  "options": ["opt1", "opt2", "opt3", "opt4"],
                  "correctAnswer": "string (Index 0-3 for MULTIPLE_CHOICE, or 0 for others)",
                  "explanation": "string (Clear bilingual explanation in Vietnamese)",
                  "points": 10,
                  "difficulty": "EASY | MEDIUM | HARD"
                }
              ]
            }
            """;

        String userPrompt = String.format(
            "Generate a chapter for level '%s' with topic '%s'. Total questions: %d. " +
            "Make sure to include diverse question types: MULTIPLE_CHOICE, SENTENCE_BUILDER, SPEAKING, and MATCHING_PAIRS. " +
            "%s",
            request.level(),
            request.topic(),
            request.questionCount(),
            (request.weakWords() != null && !request.weakWords().isEmpty())
                ? "Incorporate reinforcement for these weak words: " + String.join(", ", request.weakWords())
                : ""
        );

        String rawResponse = groqClientService.generateChatCompletion(systemPrompt, userPrompt);
        String cleanJson = extractCleanJson(rawResponse);

        try {
            return objectMapper.readValue(cleanJson, ChapterGeneratedResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse AI chapter JSON response: {}. Raw: {}", e.getMessage(), cleanJson);
            throw new IOException("AI da sinh noi dung nhung cau truc JSON khong dung dinh dang: " + e.getMessage());
        }
    }

    public MentorChatResponse mentorChat(MentorChatRequest request) throws IOException {
        String systemPrompt = """
            You are Kiki, an elite AI English Mentor.
            Provide concise, direct guidance to the student in 1 single answer without any conversational greetings or introductions (never say "Chào bạn", "Dưới đây là...", "Here is...").
            
            FORMATTING RULES (CRITICAL - strictly follow these):
            - NEVER use conversational introductions or greetings.
            - NEVER use markdown asterisks (**bold**) or hashes (#).
            - NEVER use markdown tables (| col |).
            - Structure your reply using plain text bullet points starting with "• ".
            - When explaining a word, idiom, phrase, or vocabulary, strictly follow this concise schema:
              [Word/Phrase] /IPA/ (Part of speech)
              • Nghĩa: [Vietnamese definition, concise and clear]
              • Ví dụ: [Natural English sentence] ([Vietnamese translation])
              • Từ đồng nghĩa: [3-5 synonyms separated by commas]
              • Từ trái nghĩa: [2-4 antonyms separated by commas]
            - When explaining grammar, provide a concise formula, 2 bilingual examples, and 1 tip using • bullet points.
            
            Return ONLY a valid JSON object matching this schema:
            {
              "reply": "string (Concise explanation using • bullet points, NO ** asterisks, NO greetings, NO tables)",
              "grammarTip": "string (Key takeaway in 1 concise sentence, NO ** asterisks)",
              "detectedVocabularies": [
                {
                  "word": "string (Vocabulary word)",
                  "phonetic": "string (/IPA/)",
                  "meaning": "string (Vietnamese definition)",
                  "example": "string (Illustrative English sentence)"
                }
              ]
            }
            """;

        String userPrompt = String.format(
            "Student level: %s. Current context/chapter: %s. Student question: %s",
            request.userLevel() != null ? request.userLevel() : "B1",
            request.currentChapter() != null ? request.currentChapter() : "General",
            request.message()
        );

        String rawResponse = groqClientService.generateChatCompletion(systemPrompt, userPrompt);
        String cleanJson = extractCleanJson(rawResponse);

        try {
            MentorChatResponse parsed = objectMapper.readValue(cleanJson, MentorChatResponse.class);
            // Post-process: clean up any remaining markdown asterisks from the reply
            String cleanReply = sanitizeMarkdown(parsed.reply());
            String cleanTip = sanitizeMarkdown(parsed.grammarTip());
            return new MentorChatResponse(cleanReply, cleanTip, parsed.detectedVocabularies());
        } catch (Exception e) {
            log.warn("Parsing mentor JSON failed, returning raw response: {}", e.getMessage());
            String cleanRaw = sanitizeMarkdown(rawResponse);
            return new MentorChatResponse(
                cleanRaw,
                "Luyện tập thường xuyên sẽ giúp bạn phản xạ tiếng Anh tự nhiên hơn!",
                List.of()
            );
        }
    }

    /**
     * Cleans up markdown formatting: removes ** asterisks, tables, dividers, greetings
     */
    private String sanitizeMarkdown(String input) {
        if (input == null) return "";
        return input
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")   // **bold** -> plain text
            .replaceAll("\\*(.+?)\\*", "$1")           // *italic* -> plain text
            .replaceAll("(?m)^#{1,6}\\s*", "")         // ## headings -> plain
            .replaceAll("\\[STEP\\]", "")              // [STEP] -> remove
            .replaceAll("(?m)^\\s*[-=_]{3,}\\s*$", "") // --- dividers
            .replaceAll("(?m)^\\s*\\|[-| :]+\\|\\s*$", "") // table separators
            .replaceAll("(?m)^\\s*\\|\\s*(.+?)\\s*\\|\\s*$", "$1") // table row
            .replaceAll("(?m)^\\s*[-*]\\s+", "\u2022 ")  // - bullets -> bullet char
            .replaceAll("(?i)^(chào bạn[!,.]?|hello[!,.]?|hi there[!,.]?|dưới đây là[^\n]*\n?)\\s*", "")
            .trim();
    }

    private String extractCleanJson(String input) {
        if (input == null) return "{}";
        String trimmed = input.trim();
        // Remove reasoning tags <think>...</think>
        trimmed = trimmed.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }
}
