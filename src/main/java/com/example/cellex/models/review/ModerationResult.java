package com.example.cellex.models.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    @Field("is_flagged")
    private Boolean isFlagged;

    @Field("flagged_categories")
    private List<String> flaggedCategories; // hate, harassment, sexual, violence, etc.

    @Field("category_scores")
    private Map<String, Double> categoryScores; // Scores for each category

    @Field("raw_response")
    private String rawResponse; // Raw JSON response from OpenAI

    @Field("moderated_at")
    private LocalDateTime moderatedAt;

    @Field("model_used")
    private String modelUsed; // e.g., "text-moderation-latest"
}
