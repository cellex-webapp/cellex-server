package com.example.cellex.dtos.response.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnCreateOrderResponse {
    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Data data;

    @lombok.Data
    public static class Data {
        @JsonProperty("order_code")
        private String orderCode;

        @JsonProperty("total_fee")
        private int totalFee;

        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;

        // GHN may not return label url directly in some API versions.
        // We will construct it or expect it depending on the GHN API response
        @JsonProperty("label")
        private String label;
    }
}
