package com.example.cellex.dtos.response.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private String ghnOrderCode;
    private String labelUrl;
    private LocalDateTime expectedDelivery;
}
