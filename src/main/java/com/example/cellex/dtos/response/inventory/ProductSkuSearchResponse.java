package com.example.cellex.dtos.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuSearchResponse {

    private String skuId;

    private String skuCode;

    private String productId;

    private String productName;

    private String productImage;

    private Double price;

    private Integer onHandStock;

    private Integer reservedStock;

    private Integer availableStock;

    private Integer safetyStock;

    private Map<String, String> variationData;
}
