package com.example.cellex.dtos.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

    @Schema(description = "Mã coupon hoặc ID user coupon", example = "GIAMGIA50K")
    @NotBlank(message = "Mã coupon không được để trống")
    private String couponCode;
}

