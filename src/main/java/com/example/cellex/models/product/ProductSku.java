package com.example.cellex.models.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_skus", indexes = {
        @Index(name = "idx_product_skus_product_id", columnList = "product_id"),
        @Index(name = "idx_product_skus_shop_id", columnList = "shop_id"),
        @Index(name = "idx_product_skus_sku_code", columnList = "sku_code"),
        @Index(name = "idx_product_skus_low_stock", columnList = "on_hand_stock, reserved_stock, safety_stock")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_skus_sku_code", columnNames = "sku_code"),
        @UniqueConstraint(name = "uk_product_skus_product_variation", columnNames = {"product_id", "variation_hash"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "product_id", nullable = false, length = 120)
    private String productId;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Column(name = "sku_code", nullable = false, length = 120, unique = true)
    private String skuCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variation_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> variationData = new LinkedHashMap<>();

    @Column(name = "variation_hash", nullable = false, length = 128)
    private String variationHash;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    @JsonIgnore
    private BigDecimal priceDecimal;

    @Column(name = "on_hand_stock", nullable = false)
    @Builder.Default
    private Integer onHandStock = 0;

    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private Integer reservedStock = 0;

    @Column(name = "safety_stock", nullable = false)
    @Builder.Default
    private Integer safetyStock = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    @JsonProperty("price")
    public Double getPrice() {
        return priceDecimal != null ? priceDecimal.doubleValue() : null;
    }

    public void setPrice(Double price) {
        this.priceDecimal = price != null ? BigDecimal.valueOf(price) : null;
    }

    @JsonProperty("stockQuantity")
    public Integer getStockQuantity() {
        return getAvailableStock();
    }

    @JsonProperty("availableStock")
    public Integer getAvailableStock() {
        int onHand = onHandStock != null ? onHandStock : 0;
        int reserved = reservedStock != null ? reservedStock : 0;
        return onHand - reserved;
    }
}
