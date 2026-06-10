package com.example.cellex.dtos.request.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GhnCreateOrderRequest {
    @JsonProperty("payment_type_id")
    private int paymentTypeId;

    @JsonProperty("required_note")
    private String requiredNote;

    @JsonProperty("client_order_code")
    private String clientOrderCode;

    @JsonProperty("to_name")
    private String toName;

    @JsonProperty("to_phone")
    private String toPhone;

    @JsonProperty("to_address")
    private String toAddress;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("to_district_id")
    private int toDistrictId;

    @JsonProperty("cod_amount")
    private int codAmount;

    @JsonProperty("weight")
    private int weight;

    @JsonProperty("length")
    private int length;

    @JsonProperty("width")
    private int width;

    @JsonProperty("height")
    private int height;

    @JsonProperty("service_type_id")
    private int serviceTypeId;

    @JsonProperty("items")
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        @JsonProperty("name")
        private String name;

        @JsonProperty("quantity")
        private int quantity;

        @JsonProperty("price")
        private int price;
    }
}
