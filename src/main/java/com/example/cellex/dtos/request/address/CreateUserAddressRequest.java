package com.example.cellex.dtos.request.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new delivery address")
public class CreateUserAddressRequest {

    @NotBlank(message = "Mã phường/xã không được để trống")
    @Size(max = 10, message = "Mã phường/xã không được quá 10 ký tự")
    @Schema(description = "Mã phường/xã (new ward code)", example = "00004", required = true)
    private String communeCode;

    @Size(max = 10, message = "Mã tỉnh/thành không được quá 10 ký tự")
    @Schema(description = "Mã tỉnh/thành phố (optional, auto-resolved from communeCode)", example = "01")
    private String provinceCode;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 500, message = "Địa chỉ chi tiết không được quá 500 ký tự")
    @Schema(description = "Địa chỉ chi tiết (số nhà, tên đường...)", example = "123 Đường Lê Lợi")
    private String detailAddress;

    @Size(max = 50, message = "Tag không được quá 50 ký tự")
    @Schema(description = "Nhãn/tag cho địa chỉ (không bắt buộc)", example = "Nhà riêng")
    private String tag;

    @Schema(description = "Đặt làm địa chỉ mặc định", example = "false")
    @Builder.Default
    private boolean isDefault = false;
}
