package com.example.cellex.dtos.response;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {
    private String id;
    private String userId;
    private String segmentCouponId;
    private String code;
    private String title;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private LocalDateTime issuedDate;
    private LocalDateTime expiresAt;
    private CouponStatus status;
    private String redeemedOrderId;
    private LocalDateTime redeemedAt;
    private String issueReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

