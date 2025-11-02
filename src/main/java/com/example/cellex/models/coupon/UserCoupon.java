package com.example.cellex.models.coupon;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.IssuedVia;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_coupons")
@CompoundIndex(name = "user_segment_coupon_idx", def = "{'user_id': 1, 'segment_coupon_id': 1}")
@CompoundIndex(name = "user_campaign_idx", def = "{'user_id': 1, 'campaign_id': 1}")
@CompoundIndex(name = "user_code_idx", def = "{'user_id': 1, 'code': 1}", unique = true) // Unique per user+code combo (cho SHARED_CODE)
public class UserCoupon {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("segment_coupon_id")
    private String segmentCouponId; // Cho segment coupon (có thể null nếu từ campaign)

    @Field("campaign_id")
    private String campaignId; // Cho campaign coupon (có thể null nếu từ segment)

    @Indexed(name = "code_idx") // Đặt tên khác để tránh conflict với index cũ
    @Field("code")
    private String code; // Mã coupon (unique per user cho SEGMENT, shared cho CAMPAIGN SHARED_CODE)

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("coupon_type")
    private CouponType couponType;

    @Field("discount_value")
    private Double discountValue;

    @Field("min_order_amount")
    private Double minOrderAmount;

    // Áp dụng cho sản phẩm/danh mục cụ thể (từ campaign)
    @Field("applicable_product_ids")
    private List<String> applicableProductIds;

    @Field("applicable_category_ids")
    private List<String> applicableCategoryIds;

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

    @Field("issued_via")
    private IssuedVia issuedVia; // ADMIN_MANUAL, SCHEDULED, AUTO_ON_UPGRADE, CAMPAIGN

    @Field("issued_by")
    private String issuedBy; // Admin ID nếu phát thủ công

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

