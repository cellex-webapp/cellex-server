package com.example.cellex.dtos.response.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckHistoryResponse {

    private String checkCode;

    private String shopId;

    private String status;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    private Integer totalAdjustedQuantity;

    private String reason;
}