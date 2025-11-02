package com.example.cellex.dtos.response.coupon;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.IssuedVia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {
    private String id;
    private String userId;
    private String segmentCouponId;
    private String campaignId;
    private String code;
    private String title;
    private String description;
    private CouponType couponType;
    private Double discountValue;
    private Double minOrderAmount;
    private List<String> applicableProductIds;
    private List<String> applicableCategoryIds;
    private LocalDateTime issuedDate;
    private LocalDateTime expiresAt;
    private CouponStatus status;
    private String redeemedOrderId;
    private LocalDateTime redeemedAt;
    private IssuedVia issuedVia;
    private String issuedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

