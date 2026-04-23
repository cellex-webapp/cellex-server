package com.example.cellex.seeder;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.review.Review;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.review.ReviewRepository;
import com.example.cellex.services.review.ReviewService;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewSeeder {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserBehaviorSimulator userBehaviorSimulator;
    private final ReviewService reviewService;

    private final Faker faker = new Faker();

    @Transactional
    public List<Review> seedReviewsFromOrders(List<Order> allOrders, List<User> users) {
        if (allOrders == null || allOrders.isEmpty() || users == null || users.isEmpty()) {
            return List.of();
        }

        Map<String, User> userById = new HashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) {
                userById.put(user.getId(), user);
            }
        }

        if (userById.isEmpty()) {
            return List.of();
        }

        List<Review> pendingReviews = new ArrayList<>();
        Set<String> touchedProductIds = new HashSet<>();

        for (Order order : allOrders) {
            if (!isEligibleOrder(order)) {
                continue;
            }

            String userId = order.getUserId();
            if (userId == null || !userBehaviorSimulator.willReview(userId)) {
                continue;
            }

            User user = userById.get(userId);
            if (user == null || order.getItems().isEmpty()) {
                continue;
            }

            OrderItem firstItem = order.getItems().get(0);
            if (firstItem == null || firstItem.getProductId() == null) {
                continue;
            }

            if (reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, order.getId(), firstItem.getProductId())) {
                continue;
            }

            if (productRepository.findById(firstItem.getProductId()).isEmpty()) {
                continue;
            }

            int rating = weightedRating();
            Review review = Review.builder()
                    .productId(firstItem.getProductId())
                    .userId(userId)
                    .userName(user.getFullName())
                    .userAvatar(user.getAvatarUrl())
                    .orderId(order.getId())
                    .orderItemId(firstItem.getProductId())
                    .shopId(order.getShopId())
                    .rating(rating)
                    .comment(buildComment(rating, firstItem.getProductName()))
                    .images(List.of())
                    .videos(List.of())
                    .isVerifiedPurchase(true)
                    .helpfulCount(0)
                    .status(ReviewStatus.APPROVED)
                    .createdAt(resolveReviewCreatedAt(order))
                    .updatedAt(LocalDateTime.now())
                    .build();

            pendingReviews.add(review);
            touchedProductIds.add(firstItem.getProductId());
        }

        if (pendingReviews.isEmpty()) {
            return List.of();
        }

        List<Review> saved = reviewRepository.saveAll(pendingReviews);

        for (String productId : touchedProductIds) {
            reviewService.updateProductReviewStats(productId);
        }

        log.info("Seeded reviews: {}", saved.size());
        return saved;
    }

    private boolean isEligibleOrder(Order order) {
        return order != null
                && order.getStatus() == OrderStatus.DELIVERED
                && order.getId() != null
                && order.getUserId() != null
                && order.getItems() != null
                && !order.getItems().isEmpty();
    }

    private LocalDateTime resolveReviewCreatedAt(Order order) {
        LocalDateTime base = order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getCreatedAt();
        if (base == null) {
            base = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 30));
        }
        return base.plusDays(ThreadLocalRandom.current().nextInt(1, 15));
    }

    private int weightedRating() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 40) {
            return 5;
        }
        if (roll < 70) {
            return 4;
        }
        if (roll < 90) {
            return 3;
        }
        if (roll < 97) {
            return 2;
        }
        return 1;
    }

    private String buildComment(int rating, String productName) {
        String product = productName == null ? "san pham" : productName;
        String sentence = faker.lorem().sentence(8, 15);

        if (rating >= 5) {
            return "Rat hai long voi " + product + ". " + sentence;
        }
        if (rating == 4) {
            return "San pham tot va dung ky vong. " + sentence;
        }
        if (rating == 3) {
            return "Trai nghiem o muc trung binh voi " + product + ". " + sentence;
        }
        if (rating == 2) {
            return "Chat luong chua nhu mong doi, can cai thien. " + sentence;
        }
        return "Khong hai long voi san pham va dich vu. " + sentence;
    }
}
