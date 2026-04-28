package com.example.cellex.dtos.request.inventory;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {

    private String supplierName;

    private String phoneNumber;

    @Email(message = "Email khong hop le")
    private String email;

    private String address;

    private String taxCode;
}
