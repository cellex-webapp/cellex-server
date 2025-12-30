package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.enums.NotificationType;
import com.example.cellex.models.notification.UserDevice;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.notification.UserDeviceRepository;
import com.example.cellex.services.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/debug")
@RequiredArgsConstructor
@Tag(name = "Notification Debug", description = "Debug endpoints for testing notifications")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class NotificationDebugController {

    private final NotificationService notificationService;
    private final UserDeviceRepository userDeviceRepository;

    @GetMapping("/my-devices")
    @Operation(summary = "Get all devices registered for current user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyDevices(@AuthenticationPrincipal User user) {
        List<UserDevice> devices = userDeviceRepository.findByUserId(user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("email", user.getEmail());
        result.put("deviceCount", devices.size());
        result.put("devices", devices);
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Devices retrieved successfully")
                .result(result)
                .build()
        );
    }

    @GetMapping("/all-devices")
    @Operation(summary = "Get all active devices in system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllDevices() {
        List<UserDevice> devices = userDeviceRepository.findByIsActiveTrue();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalActiveDevices", devices.size());
        result.put("devices", devices);
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("All devices retrieved successfully")
                .result(result)
                .build()
        );
    }

    @PostMapping("/test-notification")
    @Operation(summary = "Send a test notification to current user")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendTestNotification(@AuthenticationPrincipal User user) {
        log.info("Sending test notification to user: {}", user.getEmail());
        
        try {
            notificationService.sendNotificationToUser(
                user,
                "Test Notification",
                "This is a test notification from Cellex",
                NotificationType.SYSTEM,
                "{\"test\": true}",
                "/test",
                null
            );
            
            Map<String, String> result = new HashMap<>();
            result.put("userId", user.getId());
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>builder()
                    .code(1000)
                    .message("Test notification sent successfully")
                    .result(result)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error sending test notification", e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.<Map<String, String>>builder()
                    .code(5000)
                    .message("Error sending test notification: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping("/test-broadcast")
    @Operation(summary = "Send a test broadcast notification")
    public ResponseEntity<ApiResponse<Void>> sendTestBroadcast() {
        log.info("Sending test broadcast notification");
        
        try {
            notificationService.sendBroadcastNotification(
                "Test Broadcast",
                "This is a test broadcast notification",
                NotificationType.SYSTEM,
                "{\"test\": true}",
                "/test",
                null,
                null
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .code(1000)
                    .message("Test broadcast sent successfully")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error sending test broadcast", e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.<Void>builder()
                    .code(5000)
                    .message("Error sending test broadcast: " + e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping("/firebase-status")
    @Operation(summary = "Check Firebase initialization status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkFirebaseStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
            result.put("firebaseInitialized", true);
            result.put("appName", app.getName());
            result.put("projectId", app.getOptions().getProjectId());
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .code(1000)
                    .message("Firebase status retrieved successfully")
                    .result(result)
                    .build()
            );
        } catch (IllegalStateException e) {
            result.put("firebaseInitialized", false);
            result.put("error", "Firebase not initialized: " + e.getMessage());
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .code(5000)
                    .message("Firebase not initialized")
                    .result(result)
                    .build()
            );
        }
    }

    @DeleteMapping("/cleanup-devices")
    @Operation(summary = "Remove inactive devices for current user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupDevices(@AuthenticationPrincipal User user) {
        List<UserDevice> devices = userDeviceRepository.findByUserId(user.getId());
        
        int removedCount = 0;
        int keptCount = 0;
        
        for (UserDevice device : devices) {
            if (!device.getIsActive()) {
                userDeviceRepository.delete(device);
                removedCount++;
                log.info("🗑️ Removed inactive device: {}", device.getId());
            } else {
                keptCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("removedDevices", removedCount);
        result.put("activeDevices", keptCount);
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Cleanup completed")
                .result(result)
                .build()
        );
    }

    @PostMapping("/validate-token")
    @Operation(summary = "Validate FCM token by sending a dry-run message")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestParam String fcmToken
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", fcmToken.length() > 30 ? fcmToken.substring(0, 30) + "..." : fcmToken);
        
        try {
            // Build a simple message for dry-run validation
            com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                    .setToken(fcmToken)
                    .putData("validate", "true")
                    .build();
            
            // Send with dryRun=true to validate without actually sending
            String response = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .send(message, true);
            
            result.put("valid", true);
            result.put("response", response);
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .code(1000)
                    .message("Token is valid")
                    .result(result)
                    .build()
            );
        } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
            result.put("valid", false);
            result.put("error", e.getMessage());
            result.put("errorCode", e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().toString() : "UNKNOWN");
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .code(4000)
                    .message("Token validation failed: " + e.getMessage())
                    .result(result)
                    .build()
            );
        }
    }

    @PostMapping("/send-data-only")
    @Operation(summary = "Send a data-only notification (no notification payload)")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendDataOnlyNotification(
            @AuthenticationPrincipal User user
    ) {
        log.info("Sending data-only notification to user: {}", user.getEmail());
        
        try {
            List<UserDevice> devices = userDeviceRepository.findByUserIdAndIsActiveTrue(user.getId());
            
            if (devices.isEmpty()) {
                return ResponseEntity.ok(
                    ApiResponse.<Map<String, String>>builder()
                        .code(4004)
                        .message("No active devices found")
                        .build()
                );
            }

            List<String> tokens = devices.stream()
                    .map(UserDevice::getFcmToken)
                    .collect(java.util.stream.Collectors.toList());
            
            // Build data-only message (no notification payload)
            Map<String, String> data = new HashMap<>();
            data.put("title", "Test Data-Only Message");
            data.put("body", "Đây là tin nhắn data-only để test Service Worker");
            data.put("message", "Đây là tin nhắn data-only để test Service Worker");
            data.put("type", "TEST");
            data.put("actionUrl", "/test");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            com.google.firebase.messaging.MulticastMessage message = com.google.firebase.messaging.MulticastMessage.builder()
                    .putAllData(data)
                    .addAllTokens(tokens)
                    .setWebpushConfig(com.google.firebase.messaging.WebpushConfig.builder()
                            .putHeader("Urgency", "high")
                            .putHeader("TTL", "86400")
                            .build())
                    .build();
            
            com.google.firebase.messaging.BatchResponse response = 
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().sendEachForMulticast(message);
            
            log.info("📊 Data-only Result: {} success, {} failures", 
                    response.getSuccessCount(), response.getFailureCount());
            
            Map<String, String> result = new HashMap<>();
            result.put("successCount", String.valueOf(response.getSuccessCount()));
            result.put("failureCount", String.valueOf(response.getFailureCount()));
            result.put("totalTokens", String.valueOf(tokens.size()));
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>builder()
                    .code(1000)
                    .message("Data-only notification sent")
                    .result(result)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error sending data-only notification", e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.<Map<String, String>>builder()
                    .code(5000)
                    .message("Error: " + e.getMessage())
                    .build()
            );
        }
    }
}
