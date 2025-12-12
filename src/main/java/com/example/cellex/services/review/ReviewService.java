package com.example.cellex.services.review;

import com.example.cellex.dtos.request.review.CreateReviewRequest;
import com.example.cellex.dtos.request.review.VendorResponseRequest;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.review.ReviewResponse;
import com.example.cellex.dtos.response.review.ReviewStatsResponse;
import com.example.cellex.enums.OrderStatus;
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

        // 6. Tạo review
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        review = reviewRepository.save(review);

        // 7. Cập nhật thống kê review cho product
        updateProductReviewStats(request.getProductId());

        log.info("Review created successfully by user: {} for product: {} in order: {}",
                userId, request.getProductId(), request.getOrderId());

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

    public PageResponse<ReviewResponse> getProductReviews(String productId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);

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

    public PageResponse<ReviewResponse> getShopReviews(String shopId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable);

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

    public ReviewResponse getReviewById(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return mapToReviewResponse(review);
    }

    public ReviewStatsResponse getProductReviewStats(String productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

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

        long fiveStarCount = reviewRepository.countByProductIdAndRating(productId, 5);
        long fourStarCount = reviewRepository.countByProductIdAndRating(productId, 4);
        long threeStarCount = reviewRepository.countByProductIdAndRating(productId, 3);
        long twoStarCount = reviewRepository.countByProductIdAndRating(productId, 2);
        long oneStarCount = reviewRepository.countByProductIdAndRating(productId, 1);

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

    // Cập nhật thống kê review cho product
    private void updateProductReviewStats(String productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

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
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
