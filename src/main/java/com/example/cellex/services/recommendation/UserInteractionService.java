package com.example.cellex.services.recommendation;

import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service quản lý tương tác của user với sản phẩm
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserInteractionService {

    private final UserInteractionRepository userInteractionRepository;

    /**
     * Ghi nhận khi user xem sản phẩm
     */
    @Transactional
    public void recordView(String userId, String productId, String categoryId) {
        UserInteraction interaction = getOrCreateInteraction(userId, productId, categoryId);
        interaction.setViewCount(interaction.getViewCount() + 1);
        interaction.calculateTotalScore();
        interaction.setUpdatedAt(LocalDateTime.now());
        userInteractionRepository.save(interaction);
        
        log.debug("Recorded view: user={}, product={}", userId, productId);
    }

    /**
     * Ghi nhận khi user thêm vào giỏ hàng
     */
    @Transactional
    public void recordAddToCart(String userId, String productId, String categoryId) {
        UserInteraction interaction = getOrCreateInteraction(userId, productId, categoryId);
        interaction.setCartCount(interaction.getCartCount() + 1);
        interaction.calculateTotalScore();
        interaction.setUpdatedAt(LocalDateTime.now());
        userInteractionRepository.save(interaction);
        
        log.debug("Recorded add to cart: user={}, product={}", userId, productId);
    }

    /**
     * Ghi nhận khi user mua sản phẩm
     */
    @Transactional
    public void recordPurchase(String userId, String productId, String categoryId) {
        UserInteraction interaction = getOrCreateInteraction(userId, productId, categoryId);
        interaction.setPurchaseCount(interaction.getPurchaseCount() + 1);
        interaction.calculateTotalScore();
        interaction.setUpdatedAt(LocalDateTime.now());
        userInteractionRepository.save(interaction);
        
        log.debug("Recorded purchase: user={}, product={}", userId, productId);
    }

    /**
     * Ghi nhận khi user review sản phẩm
     */
    @Transactional
    public void recordReview(String userId, String productId, String categoryId) {
        UserInteraction interaction = getOrCreateInteraction(userId, productId, categoryId);
        interaction.setReviewCount(interaction.getReviewCount() + 1);
        interaction.calculateTotalScore();
        interaction.setUpdatedAt(LocalDateTime.now());
        userInteractionRepository.save(interaction);
        
        log.debug("Recorded review: user={}, product={}", userId, productId);
    }

    /**
     * Lấy hoặc tạo mới interaction
     */
    private UserInteraction getOrCreateInteraction(String userId, String productId, String categoryId) {
        return userInteractionRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> UserInteraction.builder()
                        .userId(userId)
                        .productId(productId)
                        .categoryId(categoryId)
                        .viewCount(0)
                        .cartCount(0)
                        .purchaseCount(0)
                        .reviewCount(0)
                        .totalScore(0.0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
    }

    /**
     * Kiểm tra user có lịch sử tương tác không
     */
    public boolean hasUserInteractions(String userId) {
        return userInteractionRepository.existsByUserId(userId);
    }
}
