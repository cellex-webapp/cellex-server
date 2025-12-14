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
public class ReviewModerationStatsResponse {

    @JsonProperty("total_reviews")
    private long totalReviews;

    @JsonProperty("pending_moderation")
    private long pendingModeration;

    @JsonProperty("approved")
    private long approved;

    @JsonProperty("rejected_auto")
    private long rejectedAuto;

    @JsonProperty("approved_by_admin")
    private long approvedByAdmin;

    @JsonProperty("rejected_by_admin")
    private long rejectedByAdmin;

    @JsonProperty("hidden")
    private long hidden;
}
