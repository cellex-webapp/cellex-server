package com.example.cellex.dtos.response.product;

import com.example.cellex.enums.ShopStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private String id;
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
    private List<VariationOptionResponse> variationOptions;
    private List<ProductSkuResponse> skus;
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
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariationOptionResponse {
        private String name;
        private List<String> values;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductSkuResponse {
        private String id;
        private String skuCode;
        private Map<String, String> variationData;
        private String imageUrl;
        private Double price;
        private Integer onHandStock;
        private Integer reservedStock;
        private Integer safetyStock;
        private Integer availableStock;
        private Boolean isActive;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShopInfo {
        private String id;

        @JsonProperty("vendor_id")
        private String vendorId;

        @JsonProperty("shop_name")
        private String shopName;

        private String description;

        @JsonProperty("logo_url")
        private String logoUrl;

        private AddressInfo address;

        @JsonProperty("phone_number")
        private String phoneNumber;

        private String email;

        private ShopStatus status;

        private Double rating;

        @JsonProperty("rejection_reason")
        private String rejectionReason;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressInfo {
        private String street;

        private String commune;

        private String province;

        @Builder.Default
        private String country = "Việt Nam";

        @JsonProperty("full_address")
        private String fullAddress;

        @JsonProperty("is_default")
        @Builder.Default
        private boolean isDefault = false;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInfo {
        private String id;
        private String name;
        private String slug;
        private String parentId;
        private String imageUrl;
        private String description;
        private Boolean isActive;
    }
}
