package com.example.cellex.models.shop;

import com.example.cellex.enums.StaffInvitationStatus;
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
@Table(name = "shop_staff_invitations")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStaffInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Column(name = "shop_role_id", nullable = false)
    @JsonIgnore
    private UUID shopRoleUuid;

    @Column(name = "invited_user_id", nullable = false)
    @JsonIgnore
    private UUID invitedUserUuid;

    @Column(name = "invited_by", nullable = false)
    @JsonIgnore
    private UUID invitedByUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StaffInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

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

    public void setId(String id) {
        this.uuid = (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    @JsonProperty("shopId")
    public String getShopId() {
        return shopUuid != null ? shopUuid.toString() : null;
    }

    @JsonProperty("shopRoleId")
    public String getShopRoleId() {
        return shopRoleUuid != null ? shopRoleUuid.toString() : null;
    }

    @JsonProperty("invitedUserId")
    public String getInvitedUserId() {
        return invitedUserUuid != null ? invitedUserUuid.toString() : null;
    }
}

