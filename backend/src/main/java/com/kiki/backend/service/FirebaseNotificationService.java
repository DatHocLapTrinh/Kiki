package com.kiki.backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.kiki.backend.config.FirebaseConfig;
import com.kiki.backend.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseNotificationService.class);

    private final FirebaseConfig firebaseConfig;

    public FirebaseNotificationService(FirebaseConfig firebaseConfig) {
        this.firebaseConfig = firebaseConfig;
    }

    public String sendPushNotification(NotificationRequest request) {
        if (!firebaseConfig.isInitialized() || FirebaseApp.getApps().isEmpty()) {
            log.info("📢 [DEV MOCK FCM] Sending notification to token '{}': [{}] {}",
                request.fcmToken(), request.title(), request.body());
            return "mock-fcm-message-id-" + System.currentTimeMillis();
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                .setToken(request.fcmToken())
                .setNotification(Notification.builder()
                    .setTitle(request.title())
                    .setBody(request.body())
                    .build());

            if (request.targetScreen() != null && !request.targetScreen().isEmpty()) {
                messageBuilder.putData("extra_target_screen", request.targetScreen());
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("✅ FCM Notification sent successfully, response ID: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to send FCM notification: {}", e.getMessage());
            throw new RuntimeException("Gửi thông báo qua FCM thất bại: " + e.getMessage(), e);
        }
    }
}
