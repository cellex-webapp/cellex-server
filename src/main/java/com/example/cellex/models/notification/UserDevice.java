package com.example.cellex.models.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "user_id", nullable = false)
    @JsonIgnore
    private UUID userUuid;

    @Column(name = "fcm_token", unique = true, nullable = false)
    private String fcmToken;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "device_name")
    private String deviceName;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

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
