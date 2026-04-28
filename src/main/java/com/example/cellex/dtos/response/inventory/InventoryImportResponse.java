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
public class InventoryImportResponse {

    private String referenceId;

    private String supplierId;

    private String supplierName;

    private Integer totalItems;

    private Integer totalQuantity;

    private Double totalImportAmount;

    private Double supplierDebtAmount;

    private LocalDateTime importedAt;

    private List<ItemResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private String skuId;
        private String skuCode;
        private Integer quantity;
        private Double importPrice;
        private Integer onHandStock;
        private Integer reservedStock;
        private Integer availableStock;
    }
}
