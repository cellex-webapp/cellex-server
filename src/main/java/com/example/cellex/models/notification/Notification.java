package com.example.cellex.models.notification;

import com.example.cellex.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "user_id")
    @JsonIgnore
    private UUID userUuid;

    private String title;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType type;

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder.Default
    @Column(name = "is_broadcast")
    private Boolean isBroadcast = false;

    private String metadata;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // --- Backward-compat String-based ID accessors ---

    @JsonProperty("id")
    public String getId() { return uuid != null ? uuid.toString() : null; }

    public void setId(String id) { this.uuid = id != null ? UUID.fromString(id) : null; }

    @JsonProperty("userId")
    public String getUserId() { return userUuid != null ? userUuid.toString() : null; }

    public void setUserId(String userId) {
        this.userUuid = (userId != null && !userId.isEmpty()) ? UUID.fromString(userId) : null;
    }
}
