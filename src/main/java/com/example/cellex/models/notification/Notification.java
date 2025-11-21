package com.example.cellex.models.notification;

import com.example.cellex.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;  // null nếu là broadcast notification
    
    private String title;
    
    private String message;
    
    private NotificationType type;
    
    @Field("is_read")
    @Builder.Default
    private Boolean isRead = false;
    
    @Field("read_at")
    private LocalDateTime readAt;
    
    @Field("is_broadcast")
    @Builder.Default
    private Boolean isBroadcast = false;  // true nếu gửi toàn hệ thống
    
    // Metadata lưu thông tin bổ sung (orderId, productId, etc.)
    private String metadata;
    
    // URL để navigate khi click notification
    @Field("action_url")
    private String actionUrl;
    
    // URL của image cho notification
    @Field("image_url")
    private String imageUrl;
    
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("expires_at")
    private LocalDateTime expiresAt;  // Thời hạn của notification
}
