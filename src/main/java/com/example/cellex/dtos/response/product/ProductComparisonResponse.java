package com.example.cellex.dtos.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductComparisonResponse {

    private List<ProductSummary> products;

    /**
     * Key ngoài: attributeName (ví dụ: "Dung lượng RAM")
     * Value: ComparisonRow chứa Map<productId, valueWithUnit> + metadata
     */
    private List<ComparisonRow> technicalSpecs;

    private PriceSummary priceSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private String id;
        private String name;
        private String image;
        private Double price;
        private Double finalPrice;
        private Double saleOff;
        private Double averageRating;
        private Double savedAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonRow {
        private String attributeName;
        private String dataType;
        private Integer sortOrder;
        private Boolean isHighlight;

        /**
         * Map<productId, displayValue> — ví dụ: {"prod1": "8 GB", "prod2": "16 GB"}
         */
        private Map<String, String> values;

        /**
         * true nếu các giá trị giữa các sản phẩm khác nhau
         */
        private Boolean isDifferent;

        /**
         * productId có giá trị tốt nhất (chỉ áp dụng cho NUMBER/BOOLEAN)
         */
        private String bestProductId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceSummary {
        private String lowestPriceProductId;
        private Double lowestFinalPrice;
        private String highestSavingsProductId;
        private Double highestSavingsAmount;
    }
}
