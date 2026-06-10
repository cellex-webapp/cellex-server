package com.example.cellex.dtos.request.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GhnWebhookPayload {
    @JsonProperty("OrderCode")
    private String orderCode;

    @JsonProperty("ClientOrderCode")
    private String clientOrderCode;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Warehouse")
    private String warehouse;

    @JsonProperty("Time")
    private LocalDateTime time;

    @JsonProperty("CODAmount")
    private Integer codAmount;

    @JsonProperty("ShopID")
    private Integer shopId;

    @JsonProperty("Type")
    private String type;
}
