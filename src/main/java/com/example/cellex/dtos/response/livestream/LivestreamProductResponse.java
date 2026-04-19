package com.example.cellex.dtos.response.livestream;

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
public class LivestreamProductResponse {
    private String id;
    private String productId;
    private Double flashSalePrice;
    private Boolean isPinned;

    // Product details for UI rendering
    private String shopId;
    private String categoryId;
    private String name;
    private String description;
    private List<String> images;
    private Double price;
    private Double saleOff;
    private Double finalPrice;
    private Integer stockQuantity;
    private List<ProductAttributeValueResponse> attributeValues;
    private Double averageRating;
    private Integer reviewCount;
    private Integer purchaseCount;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAttributeValueResponse {
        private String attributeId;
        private String attributeKey;
        private String attributeName;
        private String value;
        private String unit;
        private String dataType;
    }
}
