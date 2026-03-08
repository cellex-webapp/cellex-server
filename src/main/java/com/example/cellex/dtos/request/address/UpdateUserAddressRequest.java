package com.example.cellex.dtos.request.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing delivery address")
public class UpdateUserAddressRequest {

    @Size(max = 10, message = "Mã phường/xã không được quá 10 ký tự")
    @Schema(description = "Mã phường/xã (new ward code)", example = "00004")
    private String communeCode;

    @Size(max = 10, message = "Mã tỉnh/thành không được quá 10 ký tự")
    @Schema(description = "Mã tỉnh/thành phố (optional, auto-resolved from communeCode)", example = "01")
    private String provinceCode;

    @Size(max = 500, message = "Địa chỉ chi tiết không được quá 500 ký tự")
    @Schema(description = "Địa chỉ chi tiết (số nhà, tên đường...)", example = "456 Đường Nguyễn Huệ")
    private String detailAddress;

    @Size(max = 50, message = "Tag không được quá 50 ký tự")
    @Schema(description = "Nhãn/tag cho địa chỉ (không bắt buộc)", example = "Công ty")
    private String tag;

    @Schema(description = "Đặt làm địa chỉ mặc định")
    private Boolean isDefault;
}
