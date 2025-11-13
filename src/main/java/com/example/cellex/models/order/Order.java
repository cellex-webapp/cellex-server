package com.example.cellex.models.order;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed
    @Field("shop_id")
    private String shopId;

    @Field("shop_name")
    private String shopName;

    @Field("items")
    private List<OrderItem> items;

    // Địa chỉ giao hàng
    @Field("shipping_address")
    private ShippingAddress shippingAddress;

    // Thông tin giá
    @Field("subtotal")
    private Double subtotal; // Tổng tiền sản phẩm

    @Field("shipping_fee")
    @Builder.Default
    private Double shippingFee = 0.0; // Phí vận chuyển

    @Field("discount_amount")
    @Builder.Default
    private Double discountAmount = 0.0; // Số tiền giảm giá từ coupon

    @Field("total_amount")
    private Double totalAmount; // Tổng tiền phải trả

    // Thông tin coupon
    @Field("coupon_code")
    private String couponCode;

    @Field("user_coupon_id")
    private String userCouponId;

    // Thông tin thanh toán
    @Field("payment_method")
    private PaymentMethod paymentMethod;

    @Field("is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    @Field("paid_at")
    private LocalDateTime paidAt;

    // Trạng thái đơn hàng
    @Field("status")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Field("status_history")
    private List<StatusHistory> statusHistory;

    @Field("note")
    private String note; // Ghi chú từ khách hàng

    @Field("is_from_cart")
    @Builder.Default
    private Boolean isFromCart = false; // Đánh dấu đơn hàng được tạo từ giỏ hàng

    @Field("cancel_reason")
    private String cancelReason; // Lý do hủy đơn

    @Field("cancelled_at")
    private LocalDateTime cancelledAt;

    @Field("confirmed_at")
    private LocalDateTime confirmedAt;

    @Field("shipping_at")
    private LocalDateTime shippingAt;

    @Field("delivered_at")
    private LocalDateTime deliveredAt;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        @Field("province_code")
        private String provinceCode;

        @Field("province_name")
        private String provinceName;

        @Field("commune_code")
        private String communeCode;

        @Field("commune_name")
        private String communeName;

        @Field("detail_address")
        private String detailAddress;

        @Field("full_address")
        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistory {
        @Field("status")
        private OrderStatus status;

        @Field("note")
        private String note;

        @Field("updated_by")
        private String updatedBy; // user_id hoặc vendor_id

        @Field("updated_at")
        private LocalDateTime updatedAt;
    }
}
