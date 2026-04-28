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
public class InventoryImportHistoryResponse {

    private String referenceId;

    private String supplierId;

    private String supplierName;

    private Integer totalQuantity;

    private Double totalImportAmount;

    @JsonProperty("importedAt")
    private LocalDateTime importedAt;
}