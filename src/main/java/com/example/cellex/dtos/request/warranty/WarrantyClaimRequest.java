package com.example.cellex.dtos.request.warranty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimRequest {
    @NotNull(message = "ID vật phẩm đơn hàng không được để trống")
    private UUID orderItemId;

    @NotBlank(message = "Mô tả lỗi không được để trống")
    private String issueDescription;

    private String images; // Chuỗi JSONB chứa danh sách URL ảnh
}