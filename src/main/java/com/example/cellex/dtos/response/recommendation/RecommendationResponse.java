package com.example.cellex.dtos.response.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponse {

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

    @JsonProperty("recommendation_score")
    private Double recommendationScore;

    @JsonProperty("recommendation_reason")
    private String recommendationReason; // CF, TRENDING, CONTENT_BASED, POPULARITY

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("rank")
    private Integer rank;
}
