package com.example.cellex.dtos.request.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopVerificationRequest {

    @NotBlank(message = "ID cửa hàng không được để trống")
    private String shopId;

    @NotBlank(message = "Trạng thái xác thực không được để trống")
    @Pattern(regexp = "APPROVED|REJECTED", message = "Trạng thái phải là APPROVED hoặc REJECTED")
    private String status;

    private String rejectionReason;
}
