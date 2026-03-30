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
public class MLRecommendationItem {

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

    @JsonProperty("average_rating")
    private Double averageRating;

    @JsonProperty("review_count")
    private Integer reviewCount;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("recommendation_reason")
    private String recommendationReason;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("svd_score")
    private Double svdScore;

    @JsonProperty("popularity_score")
    private Double popularityScore;
}
