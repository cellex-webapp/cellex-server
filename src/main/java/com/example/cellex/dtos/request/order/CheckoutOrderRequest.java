package com.example.cellex.dtos.request.order;

import com.example.cellex.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutOrderRequest {

    @Schema(description = "Phương thức thanh toán", example = "COD")
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Schema(description = "Ghi chú đơn hàng", example = "Giao hàng ngoài giờ", nullable = true)
    private String note; // optional note provided at checkout
}
