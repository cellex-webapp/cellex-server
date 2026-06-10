package com.example.cellex.dtos.response.order;

import com.example.cellex.dtos.response.shop.ShopResponse;
import com.example.cellex.dtos.response.user.UserResponse;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String id;

    @JsonProperty("order_code")
    private String orderCode;

    // full user and shop objects - prefer objects to ids
    @JsonProperty("user")
    private UserResponse user;

    @JsonProperty("shop")
    private ShopResponse shop;

    private List<OrderItemResponse> items;

    @JsonProperty("shipping_address")
    private ShippingAddressResponse shippingAddress;

    private Double subtotal;

    @JsonProperty("shipping_fee")
    private Double shippingFee;

    @JsonProperty("discount_amount")
    private Double discountAmount;

    @JsonProperty("total_amount")
    private Double totalAmount;

    @JsonProperty("coupon_code")
    private String couponCode;

    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @JsonProperty("is_paid")
    private Boolean isPaid;

    @JsonProperty("paid_at")
    private LocalDateTime paidAt;

    private OrderStatus status;

    @JsonProperty("status_history")
    private List<StatusHistoryResponse> statusHistory;

    private String note;

    @JsonProperty("cancel_reason")
    private String cancelReason;

    @JsonProperty("cancelled_at")
    private LocalDateTime cancelledAt;

    @JsonProperty("payment_expires_at")
    private LocalDateTime paymentExpiresAt;

    @JsonProperty("confirmed_at")
    private LocalDateTime confirmedAt;

    @JsonProperty("shipping_at")
    private LocalDateTime shippingAt;

    @JsonProperty("delivered_at")
    private LocalDateTime deliveredAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("ghn_order_code")
    private String ghnOrderCode;

    @JsonProperty("tracking_url")
    private String trackingUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        // Primary key of the order_items table — used by the warranty claim feature
        // to link a claim to a specific order item.
        private String id;

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("sku_id")
        private String skuId;

        @JsonProperty("sku_code")
        private String skuCode;

        @JsonProperty("variation_data")
        private Map<String, String> variationData;

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("product_image")
        private String productImage;

        private Double price;

        private Integer quantity;

        private Double subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressResponse {
        private String street; // detailAddress

        private String commune; // communeName

        private String province; // provinceName

        @Builder.Default
        private String country = "Việt Nam";

        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryResponse {
        private OrderStatus status;
        private String note;

        @JsonProperty("updated_by")
        private String updatedBy;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
    }
}
