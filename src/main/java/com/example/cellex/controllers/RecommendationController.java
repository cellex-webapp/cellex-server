package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.recommendation.RecommendationResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.recommendation.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recommendations", description = "Product recommendation APIs using Collaborative Filtering")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "Get recommendations for current user",
            description = "Get personalized product recommendations based on user's interaction history. " +
                    "Uses CF for users with history, cold-start handling for new users."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getMyRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        String userId = ((User) userDetails).getId();
        log.info("Getting recommendations for current user: {}", userId);

        List<RecommendationResponse> recommendations = 
                recommendationService.getRecommendationsForUser(userId, categoryId, limit);

        return ResponseEntity.ok(ApiResponse.<List<RecommendationResponse>>builder()
                .code(2000)
                .message("Lấy danh sách gợi ý thành công")
                .result(recommendations)
                .build());
    }

    @Operation(
            summary = "Get recommendations for a specific user",
            description = "Get personalized recommendations for any user by userId. " +
                    "Real-time computation using CF and cold-start handling."
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendationsForUser(
            @PathVariable String userId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        log.info("Getting recommendations for user: {}", userId);

        List<RecommendationResponse> recommendations = 
                recommendationService.getRecommendationsForUser(userId, categoryId, limit);

        return ResponseEntity.ok(ApiResponse.<List<RecommendationResponse>>builder()
                .code(2000)
                .message("Lấy danh sách gợi ý thành công")
                .result(recommendations)
                .build());
    }

    @Operation(
            summary = "Get pre-computed recommendations",
            description = "Get recommendations that were pre-computed offline for faster response. " +
                    "Falls back to real-time computation if no pre-computed data exists."
    )
    @GetMapping("/precomputed/{userId}")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getPreComputedRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        log.info("Getting pre-computed recommendations for user: {}", userId);

        List<RecommendationResponse> recommendations = 
                recommendationService.getPreComputedRecommendations(userId, limit);

        return ResponseEntity.ok(ApiResponse.<List<RecommendationResponse>>builder()
                .code(2000)
                .message("Lấy danh sách gợi ý thành công")
                .result(recommendations)
                .build());
    }

    @Operation(
            summary = "Trigger recommendation computation for user",
            description = "Manually trigger offline computation of recommendations for a specific user. " +
                    "Admin/system use only."
    )
    @PostMapping("/compute/{userId}")
    public ResponseEntity<ApiResponse<Void>> computeRecommendationsForUser(@PathVariable String userId) {
        log.info("Triggering recommendation computation for user: {}", userId);

        recommendationService.computeRecommendationsForUser(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(2000)
                .message("Tính toán gợi ý thành công")
                .build());
    }

    @Operation(
            summary = "Trigger recommendation computation for all users",
            description = "Manually trigger offline computation for all users. Admin/system use only. " +
                    "This is a heavy operation and should be run during off-peak hours."
    )
    @PostMapping("/compute-all")
    public ResponseEntity<ApiResponse<Void>> computeRecommendationsForAllUsers() {
        log.info("Triggering recommendation computation for all users");

        // Run async để không block request
        new Thread(() -> {
            try {
                recommendationService.computeRecommendationsForAllUsers();
            } catch (Exception e) {
                log.error("Error computing recommendations for all users", e);
            }
        }).start();

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(2000)
                .message("Đã bắt đầu tính toán gợi ý cho tất cả người dùng")
                .build());
    }
}
