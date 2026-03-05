package com.example.cellex.models.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification_reads",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "user_id", nullable = false)
    @JsonIgnore
    private UUID userUuid;

    @Column(name = "notification_id", nullable = false)
    @JsonIgnore
    private UUID notificationUuid;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    // --- Backward-compat String-based accessors ---

    @JsonProperty("id")
    public String getId() { return uuid != null ? uuid.toString() : null; }

    public void setId(String id) { this.uuid = id != null ? UUID.fromString(id) : null; }

    @JsonProperty("userId")
    public String getUserId() { return userUuid != null ? userUuid.toString() : null; }

    public void setUserId(String userId) {
        this.userUuid = (userId != null && !userId.isEmpty()) ? UUID.fromString(userId) : null;
    }

    @JsonProperty("notificationId")
    public String getNotificationId() { return notificationUuid != null ? notificationUuid.toString() : null; }

    public void setNotificationId(String notificationId) {
        this.notificationUuid = (notificationId != null && !notificationId.isEmpty()) ? UUID.fromString(notificationId) : null;
    }
}
