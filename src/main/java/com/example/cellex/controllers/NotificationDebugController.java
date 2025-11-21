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
}
