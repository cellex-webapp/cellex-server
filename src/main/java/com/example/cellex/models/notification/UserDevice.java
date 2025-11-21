package com.example.cellex.models.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "user_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Indexed(unique = true)
    @Field("fcm_token")
    private String fcmToken;  // Firebase Cloud Messaging token
    
    @Field("device_type")
    private String deviceType;  // WEB, ANDROID, IOS
    
    @Field("device_name")
    private String deviceName;
    
    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("last_used_at")
    private LocalDateTime lastUsedAt;
}
