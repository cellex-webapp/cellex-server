package com.example.cellex.dtos.request.shipping;

import lombok.Data;

@Data
public class PrepareShipmentRequest {
    private int weight; // gram
    private int length; // cm
    private int width;  // cm
    private int height; // cm
    private String note;
}
