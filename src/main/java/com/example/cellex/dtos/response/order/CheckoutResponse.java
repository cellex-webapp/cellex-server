package com.example.cellex.dtos.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    private OrderResponse order;
    private String paymentUrl; // Only for VNPAY payment method
    private String message;
}
