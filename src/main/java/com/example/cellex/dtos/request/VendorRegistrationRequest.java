package com.example.cellex.dtos.request;

import jakarta.validation.constraints.Email;
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
public class VendorRegistrationRequest {

    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên cửa hàng phải từ 2-100 ký tự")
    private String shopName;

    @NotBlank(message = "Mô tả cửa hàng không được để trống")
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotBlank(message = "Mã tỉnh không được để trống")
    private String provinceCode;

    @NotBlank(message = "Mã xã/phường không được để trống")
    private String communeCode;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 200, message = "Địa chỉ chi tiết không được vượt quá 200 ký tự")
    private String detailAddress;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 15, message = "Số điện thoại phải từ 10-15 ký tự")
    private String phoneNumber;

    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
