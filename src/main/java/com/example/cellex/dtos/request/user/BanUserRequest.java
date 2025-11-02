package com.example.cellex.dtos.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request để ban tài khoản user")
public class BanUserRequest {

    @NotBlank(message = "Lý do ban không được để trống")
    @Size(min = 10, max = 500, message = "Lý do ban phải từ 10 đến 500 ký tự")
    @Schema(description = "Lý do khóa tài khoản",
            example = "Vi phạm chính sách bán hàng - Đăng sản phẩm cấm",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String banReason;
}
