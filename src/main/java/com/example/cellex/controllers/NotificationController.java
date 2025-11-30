package com.example.cellex.controllers;

import com.example.cellex.dtos.request.BroadcastNotificationRequest;
import com.example.cellex.dtos.request.DeviceTokenRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.NotificationListResponse;
import com.example.cellex.dtos.response.NotificationResponse;
import com.example.cellex.models.notification.Notification;
import com.example.cellex.models.notification.UserDevice;
import com.example.cellex.models.user.User;
import com.example.cellex.services.S3Service;
import com.example.cellex.services.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "14. Notification", description = "Push notification APIs")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final S3Service s3Service;

    @PostMapping(value = "/broadcast", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send broadcast notification to all users (Admin only)")
    @RequestBody(
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = BroadcastNotificationRequest.class),
            encoding = @Encoding(name = "imageFile", contentType = "application/octet-stream")
        )
    )
    public ResponseEntity<ApiResponse<Void>> sendBroadcastNotification(
            @Valid @ModelAttribute BroadcastNotificationRequest request
    ) {
        // Chỉ upload file (không còn truyền trực tiếp imageUrl)
        String finalImageUrl = null;
        try {
            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                finalImageUrl = s3Service.uploadFile(request.getImageFile(), "notifications");
            }
        } catch (Exception e) {
            // Nếu upload thất bại, log và tiếp tục (không throw)
            e.printStackTrace();
        }

        notificationService.sendBroadcastNotification(
                request.getTitle(),
                request.getMessage(),
                request.getType(),
                request.getMetadata(),
                request.getActionUrl(),
                finalImageUrl,
                request.getExpiresAt()
        );

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .code(1000)
                .message("Broadcast notification sent successfully")
                .build()
        );
    }

    @PostMapping("/device-token")
    @Operation(summary = "Register device FCM token")
    public ResponseEntity<ApiResponse<Map<String, String>>> registerDeviceToken(
            @AuthenticationPrincipal User user,
            @Valid @org.springframework.web.bind.annotation.RequestBody DeviceTokenRequest request
    ) {
        // Debug log
        System.out.println("========================================");
        System.out.println("📱 Register Device Token Request");
        System.out.println("User: " + (user != null ? user.getEmail() : "null"));
        System.out.println("Request object: " + request);
        System.out.println("FCM Token: " + request.getFcmToken());
        System.out.println("Device Type: " + request.getDeviceType());
        System.out.println("Device Name: " + request.getDeviceName());
        System.out.println("========================================");
        
        UserDevice device = notificationService.registerDeviceToken(
                user,
                request.getFcmToken(),
                request.getDeviceType(),
                request.getDeviceName()
        );

        Map<String, String> result = new HashMap<>();
        result.put("deviceId", device.getId());
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, String>>builder()
                .code(1000)
                .message("Device token registered successfully")
                .result(result)
                .build()
        );
    }

    @DeleteMapping("/device-token/{fcmToken}")
    @Operation(summary = "Unregister device FCM token")
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(
            @PathVariable String fcmToken
    ) {
        notificationService.unregisterDeviceToken(fcmToken);

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .code(1000)
                .message("Device token unregistered successfully")
                .build()
        );
    }

    @GetMapping
    @Operation(summary = "Get user notifications with pagination")
    public ResponseEntity<ApiResponse<NotificationListResponse>> getUserNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationService.getUserNotifications(user, pageable);
        Long unreadCount = notificationService.countUnreadNotifications(user);

        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        NotificationListResponse result = NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .currentPage(notificationPage.getNumber())
                .totalPages(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .build();

        return ResponseEntity.ok(
            ApiResponse.<NotificationListResponse>builder()
                .code(1000)
                .message("Notifications retrieved successfully")
                .result(result)
                .build()
        );
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user
    ) {
        Long unreadCount = notificationService.countUnreadNotifications(user);

        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Long>>builder()
                .code(1000)
                .message("Unread count retrieved successfully")
                .result(result)
                .build()
        );
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id
    ) {
        notificationService.markAsRead(id, user);

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .code(1000)
                .message("Notification marked as read")
                .build()
        );
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal User user
    ) {
        int count = notificationService.markAllAsRead(user);

        Map<String, Integer> result = new HashMap<>();
        result.put("count", count);
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Integer>>builder()
                .code(1000)
                .message("All notifications marked as read")
                .result(result)
                .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable String id
    ) {
        notificationService.deleteNotification(id, user);

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .code(1000)
                .message("Notification deleted successfully")
                .build()
        );
    }

    // Helper method to convert entity to response DTO
    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .isBroadcast(notification.getIsBroadcast())
                .metadata(notification.getMetadata())
                .actionUrl(notification.getActionUrl())
                .imageUrl(notification.getImageUrl())
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }
}
