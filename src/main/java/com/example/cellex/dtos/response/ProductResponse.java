package com.example.cellex.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private String id;
    private String shopId;
    private String categoryId;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private List<String> images;
    private Double price;
    private Double saleOff;
    private Double finalPrice;
    private Integer stockQuantity;
    private Double weight;
    private List<ProductAttributeValueResponse> attributeValues;
    private Double averageRating;
    private Integer reviewCount;
    private Integer purchaseCount;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin cửa hàng (nếu cần)
    private ShopInfo shopInfo;

    // Thông tin danh mục (nếu cần)
    private CategoryInfo categoryInfo;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductAttributeValueResponse {
        private String attributeId;
        private String attributeKey;
        private String attributeName;
        private String value;
        private String unit;
        private String dataType;
        private Boolean isComparable;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShopInfo {
        private String id;
        private String shopName;
        private String logoUrl;
        private Boolean isVerified;
        private Double rating;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInfo {
        private String id;
        private String name;
        private String imageUrl;
    }
}
