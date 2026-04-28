package com.example.cellex.models.inventory;

import com.example.cellex.enums.InventoryTransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_inventory_transactions_sku_id", columnList = "sku_id"),
        @Index(name = "idx_inventory_transactions_type", columnList = "type"),
        @Index(name = "idx_inventory_transactions_reference_id", columnList = "reference_id"),
        @Index(name = "idx_inventory_transactions_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "sku_id", nullable = false)
    @JsonIgnore
    private UUID skuUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private InventoryTransactionType type;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    public void setId(String id) {
        this.uuid = (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    @JsonProperty("skuId")
    public String getSkuId() {
        return skuUuid != null ? skuUuid.toString() : null;
    }

    public void setSkuId(String skuId) {
        this.skuUuid = (skuId != null && !skuId.isBlank()) ? UUID.fromString(skuId) : null;
    }
}
