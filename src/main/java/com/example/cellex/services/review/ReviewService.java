package com.example.cellex.services.review;

import com.example.cellex.dtos.request.review.CreateReviewRequest;
import com.example.cellex.dtos.request.review.UpdateReviewRequest;
import com.example.cellex.dtos.request.review.VendorResponseRequest;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.review.ReviewResponse;
import com.example.cellex.dtos.response.review.ReviewStatsResponse;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.review.Review;
import com.example.cellex.models.review.VendorResponse;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.review.ReviewRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ReviewModerationService reviewModerationService;

    // List of statuses visible to public users
    private static final List<ReviewStatus> PUBLIC_VISIBLE_STATUSES = Arrays.asList(
            ReviewStatus.APPROVED, 
            ReviewStatus.APPROVED_BY_ADMIN
    );

    @Transactional
    public ReviewResponse createReview(String userId, CreateReviewRequest request) {
        // 1. Kiểm tra order có tồn tại và thuộc về user không
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Kiểm tra order đã được giao thành công chưa (DELIVERED)
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.ORDER_NOT_DELIVERED);
        }

        // 3. Kiểm tra product có trong order không
        OrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_IN_ORDER));

        // 4. Kiểm tra user đã review product trong order này chưa
        if (reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, request.getOrderId(), request.getProductId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 5. Lấy thông tin product và user
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 6. Tạo review với status PENDING_MODERATION
        Review review = Review.builder()
                .productId(request.getProductId())
                .userId(userId)
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .orderId(request.getOrderId())
                .orderItemId(orderItem.getProductId()) // Lưu reference đến order item
                .shopId(product.getShopId())
                .rating(request.getRating())
                .comment(request.getComment())
                .images(request.getImages())
                .videos(request.getVideos())
                .isVerifiedPurchase(true)
                .status(ReviewStatus.PENDING_MODERATION)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        review = reviewRepository.save(review);

        // 7. Trigger async moderation
        reviewModerationService.moderateReviewAsync(review.getId());

        log.info("Review created with PENDING_MODERATION status by user: {} for product: {} in order: {}",
                userId, request.getProductId(), request.getOrderId());

        return mapToReviewResponse(review);
    }

    @Transactional
    public ReviewResponse updateReview(String userId, String reviewId, UpdateReviewRequest request) {
        // 1. Lấy review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // 2. Kiểm tra review có thuộc về user không
        if (!review.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Cập nhật thông tin review
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setImages(request.getImages());
        review.setVideos(request.getVideos());
        review.setUpdatedAt(LocalDateTime.now());

        // 4. Đặt lại status về PENDING_MODERATION để kiểm duyệt lại
        review.setStatus(ReviewStatus.PENDING_MODERATION);
        
        // 5. Xóa kết quả kiểm duyệt cũ
        review.setModerationResult(null);
        review.setAdminDecision(null);

        review = reviewRepository.save(review);

        // 6. Trigger async moderation lại
        reviewModerationService.moderateReviewAsync(review.getId());

        // 7. Cập nhật lại thống kê sản phẩm (tạm thời loại review này ra khỏi tính toán)
        updateProductReviewStats(review.getProductId());

        log.info("Review {} updated by user {} and set to PENDING_MODERATION for re-moderation", reviewId, userId);

        return mapToReviewResponse(review);
    }

    @Transactional
    public ReviewResponse addVendorResponse(String vendorId, String reviewId, VendorResponseRequest request) {
        // 1. Lấy review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // 2. Kiểm tra shop của vendor có quyền trả lời review này không
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!review.getShopId().equals(shop.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Thêm hoặc cập nhật vendor response
        VendorResponse vendorResponse = VendorResponse.builder()
                .vendorId(vendorId)
                .vendorName(shop.getShopName())
                .comment(request.getComment())
                .createdAt(review.getVendorResponse() != null ?
                        review.getVendorResponse().getCreatedAt() : LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        review.setVendorResponse(vendorResponse);
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        log.info("Vendor response added/updated by vendor: {} for review: {}", vendorId, reviewId);

        return mapToReviewResponse(review);
    }

    @Transactional
    public void deleteVendorResponse(String vendorId, String reviewId) {
        // 1. Lấy review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // 2. Kiểm tra shop của vendor có quyền xóa response này không
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!review.getShopId().equals(shop.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Xóa vendor response
        review.setVendorResponse(null);
        review.setUpdatedAt(LocalDateTime.now());

        reviewRepository.save(review);

        log.info("Vendor response deleted by vendor: {} for review: {}", vendorId, reviewId);
    }

    /**
     * Get public product reviews (only APPROVED and APPROVED_BY_ADMIN)
     */
    public PageResponse<ReviewResponse> getProductReviews(String productId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByProductIdAndStatusInOrderByCreatedAtDesc(
                productId, PUBLIC_VISIBLE_STATUSES, pageable);

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

    /**
     * Get public shop reviews (only APPROVED and APPROVED_BY_ADMIN)
     */
    public PageResponse<ReviewResponse> getShopReviews(String shopId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByShopIdAndStatusInOrderByCreatedAtDesc(
                shopId, PUBLIC_VISIBLE_STATUSES, pageable);

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

    /**
     * Get user's own reviews (all statuses for the owner to see their review history)
     */
    public PageResponse<ReviewResponse> getUserReviews(String userId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

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

    /**
     * Get all reviews for a specific order
     * Returns all reviews regardless of status (for order owner)
     */
    public List<ReviewResponse> getOrderReviews(String orderId) {
        List<Review> reviews = reviewRepository.findByOrderId(orderId);
        return reviews.stream()
                .map(this::mapToReviewResponse)
                .toList();
    }

    public ReviewResponse getReviewById(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return mapToReviewResponse(review, null);
    }

    /**
     * Mark a review as helpful
     * Each user can only vote once per review
     */
    @Transactional
    public ReviewResponse markReviewHelpful(String userId, String reviewId) {
        // 1. Lấy review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // 2. Kiểm tra review có public không (chỉ cho vote review đã được duyệt)
        if (!PUBLIC_VISIBLE_STATUSES.contains(review.getStatus())) {
            throw new AppException(ErrorCode.REVIEW_NOT_APPROVED);
        }

        // 3. Kiểm tra user đã vote chưa
        List<String> votedUserIds = review.getHelpfulVotedUserIds();
        if (votedUserIds == null) {
            votedUserIds = new ArrayList<>();
        }

        if (votedUserIds.contains(userId)) {
            throw new AppException(ErrorCode.ALREADY_VOTED_HELPFUL);
        }

        // 4. Thêm vote
        votedUserIds.add(userId);
        review.setHelpfulVotedUserIds(votedUserIds);
        review.setHelpfulCount(votedUserIds.size());
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        log.info("User {} voted helpful for review {}", userId, reviewId);

        return mapToReviewResponse(review, userId);
    }

    /**
     * Get product review statistics (only approved reviews)
     */
    public ReviewStatsResponse getProductReviewStats(String productId) {
        // Only count approved reviews for public stats
        List<Review> reviews = reviewRepository.findByProductIdAndStatusIn(productId, PUBLIC_VISIBLE_STATUSES);

        if (reviews.isEmpty()) {
            return ReviewStatsResponse.builder()
                    .averageRating(0.0)
                    .totalReviews(0)
                    .fiveStarCount(0L)
                    .fourStarCount(0L)
                    .threeStarCount(0L)
                    .twoStarCount(0L)
                    .oneStarCount(0L)
                    .fiveStarPercentage(0.0)
                    .fourStarPercentage(0.0)
                    .threeStarPercentage(0.0)
                    .twoStarPercentage(0.0)
                    .oneStarPercentage(0.0)
                    .build();
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        long fiveStarCount = reviewRepository.countByProductIdAndRatingAndStatusIn(productId, 5, PUBLIC_VISIBLE_STATUSES);
        long fourStarCount = reviewRepository.countByProductIdAndRatingAndStatusIn(productId, 4, PUBLIC_VISIBLE_STATUSES);
        long threeStarCount = reviewRepository.countByProductIdAndRatingAndStatusIn(productId, 3, PUBLIC_VISIBLE_STATUSES);
        long twoStarCount = reviewRepository.countByProductIdAndRatingAndStatusIn(productId, 2, PUBLIC_VISIBLE_STATUSES);
        long oneStarCount = reviewRepository.countByProductIdAndRatingAndStatusIn(productId, 1, PUBLIC_VISIBLE_STATUSES);

        int totalReviews = reviews.size();

        return ReviewStatsResponse.builder()
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalReviews(totalReviews)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .fiveStarPercentage(Math.round((fiveStarCount * 100.0 / totalReviews) * 10.0) / 10.0)
                .fourStarPercentage(Math.round((fourStarCount * 100.0 / totalReviews) * 10.0) / 10.0)
                .threeStarPercentage(Math.round((threeStarCount * 100.0 / totalReviews) * 10.0) / 10.0)
                .twoStarPercentage(Math.round((twoStarCount * 100.0 / totalReviews) * 10.0) / 10.0)
                .oneStarPercentage(Math.round((oneStarCount * 100.0 / totalReviews) * 10.0) / 10.0)
                .build();
    }

    /**
     * Update product review stats (only count approved reviews)
     */
    public void updateProductReviewStats(String productId) {
        List<Review> reviews = reviewRepository.findByProductIdAndStatusIn(productId, PUBLIC_VISIBLE_STATUSES);

        if (!reviews.isEmpty()) {
            double averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXISTED));

            product.setAverageRating(Math.round(averageRating * 10.0) / 10.0); // Làm tròn 1 chữ số
            product.setReviewCount(reviews.size());
            product.setUpdatedAt(LocalDateTime.now());

            productRepository.save(product);

            log.info("Updated review stats for product: {} - Average: {}, Count: {}",
                    productId, product.getAverageRating(), product.getReviewCount());
        }
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return mapToReviewResponse(review, null);
    }

    private ReviewResponse mapToReviewResponse(Review review, String currentUserId) {
        String rejectionReason = null;
        List<String> flaggedCategoriesVi = null;

        // Generate rejection reason and Vietnamese categories if review was rejected
        if (review.getModerationResult() != null && 
            (review.getStatus() == ReviewStatus.REJECTED_AUTO || 
             review.getStatus() == ReviewStatus.REJECTED_BY_ADMIN)) {
            
            if (review.getModerationResult().getFlaggedCategories() != null && 
                !review.getModerationResult().getFlaggedCategories().isEmpty()) {
                rejectionReason = reviewModerationService.generateRejectionReason(
                        review.getModerationResult().getFlaggedCategories());
                flaggedCategoriesVi = reviewModerationService.mapCategoriesToVietnamese(
                        review.getModerationResult().getFlaggedCategories());
            }
        }

        // Check if current user has voted helpful
        Boolean hasVotedHelpful = null;
        if (currentUserId != null && review.getHelpfulVotedUserIds() != null) {
            hasVotedHelpful = review.getHelpfulVotedUserIds().contains(currentUserId);
        }

        // Get product info
        String productName = null;
        String productImage = null;
        try {
            Product product = productRepository.findById(review.getProductId()).orElse(null);
            if (product != null) {
                productName = product.getName();
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    productImage = product.getImages().get(0);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch product info for review {}: {}", review.getId(), e.getMessage());
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .productName(productName)
                .productImage(productImage)
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
                .helpfulCount(review.getHelpfulCount())
                .hasVotedHelpful(hasVotedHelpful)
                .status(review.getStatus())
                .moderationResult(review.getModerationResult())
                .adminDecision(review.getAdminDecision())
                .rejectionReason(rejectionReason)
                .flaggedCategoriesVi(flaggedCategoriesVi)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    /**
     * Delete a review (Customer only - their own review)
     * Cascade deletes vendor responses (handled by Review model cascade setting)
     */
    @Transactional
    public void deleteReview(String userId, String reviewId) {
        log.info("User {} attempting to delete review {}", userId, reviewId);

        // Find review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Delete review (vendor response will be cascade deleted automatically if configured)
        reviewRepository.delete(review);

        log.info("Review {} deleted successfully by user {}", reviewId, userId);
    }
}
