package com.example.cellex.dtos.response.vnpay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCallbackResponse {
    
    private String rspCode; // Response code to VNPAY
    private String message; // Response message
}
