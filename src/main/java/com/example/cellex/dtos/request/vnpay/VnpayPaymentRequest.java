package com.example.cellex.dtos.request.vnpay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentRequest {
    
    private String orderId;
    private Long amount; // Amount in VND
    private String orderInfo;
    private String bankCode; // Optional: specific bank code
    private String locale; // vn or en
}
