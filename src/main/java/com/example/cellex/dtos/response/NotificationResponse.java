package com.example.cellex.dtos.response;

import com.example.cellex.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private String id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Boolean isBroadcast;
    private String metadata;
    private String actionUrl;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
