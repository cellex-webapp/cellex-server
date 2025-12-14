package com.example.cellex.controllers;

import com.example.cellex.dtos.request.review.AdminReviewActionRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.review.ReviewModerationStatsResponse;
import com.example.cellex.dtos.response.review.ReviewResponse;
import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.services.review.AdminReviewService;
import com.example.cellex.services.review.ReviewModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "16. Admin Review Management", description = "APIs for admin to manage review moderation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;
    private final ReviewModerationService reviewModerationService;

    @Operation(summary = "Get all reviews", description = "Admin gets all reviews with all statuses")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ReviewResponse> reviews = adminReviewService.getAllReviews(pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get reviews by status", description = "Admin gets reviews filtered by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviewsByStatus(
            @PathVariable ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> reviews = adminReviewService.getReviewsByStatus(status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá theo trạng thái thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get pending reviews", description = "Admin gets reviews pending moderation or auto-rejected")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        List<ReviewStatus> pendingStatuses = List.of(
                ReviewStatus.PENDING_MODERATION,
                ReviewStatus.REJECTED_AUTO
        );
        PageResponse<ReviewResponse> reviews = adminReviewService.getReviewsByStatuses(pendingStatuses, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá cần xử lý thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get reviews by product", description = "Admin gets all reviews for a specific product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviewsByProduct(
            @PathVariable String productId,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> reviews = adminReviewService.getReviewsByProduct(productId, status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá sản phẩm thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get reviews by user", description = "Admin gets all reviews from a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviewsByUser(
            @PathVariable String userId,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> reviews = adminReviewService.getReviewsByUser(userId, status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá người dùng thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get reviews by date range", description = "Admin gets reviews within a date range")
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviewsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> reviews = adminReviewService.getReviewsByDateRange(
                startDate, endDate, status, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá theo khoảng thời gian thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get review by ID", description = "Admin gets detailed review information")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable String reviewId) {
        ReviewResponse review = adminReviewService.getReviewById(reviewId);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Lấy thông tin đánh giá thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Approve review", description = "Admin approves a rejected or pending review")
    @PostMapping("/{reviewId}/approve")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String reviewId,
            @Valid @RequestBody AdminReviewActionRequest request
    ) {
        String adminId = ((com.example.cellex.models.user.User) userDetails).getId();
        ReviewResponse review = adminReviewService.approveReview(adminId, reviewId, request);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Duyệt đánh giá thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Reject review", description = "Admin rejects an approved or pending review")
    @PostMapping("/{reviewId}/reject")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String reviewId,
            @Valid @RequestBody AdminReviewActionRequest request
    ) {
        String adminId = ((com.example.cellex.models.user.User) userDetails).getId();
        ReviewResponse review = adminReviewService.rejectReview(adminId, reviewId, request);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Từ chối đánh giá thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Hide review", description = "Admin hides a visible review")
    @PostMapping("/{reviewId}/hide")
    public ResponseEntity<ApiResponse<ReviewResponse>> hideReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String reviewId,
            @Valid @RequestBody AdminReviewActionRequest request
    ) {
        String adminId = ((com.example.cellex.models.user.User) userDetails).getId();
        ReviewResponse review = adminReviewService.hideReview(adminId, reviewId, request);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Ẩn đánh giá thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Re-moderate review", description = "Trigger moderation again for a review")
    @PostMapping("/{reviewId}/remoderate")
    public ResponseEntity<ApiResponse<ReviewResponse>> remoderateReview(@PathVariable String reviewId) {
        ReviewResponse review = adminReviewService.remoderate(reviewId);
        
        // Trigger async moderation
        reviewModerationService.moderateReviewAsync(reviewId);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Đánh giá đang được kiểm duyệt lại")
                .result(review)
                .build());
    }

    @Operation(summary = "Get moderation statistics", description = "Get statistics on review moderation")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReviewModerationStatsResponse>> getModerationStats() {
        ReviewModerationStatsResponse stats = adminReviewService.getModerationStats();

        return ResponseEntity.ok(ApiResponse.<ReviewModerationStatsResponse>builder()
                .code(2000)
                .message("Lấy thống kê kiểm duyệt thành công")
                .result(stats)
                .build());
    }
}
