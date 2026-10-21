package com.kiki.backend.controller;

import com.kiki.backend.dto.ChapterGeneratedResponse;
import com.kiki.backend.dto.GenerateChapterRequest;
import com.kiki.backend.security.UserPrincipal;
import com.kiki.backend.service.AiGenerationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai")
public class AiCurriculumController {

    private static final Logger log = LoggerFactory.getLogger(AiCurriculumController.class);

    private final AiGenerationService aiGenerationService;

    public AiCurriculumController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @PostMapping("/generate-chapter")
    public ResponseEntity<ChapterGeneratedResponse> generateChapter(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody GenerateChapterRequest request
    ) throws IOException {
        String requester = principal != null ? principal.email() : "anonymous";
        log.info("Generating customized chapter for user [{}] with level [{}] and topic [{}]",
            requester, request.level(), request.topic());

        ChapterGeneratedResponse response = aiGenerationService.generateChapter(request);
        return ResponseEntity.ok(response);
    }
}
