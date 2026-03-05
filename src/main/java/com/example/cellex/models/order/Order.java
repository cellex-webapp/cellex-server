package com.example.cellex.models.order;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.PaymentMethod;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for the 'orders' table in PostgreSQL (Supabase).
 * Migrated from MongoDB @Document. Same package and class name
 * to preserve backward compatibility across 18+ dependent files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_shop_id", columnList = "shop_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_order_code", columnList = "order_code"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "order_code", unique = true)
    private String orderCode;

    @Column(name = "user_id", nullable = false)
    @JsonIgnore
    private UUID userUuid;

    @Column(name = "shop_id", nullable = false)
    @JsonIgnore
    private UUID shopUuid;

    @Column(name = "shop_name")
    private String shopName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address_json", columnDefinition = "jsonb")
    private ShippingAddress shippingAddress;

    // Money fields — BigDecimal internally, Double backward-compat getters
    @Column(name = "subtotal", precision = 15, scale = 2)
    @JsonIgnore
    private BigDecimal subtotalDecimal;

    @Column(name = "shipping_fee", precision = 15, scale = 2)
    @JsonIgnore
    @Builder.Default
    private BigDecimal shippingFeeDecimal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @JsonIgnore
    @Builder.Default
    private BigDecimal discountAmountDecimal = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    @JsonIgnore
    private BigDecimal totalAmountDecimal;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "user_coupon_id", length = 50)
    private String userCouponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // VNPay payment fields
    @Column(name = "vnpay_transaction_id", length = 100)
    private String vnpayTransactionId;

    @Column(name = "vnpay_response_code", length = 20)
    private String vnpayResponseCode;

    @Column(name = "vnpay_bank_code", length = 20)
    private String vnpayBankCode;

    @Column(name = "vnpay_pay_date", length = 50)
    private String vnpayPayDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_history_json", columnDefinition = "jsonb")
    @Builder.Default
    private List<StatusHistory> statusHistory = new ArrayList<>();

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "is_from_cart")
    @Builder.Default
    private Boolean isFromCart = false;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipping_at")
    private LocalDateTime shippingAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Backward-compatible ID accessors ====================

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    public void setId(String id) {
        this.uuid = (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    @JsonProperty("userId")
    public String getUserId() {
        return userUuid != null ? userUuid.toString() : null;
    }

    public void setUserId(String userId) {
        this.userUuid = (userId != null && !userId.isBlank()) ? UUID.fromString(userId) : null;
    }

    @JsonProperty("shopId")
    public String getShopId() {
        return shopUuid != null ? shopUuid.toString() : null;
    }

    public void setShopId(String shopId) {
        this.shopUuid = (shopId != null && !shopId.isBlank()) ? UUID.fromString(shopId) : null;
    }

    // ==================== Backward-compatible money accessors ====================

    @JsonProperty("subtotal")
    public Double getSubtotal() {
        return subtotalDecimal != null ? subtotalDecimal.doubleValue() : null;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotalDecimal = subtotal != null ? BigDecimal.valueOf(subtotal) : null;
    }

    @JsonProperty("shippingFee")
    public Double getShippingFee() {
        return shippingFeeDecimal != null ? shippingFeeDecimal.doubleValue() : null;
    }

    public void setShippingFee(Double shippingFee) {
        this.shippingFeeDecimal = shippingFee != null ? BigDecimal.valueOf(shippingFee) : null;
    }

    @JsonProperty("discountAmount")
    public Double getDiscountAmount() {
        return discountAmountDecimal != null ? discountAmountDecimal.doubleValue() : null;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmountDecimal = discountAmount != null ? BigDecimal.valueOf(discountAmount) : null;
    }

    @JsonProperty("totalAmount")
    public Double getTotalAmount() {
        return totalAmountDecimal != null ? totalAmountDecimal.doubleValue() : null;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmountDecimal = totalAmount != null ? BigDecimal.valueOf(totalAmount) : null;
    }

    // ==================== Inner classes (JSONB serialized) ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private String provinceCode;
        private String provinceName;
        private String communeCode;
        private String communeName;
        private String detailAddress;
        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistory {
        private OrderStatus status;
        private String note;
        private String updatedBy;
        private LocalDateTime updatedAt;
    }
}
