package com.example.cellex.dtos.request.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCartItemQuantityRequest {

    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    @NotNull(message = "Action không được để trống")
    private QuantityAction action;

    public enum QuantityAction {
        INCREASE,  // Tăng 1
        DECREASE   // Giảm 1
    }
}

