package com.kiki.backend.controller;

import com.kiki.backend.dto.NotificationRequest;
import com.kiki.backend.security.UserPrincipal;
import com.kiki.backend.service.FirebaseNotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final FirebaseNotificationService notificationService;

    public NotificationController(FirebaseNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send-test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody NotificationRequest request
    ) {
        String requester = principal != null ? principal.email() : "admin";
        log.info("Sending test push notification requested by [{}]: {}", requester, request.title());

        String messageId = notificationService.sendPushNotification(request);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "messageId", messageId,
            "title", request.title(),
            "targetToken", request.fcmToken()
        ));
    }
}
