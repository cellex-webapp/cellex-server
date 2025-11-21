package com.example.cellex.dtos.response.vnpay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentResponse {
    
    private String code; // Response code
    private String message; // Response message
    private String paymentUrl; // Payment URL to redirect user
}
