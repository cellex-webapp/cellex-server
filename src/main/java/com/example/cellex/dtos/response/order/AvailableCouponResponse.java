package com.example.cellex.dtos.response.order;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.CouponType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCouponResponse {

    private String id;

    private String code;

    private String title;

    private String description;

    @JsonProperty("coupon_type")
    private CouponType couponType;

    @JsonProperty("discount_value")
    private Double discountValue;

    @JsonProperty("min_order_amount")
    private Double minOrderAmount;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    private CouponStatus status;

    @JsonProperty("can_apply")
    private Boolean canApply; // Có thể áp dụng cho đơn hàng hiện tại hay không

    @JsonProperty("discount_preview")
    private Double discountPreview; // Số tiền sẽ được giảm nếu áp dụng coupon này
}

