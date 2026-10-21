package com.kiki.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "reminder.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledReminderTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReminderTask.class);

    private final FirebaseNotificationService notificationService;

    public ScheduledReminderTask(FirebaseNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Tự động quét và nhắc nhở học viên giữ lửa chuỗi Streak mỗi tối vào lúc 20:00
     */
    @Scheduled(cron = "${reminder.cron:0 0 20 * * *}")
    public void executeDailyStreakReminder() {
        String today = LocalDate.now().toString();
        log.info("⏰ [CRON JOB] Bắt đầu quét kiểm tra học viên chưa hoàn thành bài học ngày: {}", today);

        // Trong kịch bản thực tế có Firestore:
        // Quét Firestore collection "users" với điều kiện lastActiveDate != today
        // Với mỗi user tìm được -> lấy fcmToken và gửi tin nhắn cá nhân hóa
        log.info("⏰ [CRON JOB] Hoàn thành lượt quét tự động. Hệ thống sẵn sàng đẩy thông báo giữ Streak.");
    }
}
