package com.example.cellex.services.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduledTasks {

    private final NotificationService notificationService;

    /**
     * Tự động xóa các notifications đã hết hạn
     * Chạy mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredNotifications() {
        log.info("Starting cleanup of expired notifications");
        int deleted = notificationService.deleteExpiredNotifications();
        log.info("Cleanup completed. Deleted {} expired notifications", deleted);
    }
}
