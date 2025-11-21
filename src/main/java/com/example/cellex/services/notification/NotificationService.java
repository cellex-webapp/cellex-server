package com.example.cellex.services.notification;

import com.example.cellex.enums.NotificationType;
import com.example.cellex.models.notification.Notification;
import com.example.cellex.models.notification.UserDevice;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.notification.NotificationRepository;
import com.example.cellex.repositories.notification.UserDeviceRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;

    /**
     * Tạo và gửi notification cho một user cụ thể
     */
    @Async
    public void sendNotificationToUser(
            User user,
            String title,
            String message,
            NotificationType type,
            String metadata,
            String actionUrl,
            String imageUrl
    ) {
        // Lưu notification vào database
        Notification notification = Notification.builder()
                .userId(user.getId())
                .title(title)
                .message(message)
                .type(type)
                .metadata(metadata)
                .actionUrl(actionUrl)
                .imageUrl(imageUrl)
                .isBroadcast(false)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        notificationRepository.save(notification);

        // Gửi push notification qua FCM
        sendPushNotificationToUser(user.getId(), title, message, metadata, actionUrl, imageUrl);
    }

    /**
     * Tạo và gửi broadcast notification đến toàn hệ thống (admin only)
     */
    @Async
    public void sendBroadcastNotification(
            String title,
            String message,
            NotificationType type,
            String metadata,
            String actionUrl,
            String imageUrl,
            LocalDateTime expiresAt
    ) {
        // Lưu broadcast notification vào database
        Notification notification = Notification.builder()
                .userId(null)
                .title(title)
                .message(message)
                .type(type)
                .metadata(metadata)
                .actionUrl(actionUrl)
                .imageUrl(imageUrl)
                .isBroadcast(true)
                .isRead(false)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
        
        notificationRepository.save(notification);

        // Gửi push notification đến tất cả devices
        sendPushNotificationToAllDevices(title, message, metadata, actionUrl, imageUrl);
    }

    /**
     * Gửi push notification qua FCM cho một user
     */
    private void sendPushNotificationToUser(
            String userId,
            String title, 
            String message,
            String metadata,
            String actionUrl,
            String imageUrl
    ) {
        List<UserDevice> devices = userDeviceRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (devices.isEmpty()) {
            log.warn("⚠️ No active devices found for user: {}", userId);
            return;
        }

        log.info("📱 Found {} active device(s) for user: {}", devices.size(), userId);
        
        List<String> tokens = devices.stream()
                .map(UserDevice::getFcmToken)
                .collect(Collectors.toList());

        sendPushToTokens(tokens, title, message, metadata, actionUrl, imageUrl);
    }

    /**
     * Gửi push notification đến tất cả devices trong hệ thống
     */
    private void sendPushNotificationToAllDevices(
            String title, 
            String message,
            String metadata,
            String actionUrl,
            String imageUrl
    ) {
        List<UserDevice> allDevices = userDeviceRepository.findByIsActiveTrue();
        
        if (allDevices.isEmpty()) {
            log.info("No active devices found in the system");
            return;
        }

        List<String> tokens = allDevices.stream()
                .map(UserDevice::getFcmToken)
                .collect(Collectors.toList());

        // FCM có giới hạn 500 tokens per request, nên chia nhỏ
        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tokens.size());
            List<String> batch = tokens.subList(i, end);
            sendPushToTokens(batch, title, message, metadata, actionUrl, imageUrl);
        }
    }

    /**
     * Gửi push notification đến danh sách FCM tokens
     */
    private void sendPushToTokens(
            List<String> tokens, 
            String title, 
            String message,
            String metadata,
            String actionUrl,
            String imageUrl
    ) {
        if (tokens == null || tokens.isEmpty()) {
            log.warn("⚠️ No tokens provided to sendPushToTokens");
            return;
        }

        log.info("📤 Sending FCM notification to {} token(s)", tokens.size());
        log.info("📝 Title: {}", title);
        log.info("💬 Message: {}", message);

        try {
            // Build notification payload
            com.google.firebase.messaging.Notification.Builder notificationBuilder = 
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(message);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                notificationBuilder.setImage(imageUrl);
            }

            // Build data payload
            Map<String, String> data = new HashMap<>();
            if (metadata != null) {
                data.put("metadata", metadata);
            }
            if (actionUrl != null) {
                data.put("actionUrl", actionUrl);
            }
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // Build multicast message
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .setNotification(notificationBuilder.build())
                    .putAllData(data)
                    .addAllTokens(tokens)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setIcon("/icon-192x192.png")
                                    .setBadge("/badge-72x72.png")
                                    .setRequireInteraction(true)
                                    .build())
                            .build())
                    .build();

            // Send message
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicastMessage);
            
            log.info("📊 FCM Result: Successfully sent {} notifications, {} failures", 
                    response.getSuccessCount(), response.getFailureCount());

            // Handle invalid tokens
            if (response.getFailureCount() > 0) {
                log.warn("⚠️ {} FCM messages failed to send", response.getFailureCount());
                handleFailedTokens(tokens, response);
            }

        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to send FCM notification: {}", e.getMessage());
            log.error("FCM Error Code: {}", e.getMessagingErrorCode());
            log.error("FCM Error Details: ", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Xử lý các tokens bị lỗi (invalid, unregistered)
     */
    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                String token = tokens.get(i);
                Exception exception = responses.get(i).getException();
                
                log.error("❌ Failed to send to token [{}...]: {}", 
                        token.substring(0, Math.min(20, token.length())), 
                        exception.getMessage());
                
                if (exception instanceof FirebaseMessagingException) {
                    FirebaseMessagingException fme = (FirebaseMessagingException) exception;
                    MessagingErrorCode errorCode = fme.getMessagingErrorCode();
                    
                    log.error("FCM Error Code: {}", errorCode);
                    log.error("FCM Error Message: {}", fme.getMessage());
                    
                    // Deactivate invalid tokens
                    if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                        errorCode == MessagingErrorCode.UNREGISTERED) {
                        // Tìm và deactivate device
                        userDeviceRepository.findByFcmToken(token).ifPresent(device -> {
                            device.setIsActive(false);
                            userDeviceRepository.save(device);
                            log.info("🔒 Deactivated invalid token: {}...", token.substring(0, 20));
                        });
                    }
                }
            }
        }
    }

    /**
     * Đăng ký device token cho user
     */
    public UserDevice registerDeviceToken(User user, String fcmToken, String deviceType, String deviceName) {
        Optional<UserDevice> existingDevice = userDeviceRepository.findByFcmToken(fcmToken);
        
        if (existingDevice.isPresent()) {
            UserDevice device = existingDevice.get();
            device.setUserId(user.getId());
            device.setDeviceType(deviceType);
            device.setDeviceName(deviceName);
            device.setIsActive(true);
            device.setLastUsedAt(LocalDateTime.now());
            return userDeviceRepository.save(device);
        }

        UserDevice newDevice = UserDevice.builder()
                .userId(user.getId())
                .fcmToken(fcmToken)
                .deviceType(deviceType)
                .deviceName(deviceName)
                .isActive(true)
                .lastUsedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return userDeviceRepository.save(newDevice);
    }

    /**
     * Hủy đăng ký device token
     */
    public void unregisterDeviceToken(String fcmToken) {
        userDeviceRepository.findByFcmToken(fcmToken).ifPresent(device -> {
            device.setIsActive(false);
            userDeviceRepository.save(device);
        });
    }

    /**
     * Lấy danh sách notifications của user (bao gồm cả broadcast)
     */
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findUserNotifications(user.getId(), LocalDateTime.now(), pageable);
    }

    /**
     * Đếm số notification chưa đọc
     */
    public Long countUnreadNotifications(User user) {
        return notificationRepository.countUnreadNotifications(user.getId(), LocalDateTime.now());
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    public void markAsRead(String notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify user owns this notification (or it's a broadcast)
        if (!notification.getIsBroadcast() && !notification.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Đánh dấu tất cả notifications là đã đọc
     */
    public int markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
        LocalDateTime now = LocalDateTime.now();
        
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        return unreadNotifications.size();
    }

    /**
     * Xóa notification
     */
    public void deleteNotification(String notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify user owns this notification
        if (!notification.getIsBroadcast() && !notification.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Xóa các notifications đã hết hạn (scheduled job)
     */
    public int deleteExpiredNotifications() {
        List<Notification> expiredNotifications = notificationRepository.findExpiredNotifications(LocalDateTime.now());
        notificationRepository.deleteAll(expiredNotifications);
        int deleted = expiredNotifications.size();
        log.info("Deleted {} expired notifications", deleted);
        return deleted;
    }
}
