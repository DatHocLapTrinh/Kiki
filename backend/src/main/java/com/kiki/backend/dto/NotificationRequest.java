package com.kiki.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
    @NotBlank(message = "FCM Token không được để trống")
    String fcmToken,

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    String title,

    @NotBlank(message = "Nội dung thông báo không được để trống")
    String body,

    String targetScreen
) {}
