package com.example.cellex.models.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for the 'order_items' table in PostgreSQL (Supabase).
 * Migrated from MongoDB embedded document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "sku_id", length = 50)
    private String skuId;

    @Column(name = "sku_code", length = 120)
    private String skuCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_image", columnDefinition = "TEXT")
    private String productImage;

    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    @JsonIgnore
    private BigDecimal priceDecimal;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", precision = 15, scale = 2, nullable = false)
    @JsonIgnore
    private BigDecimal subtotalDecimal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variation_data", columnDefinition = "jsonb")
    private Map<String, String> variationData;

    // ==================== Backward-compatible money accessors ====================

    @JsonProperty("price")
    public Double getPrice() {
        return priceDecimal != null ? priceDecimal.doubleValue() : null;
    }

    public void setPrice(Double price) {
        this.priceDecimal = price != null ? BigDecimal.valueOf(price) : null;
    }

    @JsonProperty("subtotal")
    public Double getSubtotal() {
        return subtotalDecimal != null ? subtotalDecimal.doubleValue() : null;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotalDecimal = subtotal != null ? BigDecimal.valueOf(subtotal) : null;
    }
}

