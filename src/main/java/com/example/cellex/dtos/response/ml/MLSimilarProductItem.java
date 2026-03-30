package com.example.cellex.dtos.response.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MLSimilarProductItem {

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_image")
    private String productImage;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("final_price")
    private Double finalPrice;

    @JsonProperty("similarity")
    private Double similarity;

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("recommendation_reason")
    private String recommendationReason;
}
