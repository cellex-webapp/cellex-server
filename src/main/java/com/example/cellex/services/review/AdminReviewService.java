package com.example.cellex.services.review;

import com.example.cellex.dtos.request.review.AdminReviewActionRequest;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.review.ReviewModerationStatsResponse;
import com.example.cellex.dtos.response.review.ReviewResponse;
import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.review.AdminDecision;
import com.example.cellex.models.review.Review;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.review.ReviewRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final ReviewModerationService reviewModerationService;

    /**
     * Get all reviews for admin (all statuses)
     */
    public PageResponse<ReviewResponse> getAllReviews(Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findAll(pageable);
        return buildPageResponse(reviewPage);
    }

    /**
     * Get reviews by status for admin
     */
    public PageResponse<ReviewResponse> getReviewsByStatus(ReviewStatus status, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return buildPageResponse(reviewPage);
    }

    /**
     * Get reviews by multiple statuses for admin
     */
    public PageResponse<ReviewResponse> getReviewsByStatuses(List<ReviewStatus> statuses, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByStatusInOrderByCreatedAtDesc(statuses, pageable);
        return buildPageResponse(reviewPage);
    }

    /**
     * Get reviews by product for admin (all statuses)
     */
    public PageResponse<ReviewResponse> getReviewsByProduct(String productId, ReviewStatus status, Pageable pageable) {
        Page<Review> reviewPage;
        if (status != null) {
            reviewPage = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, status, pageable);
        } else {
            reviewPage = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }
        return buildPageResponse(reviewPage);
    }

    /**
     * Get reviews by user for admin (all statuses)
     */
    public PageResponse<ReviewResponse> getReviewsByUser(String userId, ReviewStatus status, Pageable pageable) {
        Page<Review> reviewPage;
        if (status != null) {
            reviewPage = reviewRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return buildPageResponse(reviewPage);
    }

    /**
     * Get reviews by date range for admin
     */
    public PageResponse<ReviewResponse> getReviewsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, ReviewStatus status, Pageable pageable) {
        Page<Review> reviewPage;
        if (status != null) {
            reviewPage = reviewRepository.findByCreatedAtBetweenAndStatusOrderByCreatedAtDesc(
                    startDate, endDate, status, pageable);
        } else {
            reviewPage = reviewRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
        }
        return buildPageResponse(reviewPage);
    }

    /**
     * Admin approves a rejected review
     */
    @Transactional
    public ReviewResponse approveReview(String adminId, String reviewId, AdminReviewActionRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Only allow approving reviews that are REJECTED_AUTO or PENDING_MODERATION
        if (review.getStatus() != ReviewStatus.REJECTED_AUTO && 
            review.getStatus() != ReviewStatus.PENDING_MODERATION &&
            review.getStatus() != ReviewStatus.HIDDEN) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        AdminDecision decision = AdminDecision.builder()
                .adminId(adminId)
                .adminName(admin.getFullName())
                .action("APPROVE")
                .reason(request.getReason())
                .decidedAt(LocalDateTime.now())
                .build();

        review.setStatus(ReviewStatus.APPROVED_BY_ADMIN);
        review.setAdminDecision(decision);
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        // Update product stats since review is now visible
        reviewService.updateProductReviewStats(review.getProductId());

        log.info("Review {} approved by admin {} - Reason: {}", reviewId, adminId, request.getReason());

        return mapToReviewResponse(review);
    }

    /**
     * Admin rejects an approved review
     */
    @Transactional
    public ReviewResponse rejectReview(String adminId, String reviewId, AdminReviewActionRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Only allow rejecting reviews that are APPROVED, APPROVED_BY_ADMIN, or PENDING_MODERATION
        if (review.getStatus() != ReviewStatus.APPROVED && 
            review.getStatus() != ReviewStatus.APPROVED_BY_ADMIN &&
            review.getStatus() != ReviewStatus.PENDING_MODERATION) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        String productId = review.getProductId();
        boolean wasVisible = review.getStatus() == ReviewStatus.APPROVED || 
                            review.getStatus() == ReviewStatus.APPROVED_BY_ADMIN;

        AdminDecision decision = AdminDecision.builder()
                .adminId(adminId)
                .adminName(admin.getFullName())
                .action("REJECT")
                .reason(request.getReason())
                .decidedAt(LocalDateTime.now())
                .build();

        review.setStatus(ReviewStatus.REJECTED_BY_ADMIN);
        review.setAdminDecision(decision);
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        // Update product stats if review was previously visible
        if (wasVisible) {
            reviewService.updateProductReviewStats(productId);
        }

        log.info("Review {} rejected by admin {} - Reason: {}", reviewId, adminId, request.getReason());

        return mapToReviewResponse(review);
    }

    /**
     * Admin hides a review (softer than reject)
     */
    @Transactional
    public ReviewResponse hideReview(String adminId, String reviewId, AdminReviewActionRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Only allow hiding reviews that are visible (APPROVED or APPROVED_BY_ADMIN)
        if (review.getStatus() != ReviewStatus.APPROVED && 
            review.getStatus() != ReviewStatus.APPROVED_BY_ADMIN) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        String productId = review.getProductId();

        AdminDecision decision = AdminDecision.builder()
                .adminId(adminId)
                .adminName(admin.getFullName())
                .action("HIDE")
                .reason(request.getReason())
                .decidedAt(LocalDateTime.now())
                .build();

        review.setStatus(ReviewStatus.HIDDEN);
        review.setAdminDecision(decision);
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        // Update product stats since review is now hidden
        reviewService.updateProductReviewStats(productId);

        log.info("Review {} hidden by admin {} - Reason: {}", reviewId, adminId, request.getReason());

        return mapToReviewResponse(review);
    }

    /**
     * Get detailed review by ID for admin
     */
    public ReviewResponse getReviewById(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return mapToReviewResponse(review);
    }

    /**
     * Get moderation statistics
     */
    public ReviewModerationStatsResponse getModerationStats() {
        long total = reviewRepository.count();
        long pendingModeration = reviewRepository.countByStatus(ReviewStatus.PENDING_MODERATION);
        long approved = reviewRepository.countByStatus(ReviewStatus.APPROVED);
        long rejectedAuto = reviewRepository.countByStatus(ReviewStatus.REJECTED_AUTO);
        long approvedByAdmin = reviewRepository.countByStatus(ReviewStatus.APPROVED_BY_ADMIN);
        long rejectedByAdmin = reviewRepository.countByStatus(ReviewStatus.REJECTED_BY_ADMIN);
        long hidden = reviewRepository.countByStatus(ReviewStatus.HIDDEN);

        return ReviewModerationStatsResponse.builder()
                .totalReviews(total)
                .pendingModeration(pendingModeration)
                .approved(approved)
                .rejectedAuto(rejectedAuto)
                .approvedByAdmin(approvedByAdmin)
                .rejectedByAdmin(rejectedByAdmin)
                .hidden(hidden)
                .build();
    }

    /**
     * Re-moderate a review (trigger moderation again)
     */
    @Transactional
    public ReviewResponse remoderate(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // Reset status to pending
        review.setStatus(ReviewStatus.PENDING_MODERATION);
        review.setModerationResult(null);
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        log.info("Review {} set to re-moderate", reviewId);

        return mapToReviewResponse(review);
    }

    private PageResponse<ReviewResponse> buildPageResponse(Page<Review> reviewPage) {
        List<ReviewResponse> reviews = reviewPage.getContent().stream()
                .map(this::mapToReviewResponse)
                .toList();

        return PageResponse.<ReviewResponse>builder()
                .currentPage(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalPages(reviewPage.getTotalPages())
                .totalElements(reviewPage.getTotalElements())
                .content(reviews)
                .build();
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        String rejectionReason = null;
        List<String> flaggedCategoriesVi = null;

        // Generate rejection reason and Vietnamese categories if review was rejected
        if (review.getStatus() == ReviewStatus.REJECTED_AUTO || review.getStatus() == ReviewStatus.REJECTED_BY_ADMIN) {
            if (review.getModerationResult() != null && Boolean.TRUE.equals(review.getModerationResult().getIsFlagged())) {
                rejectionReason = reviewModerationService.generateRejectionReason(review.getModerationResult().getFlaggedCategories());
                flaggedCategoriesVi = reviewModerationService.mapCategoriesToVietnamese(review.getModerationResult().getFlaggedCategories());
            }
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .userAvatar(review.getUserAvatar())
                .orderId(review.getOrderId())
                .shopId(review.getShopId())
                .rating(review.getRating())
                .comment(review.getComment())
                .images(review.getImages())
                .videos(review.getVideos())
                .vendorResponse(review.getVendorResponse())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .status(review.getStatus())
                .moderationResult(review.getModerationResult())
                .adminDecision(review.getAdminDecision())
                .rejectionReason(rejectionReason)
                .flaggedCategoriesVi(flaggedCategoriesVi)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
