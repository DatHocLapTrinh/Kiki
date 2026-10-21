package com.kiki.backend.controller;

import com.kiki.backend.dto.MentorChatRequest;
import com.kiki.backend.dto.MentorChatResponse;
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
public class AiMentorController {

    private static final Logger log = LoggerFactory.getLogger(AiMentorController.class);

    private final AiGenerationService aiGenerationService;

    public AiMentorController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @PostMapping("/mentor-chat")
    public ResponseEntity<MentorChatResponse> mentorChat(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody MentorChatRequest request
    ) throws IOException {
        String requester = principal != null ? principal.email() : "anonymous";
        log.info("Socratic mentor chat query from user [{}]: {}", requester, request.message());

        MentorChatResponse response = aiGenerationService.mentorChat(request);
        return ResponseEntity.ok(response);
    }
}
