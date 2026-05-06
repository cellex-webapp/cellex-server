package com.example.cellex.dtos.request.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariationOptionRequest {

    @NotBlank(message = "Ten nhom bien the khong duoc de trong")
    private String name;

    @NotEmpty(message = "Danh sach gia tri bien the khong duoc de trong")
    private List<String> values;
}
