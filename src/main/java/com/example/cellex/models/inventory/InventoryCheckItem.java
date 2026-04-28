package com.example.cellex.models.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "inventory_check_items", indexes = {
        @Index(name = "idx_inventory_check_items_check_id", columnList = "check_id"),
        @Index(name = "idx_inventory_check_items_sku_id", columnList = "sku_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_check_items_check_sku", columnNames = {"check_id", "sku_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "check_id", nullable = false)
    @JsonIgnore
    private UUID checkUuid;

    @Column(name = "sku_id", nullable = false)
    @JsonIgnore
    private UUID skuUuid;

    @Column(name = "system_stock", nullable = false)
    private Integer systemStock;

    @Column(name = "actual_stock", nullable = false)
    private Integer actualStock;

    @Column(name = "difference", nullable = false)
    private Integer difference;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    public void setId(String id) {
        this.uuid = (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    @JsonProperty("checkId")
    public String getCheckId() {
        return checkUuid != null ? checkUuid.toString() : null;
    }

    public void setCheckId(String checkId) {
        this.checkUuid = (checkId != null && !checkId.isBlank()) ? UUID.fromString(checkId) : null;
    }

    @JsonProperty("skuId")
    public String getSkuId() {
        return skuUuid != null ? skuUuid.toString() : null;
    }

    public void setSkuId(String skuId) {
        this.skuUuid = (skuId != null && !skuId.isBlank()) ? UUID.fromString(skuId) : null;
    }
}
