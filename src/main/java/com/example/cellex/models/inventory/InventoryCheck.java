package com.example.cellex.models.inventory;

import com.example.cellex.enums.InventoryCheckStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_checks", indexes = {
        @Index(name = "idx_inventory_checks_shop_id", columnList = "shop_id"),
        @Index(name = "idx_inventory_checks_status", columnList = "status"),
        @Index(name = "idx_inventory_checks_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryCheckStatus status = InventoryCheckStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    @JsonIgnore
    private UUID createdByUuid;

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

    public void setShopId(String shopId) {
        this.shopUuid = (shopId != null && !shopId.isBlank()) ? UUID.fromString(shopId) : null;
    }

    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdByUuid != null ? createdByUuid.toString() : null;
    }

    public void setCreatedBy(String createdBy) {
        this.createdByUuid = (createdBy != null && !createdBy.isBlank()) ? UUID.fromString(createdBy) : null;
    }
}
