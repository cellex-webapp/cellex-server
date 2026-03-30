package com.example.cellex.services.recommendation;

import com.example.cellex.dtos.response.ml.MLRecommendationItem;
import com.example.cellex.dtos.response.recommendation.RecommendationResponse;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.recommendation.ProductSimilarity;
import com.example.cellex.models.recommendation.Recommendation;
import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.recommendation.RecommendationRepository;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main Recommendation Service - kết hợp CF và Cold-start handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final ProductRepository productRepository;
    private final CollaborativeFilteringService cfService;
    private final ColdStartService coldStartService;
    private final UserInteractionService userInteractionService;
    private final MLRecommendationService mlRecommendationService;

    /**
     * Lấy recommendations cho user (API endpoint)
     * Flow: ML service (hybrid) -> CF -> Cold-start
     */
    public List<RecommendationResponse> getRecommendationsForUser(String userId, String categoryId, Integer limit) {
        int finalLimit = limit != null ? limit : 20;

        log.info("Getting recommendations for user: {}, category: {}, limit: {}", userId, categoryId, finalLimit);

        // Kiểm tra user có lịch sử tương tác không
        boolean hasHistory = userInteractionService.hasUserInteractions(userId);

        // Nếu user có history, thử gọi ML service trước
        if (hasHistory) {
            log.debug("User has interaction history. Trying ML service first...");

            var mlRecommendations = mlRecommendationService.getHybridRecommendations(userId, finalLimit, categoryId);

            if (mlRecommendations.isPresent() && !mlRecommendations.get().isEmpty()) {
                log.info("Using ML-based recommendations for user: {}", userId);
                return convertMLRecommendationsToResponses(mlRecommendations.get());
            }

            log.info("ML service unavailable or returned empty. Falling back to rule-based CF.");

            // Fallback to rule-based CF
            List<Product> recommendedProducts = getCollaborativeFilteringRecommendations(userId, categoryId, finalLimit);
            return convertToRecommendationResponses(recommendedProducts, "COLLABORATIVE_FILTERING",
                    "Dựa trên lịch sử mua hàng và sản phẩm tương tự mà bạn đã quan tâm");
        } else {
            // User mới -> dùng Cold-start handling
            log.debug("New user detected. Using cold-start recommendations.");

            List<Product> recommendedProducts;
            String reason;
            String explanation;

            if (categoryId != null) {
                recommendedProducts = coldStartService.getPopularProductsByCategory(categoryId, finalLimit);
                reason = "POPULAR_IN_CATEGORY";
                explanation = "Sản phẩm phổ biến trong danh mục bạn quan tâm";
            } else {
                recommendedProducts = coldStartService.getPopularityBasedRecommendations(finalLimit);
                reason = "TRENDING";
                explanation = "Sản phẩm đang được yêu thích nhất";
            }

            return convertToRecommendationResponses(recommendedProducts, reason, explanation);
        }
    }

    /**
     * Collaborative Filtering recommendations
     */
    private List<Product> getCollaborativeFilteringRecommendations(String userId, String categoryId, int limit) {
        // Lấy products user đã tương tác
        List<UserInteraction> userInteractions = userInteractionRepository
                .findByUserIdOrderByTotalScoreDesc(userId);
        
        if (userInteractions.isEmpty()) {
            return coldStartService.getTrendingProducts(limit);
        }
        
        // Lấy top products user thích nhất (top 5)
        List<String> topInteractedProductIds = userInteractions.stream()
                .limit(5)
                .map(UserInteraction::getProductId)
                .collect(Collectors.toList());
        
        // Tìm sản phẩm tương tự với các products user đã thích
        Map<String, Double> candidateScores = new HashMap<>();
        Set<String> interactedProductIds = userInteractions.stream()
                .map(UserInteraction::getProductId)
                .collect(Collectors.toSet());
        
        for (String productId : topInteractedProductIds) {
            List<ProductSimilarity> similarities = cfService.getSimilarProducts(productId, 20);
            
            for (ProductSimilarity similarity : similarities) {
                String candidateId = similarity.getSimilarProductId();
                
                // Bỏ qua products user đã tương tác
                if (interactedProductIds.contains(candidateId)) {
                    continue;
                }
                
                // Cộng dồn điểm similarity
                candidateScores.merge(candidateId, similarity.getSimilarityScore(), Double::sum);
            }
        }
        
        // Sắp xếp theo điểm và lấy top
        List<String> topCandidateIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (topCandidateIds.isEmpty()) {
            // Fallback to content-based
            return coldStartService.getContentBasedRecommendations(userId, limit);
        }
        
        List<Product> products = productRepository.findAllById(topCandidateIds);
        
        // Filter by category if specified
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> categoryId.equals(p.getCategoryId()))
                    .collect(Collectors.toList());
        }
        
        // Filter only published products
        return products.stream()
                .filter(Product::getIsPublished)
                .collect(Collectors.toList());
    }

    /**
     * Tính toán offline recommendations cho tất cả users (Scheduled job)
     */
    @Transactional
    public void computeRecommendationsForAllUsers() {
        log.info("Starting offline recommendation computation for all users...");
        
        // Lấy tất cả users có interactions
        List<UserInteraction> allInteractions = userInteractionRepository.findAll();
        
        Set<String> uniqueUserIds = allInteractions.stream()
                .map(UserInteraction::getUserId)
                .collect(Collectors.toSet());
        
        log.info("Computing recommendations for {} users", uniqueUserIds.size());
        
        int processedUsers = 0;
        for (String userId : uniqueUserIds) {
            try {
                computeRecommendationsForUser(userId);
                processedUsers++;
                
                if (processedUsers % 100 == 0) {
                    log.info("Processed {} / {} users", processedUsers, uniqueUserIds.size());
                }
            } catch (Exception e) {
                log.error("Error computing recommendations for user: {}", userId, e);
            }
        }
        
        log.info("Offline recommendation computation completed. Processed {} users", processedUsers);
    }

    /**
     * Tính recommendations cho một user cụ thể
     */
    @Transactional
    public void computeRecommendationsForUser(String userId) {
        // Xóa recommendations cũ
        recommendationRepository.deleteByUserId(userId);
        
        // Lấy CF-based recommendations
        List<Product> recommendedProducts = getCollaborativeFilteringRecommendations(userId, null, 50);
        
        List<Recommendation> recommendations = new ArrayList<>();
        
        for (int i = 0; i < recommendedProducts.size(); i++) {
            Product product = recommendedProducts.get(i);
            
            recommendations.add(Recommendation.builder()
                    .userId(userId)
                    .productId(product.getId())
                    .recommendationScore(1.0 - (i * 0.02)) // Score giảm dần theo rank
                    .recommendationReason("COLLABORATIVE_FILTERING")
                    .explanation("Dựa trên sản phẩm tương tự mà bạn đã quan tâm")
                    .rank(i + 1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
        
        if (!recommendations.isEmpty()) {
            recommendationRepository.saveAll(recommendations);
            log.debug("Saved {} recommendations for user: {}", recommendations.size(), userId);
        }
    }

    /**
     * Lấy pre-computed recommendations
     */
    public List<RecommendationResponse> getPreComputedRecommendations(String userId, Integer limit) {
        int finalLimit = limit != null ? limit : 20;
        
        List<Recommendation> recommendations = recommendationRepository
                .findByUserIdOrderByRecommendationScoreDesc(userId, PageRequest.of(0, finalLimit));
        
        if (recommendations.isEmpty()) {
            // Fallback to real-time computation
            return getRecommendationsForUser(userId, null, finalLimit);
        }
        
        List<String> productIds = recommendations.stream()
                .map(Recommendation::getProductId)
                .collect(Collectors.toList());
        
        List<Product> products = productRepository.findAllById(productIds);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        return recommendations.stream()
                .map(rec -> {
                    Product product = productMap.get(rec.getProductId());
                    if (product == null) return null;
                    
                    return RecommendationResponse.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .productImage(product.getImages() != null && !product.getImages().isEmpty() 
                                    ? product.getImages().get(0) : null)
                            .price(product.getPrice())
                            .finalPrice(product.getFinalPrice())
                            .averageRating(product.getAverageRating())
                            .reviewCount(product.getReviewCount())
                            .recommendationScore(rec.getRecommendationScore())
                            .recommendationReason(rec.getRecommendationReason())
                            .explanation(rec.getExplanation())
                            .rank(rec.getRank())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Convert Products to RecommendationResponses
     */
    private List<RecommendationResponse> convertToRecommendationResponses(
            List<Product> products, String reason, String explanation) {
        
        List<RecommendationResponse> responses = new ArrayList<>();
        
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            
            responses.add(RecommendationResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(product.getImages() != null && !product.getImages().isEmpty() 
                            ? product.getImages().get(0) : null)
                    .price(product.getPrice())
                    .finalPrice(product.getFinalPrice())
                    .averageRating(product.getAverageRating())
                    .reviewCount(product.getReviewCount())
                    .recommendationScore(1.0 - (i * 0.02))
                    .recommendationReason(reason)
                    .explanation(explanation)
                    .rank(i + 1)
                    .build());
        }
        
        return responses;
    }

    /**
     * Convert MLRecommendationItem (from ML service) to RecommendationResponse
     */
    private List<RecommendationResponse> convertMLRecommendationsToResponses(List<MLRecommendationItem> mlItems) {
        return mlItems.stream()
                .map(item -> RecommendationResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .price(item.getPrice())
                        .finalPrice(item.getFinalPrice())
                        .averageRating(item.getAverageRating())
                        .reviewCount(item.getReviewCount())
                        .recommendationScore(item.getScore())
                        .recommendationReason(item.getRecommendationReason())
                        .explanation(item.getExplanation())
                        .rank(item.getRank())
                        .build())
                .collect(Collectors.toList());
    }
}
