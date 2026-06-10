package com.example.cellex.dtos.response.shipping;

import com.example.cellex.models.order.Order.TrackingEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {
    private String ghnOrderCode;
    private String trackingUrl;
    private String carrierStatus;
    private List<TrackingEvent> events;
}
