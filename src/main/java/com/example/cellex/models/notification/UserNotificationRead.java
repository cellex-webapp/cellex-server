package com.example.cellex.models.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Bảng track trạng thái đọc của broadcast notifications cho từng user
 * Giải quyết vấn đề: broadcast notification được share giữa tất cả users
 */
@Document(collection = "user_notification_reads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "user_notification_idx", def = "{'user_id': 1, 'notification_id': 1}", unique = true)
public class UserNotificationRead {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("notification_id")
    private String notificationId;
    
    @Field("read_at")
    private LocalDateTime readAt;
}
