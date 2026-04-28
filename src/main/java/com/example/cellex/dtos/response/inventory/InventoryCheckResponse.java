package com.example.cellex.dtos.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResponse {

    private String checkId;

    private String shopId;

    private String status;

    private LocalDateTime createdAt;

    private Integer totalAdjustedQuantity;

    private List<ItemResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private String skuId;
        private String skuCode;
        private Integer systemStock;
        private Integer actualStock;
        private Integer difference;
        private String reason;
        private Integer onHandStock;
        private Integer reservedStock;
        private Integer availableStock;
    }
}
