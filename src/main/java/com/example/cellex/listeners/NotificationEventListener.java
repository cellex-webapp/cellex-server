package com.example.cellex.listeners;

import com.example.cellex.models.events.NotificationEvent;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * Lắng nghe sự kiện NotificationEvent được bắn ra từ NotificationService hoặc ChatService
     * * Luồng hoạt động:
     * 1. Service gốc (Chat/Notification) xử lý Logic + Gửi FCM.
     * 2. Service gốc bắn sự kiện (publishEvent).
     * 3. Listener này bắt sự kiện và gửi Email (chạy bất đồng bộ).
     * * @Async đảm bảo việc gửi mail chạy ở thread riêng, không làm chậm phản hồi của API
     */
    @Async("taskExecutor") 
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("🔔 [EventListener] Received event: {}", event.getTitle());

        // 1. Kiểm tra ID người nhận
        if (event.getRecipientId() == null) {
            log.warn("⚠️ Event ignored: Recipient ID is null");
            return;
        }

        // 2. Lấy thông tin User mới nhất từ DB (để lấy email chính xác)
        User user = userRepository.findById(event.getRecipientId()).orElse(null);
        
        // 3. Validate User và Email
        if (user == null) {
            log.warn("❌ Cannot send email: User not found with ID {}", event.getRecipientId());
            return;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("⚠️ Cannot send email: User {} has no email address", user.getId());
            return;
        }

        // 4. Gọi EmailService để gửi mail
        // Mapping dữ liệu từ Event sang tham số của EmailService
        try {
            emailService.sendNotificationEmail(
                    user.getEmail(),                // To
                    "[Cellex] " + event.getTitle(), // Subject (Thêm prefix để user dễ nhận diện)
                    event.getMessage(),             // Content
                    event.getActionUrl(),           // Action URL (Link trỏ về web)
                    user.getFullName()              // User Name (để chào hỏi trong mail)
            );
        } catch (Exception e) {
            // Catch all exception ở đây để đảm bảo thread async không bị chết âm thầm
            log.error("❌ Unexpected error in NotificationEventListener: {}", e.getMessage(), e);
        }
    }
}