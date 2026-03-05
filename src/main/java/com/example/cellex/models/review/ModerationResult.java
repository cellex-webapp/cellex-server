package com.example.cellex.models.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    private Boolean isFlagged;
    private List<String> flaggedCategories;
    private Map<String, Double> categoryScores;
    private String rawResponse;
    private LocalDateTime moderatedAt;
    private String modelUsed;
}
