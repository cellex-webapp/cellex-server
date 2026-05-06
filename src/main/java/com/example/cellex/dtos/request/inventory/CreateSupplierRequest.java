package com.example.cellex.dtos.request.inventory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierRequest {

    private String shopId;

    @NotBlank(message = "Ten nha cung cap khong duoc de trong")
    private String supplierName;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    private String phoneNumber;

    @Email(message = "Email khong hop le")
    private String email;

    private String address;

    @NotBlank(message = "Ma so thue khong duoc de trong")
    private String taxCode;
}
