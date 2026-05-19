package com.example.cellex.models.shop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shop_staff_members")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStaffMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Column(name = "user_id", nullable = false)
    @JsonIgnore
    private UUID userUuid;

    @Column(name = "shop_role_id", nullable = false)
    @JsonIgnore
    private UUID shopRoleUuid;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    @JsonProperty("shopId")
    public String getShopId() {
        return shopUuid != null ? shopUuid.toString() : null;
    }

    @JsonProperty("userId")
    public String getUserId() {
        return userUuid != null ? userUuid.toString() : null;
    }

    @JsonProperty("shopRoleId")
    public String getShopRoleId() {
        return shopRoleUuid != null ? shopRoleUuid.toString() : null;
    }
}

