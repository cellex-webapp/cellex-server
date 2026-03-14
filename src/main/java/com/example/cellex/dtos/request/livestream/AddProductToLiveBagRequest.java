package com.example.cellex.dtos.request.livestream;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddProductToLiveBagRequest {
    @NotBlank(message = "ID sản phẩm không được để trống")
    private String productId;
    private Double flashSalePrice;
}