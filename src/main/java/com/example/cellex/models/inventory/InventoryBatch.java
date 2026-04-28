package com.example.cellex.models.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_batches", indexes = {
        @Index(name = "idx_inventory_batches_sku_id", columnList = "sku_id"),
        @Index(name = "idx_inventory_batches_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_inventory_batches_import_date", columnList = "import_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "sku_id", nullable = false)
    @JsonIgnore
    private UUID skuUuid;

    @Column(name = "supplier_id", nullable = false)
    @JsonIgnore
    private UUID supplierUuid;

    @Column(name = "import_price", precision = 15, scale = 2, nullable = false)
    @JsonIgnore
    private BigDecimal importPriceDecimal;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "remain_quantity", nullable = false)
    private Integer remainQuantity;

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "reference_id", length = 120)
    private String referenceId;

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

    @JsonProperty("supplierId")
    public String getSupplierId() {
        return supplierUuid != null ? supplierUuid.toString() : null;
    }

    public void setSupplierId(String supplierId) {
        this.supplierUuid = (supplierId != null && !supplierId.isBlank()) ? UUID.fromString(supplierId) : null;
    }

    @JsonProperty("importPrice")
    public Double getImportPrice() {
        return importPriceDecimal != null ? importPriceDecimal.doubleValue() : null;
    }

    public void setImportPrice(Double importPrice) {
        this.importPriceDecimal = importPrice != null ? BigDecimal.valueOf(importPrice) : null;
    }
}
