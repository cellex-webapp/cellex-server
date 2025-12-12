package com.example.cellex.dtos.response.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewStatsResponse {

    @JsonProperty("average_rating")
    private Double averageRating;

    @JsonProperty("total_reviews")
    private Integer totalReviews;

    @JsonProperty("five_star_count")
    private Long fiveStarCount;

    @JsonProperty("four_star_count")
    private Long fourStarCount;

    @JsonProperty("three_star_count")
    private Long threeStarCount;

    @JsonProperty("two_star_count")
    private Long twoStarCount;

    @JsonProperty("one_star_count")
    private Long oneStarCount;

    @JsonProperty("five_star_percentage")
    private Double fiveStarPercentage;

    @JsonProperty("four_star_percentage")
    private Double fourStarPercentage;

    @JsonProperty("three_star_percentage")
    private Double threeStarPercentage;

    @JsonProperty("two_star_percentage")
    private Double twoStarPercentage;

    @JsonProperty("one_star_percentage")
    private Double oneStarPercentage;
}
