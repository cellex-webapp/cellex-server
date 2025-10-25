package com.example.cellex.models;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.DiscountType;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_coupons")
@CompoundIndex(name = "user_segment_coupon_idx", def = "{'user_id': 1, 'segment_coupon_id': 1}")
public class UserCoupon {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("segment_coupon_id")
    private String segmentCouponId;

    @Indexed(unique = true)
    @Field("code")
    private String code; // Mã coupon unique cho từng user

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("discount_type")
    private DiscountType discountType;

    @Field("discount_value")
    private Double discountValue;

    @Field("min_order_amount")
    private Double minOrderAmount;

    @Field("issued_date")
    private LocalDateTime issuedDate; // Ngày phát coupon

    @Field("expires_at")
    private LocalDateTime expiresAt; // Ngày hết hạn

    @Field("status")
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @Field("redeemed_order_id")
    private String redeemedOrderId; // ID đơn hàng đã sử dụng coupon

    @Field("redeemed_at")
    private LocalDateTime redeemedAt; // Thời điểm sử dụng coupon

    @Field("issue_reason")
    private String issueReason; // Lý do phát: "UPGRADE", "SCHEDULED", "MANUAL"

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

