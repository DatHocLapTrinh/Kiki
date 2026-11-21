package com.kiki.backend.controller;

import com.kiki.backend.dto.CosmicCallRequest;
import com.kiki.backend.dto.CosmicCallResponse;
import com.kiki.backend.security.UserPrincipal;
import com.kiki.backend.service.CosmicCallService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai/call")
public class CosmicCallController {

    private static final Logger log = LoggerFactory.getLogger(CosmicCallController.class);

    private final CosmicCallService cosmicCallService;

    public CosmicCallController(CosmicCallService cosmicCallService) {
        this.cosmicCallService = cosmicCallService;
    }

    @PostMapping("/converse")
    public ResponseEntity<CosmicCallResponse> converse(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CosmicCallRequest request
    ) {
        String requester = principal != null ? principal.email() : "anonymous";
        log.info("Cosmic Call 1-1 conversation from user [{}]: User said: '{}'", requester, request.userAnswer());

        CosmicCallResponse response = cosmicCallService.converseAndEvaluate(request);
        return ResponseEntity.ok(response);
    }
}
