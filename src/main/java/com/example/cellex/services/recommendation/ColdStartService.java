package com.example.cellex.services.recommendation;

import com.example.cellex.models.product.Product;
import com.example.cellex.models.recommendation.ProductSimilarity;
import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.recommendation.ProductSimilarityRepository;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý cold-start problem
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColdStartService {

    private final ProductRepository productRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final ProductSimilarityRepository productSimilarityRepository;

    /**
     * Lấy trending products (sản phẩm có nhiều tương tác nhất)
     */
    public List<Product> getTrendingProducts(int limit) {
        log.debug("Getting trending products with limit: {}", limit);
        
        // Lấy tất cả interactions và tính tổng điểm cho mỗi product
        List<UserInteraction> allInteractions = userInteractionRepository.findAll();
        
        Map<String, Double> productScores = new HashMap<>();
        for (UserInteraction interaction : allInteractions) {
            String productId = interaction.getProductId();
            productScores.merge(productId, interaction.getTotalScore(), Double::sum);
        }
        
        // Sắp xếp theo điểm và lấy top products
        List<String> topProductIds = productScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (topProductIds.isEmpty()) {
            // Fallback: lấy sản phẩm mới nhất
            return productRepository.findAllBy(PageRequest.of(0, limit, 
                    Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent();
        }
        
        return productRepository.findAllById(topProductIds);
    }

    /**
     * Lấy popular products theo category
     */
    public List<Product> getPopularProductsByCategory(String categoryId, int limit) {
        log.debug("Getting popular products for category: {}", categoryId);
        
        // Lấy products theo category
        List<Product> categoryProducts = productRepository
                .findByCategoryIdAndIsPublishedTrue(categoryId, PageRequest.of(0, 100))
                .getContent();
        
        if (categoryProducts.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Lấy interactions cho các products này
        Set<String> productIds = categoryProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        
        Map<String, Double> productScores = new HashMap<>();
        for (String productId : productIds) {
            List<UserInteraction> interactions = userInteractionRepository.findByProductId(productId);
            double totalScore = interactions.stream()
                    .mapToDouble(UserInteraction::getTotalScore)
                    .sum();
            productScores.put(productId, totalScore);
        }
        
        // Sắp xếp và lấy top
        return categoryProducts.stream()
                .sorted((p1, p2) -> {
                    double score1 = productScores.getOrDefault(p1.getId(), 0.0);
                    double score2 = productScores.getOrDefault(p2.getId(), 0.0);
                    return Double.compare(score2, score1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Content-based recommendation dựa trên metadata (category, price range)
     */
    public List<Product> getContentBasedRecommendations(String userId, int limit) {
        log.debug("Getting content-based recommendations for user: {}", userId);
        
        // Lấy interactions của user để xác định sở thích
        List<UserInteraction> userInteractions = userInteractionRepository
                .findByUserIdOrderByTotalScoreDesc(userId);
        
        if (userInteractions.isEmpty()) {
            return getTrendingProducts(limit);
        }
        
        // Phân tích categories user quan tâm
        Map<String, Double> categoryPreferences = new HashMap<>();
        for (UserInteraction interaction : userInteractions) {
            String categoryId = interaction.getCategoryId();
            if (categoryId != null) {
                categoryPreferences.merge(categoryId, interaction.getTotalScore(), Double::sum);
            }
        }
        
        // Lấy category user thích nhất
        String topCategory = categoryPreferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        if (topCategory == null) {
            return getTrendingProducts(limit);
        }
        
        // Lấy products từ category đó, loại bỏ products user đã tương tác
        Set<String> interactedProductIds = userInteractions.stream()
                .map(UserInteraction::getProductId)
                .collect(Collectors.toSet());
        
        List<Product> categoryProducts = productRepository
                .findByCategoryIdAndIsPublishedTrue(topCategory, PageRequest.of(0, limit * 2))
                .getContent();
        
        return categoryProducts.stream()
                .filter(p -> !interactedProductIds.contains(p.getId()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Tính content-based similarity cho sản phẩm mới (cold product)
     */
    public void computeContentBasedSimilarity(String newProductId) {
        log.info("Computing content-based similarity for new product: {}", newProductId);
        
        Optional<Product> newProductOpt = productRepository.findById(newProductId);
        if (newProductOpt.isEmpty()) {
            return;
        }
        
        Product newProduct = newProductOpt.get();
        
        // Tìm sản phẩm cùng category
        List<Product> similarCategoryProducts = productRepository
                .findByCategoryIdAndIsPublishedTrue(newProduct.getCategoryId(), PageRequest.of(0, 50))
                .getContent();
        
        List<ProductSimilarity> similarities = new ArrayList<>();
        
        for (Product similarProduct : similarCategoryProducts) {
            if (similarProduct.getId().equals(newProductId)) {
                continue;
            }
            
            // Tính similarity dựa trên metadata
            double similarity = calculateMetadataSimilarity(newProduct, similarProduct);
            
            if (similarity > 0.3) {
                similarities.add(ProductSimilarity.builder()
                        .productId(newProductId)
                        .similarProductId(similarProduct.getId())
                        .similarityScore(similarity)
                        .calculationMethod("CONTENT_BASED")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        }
        
        if (!similarities.isEmpty()) {
            // Sắp xếp và lưu top 20
            similarities.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
            List<ProductSimilarity> top20 = similarities.stream()
                    .limit(20)
                    .collect(Collectors.toList());
            
            productSimilarityRepository.saveAll(top20);
            log.info("Saved {} content-based similarities for product: {}", top20.size(), newProductId);
        }
    }

    /**
     * Tính similarity dựa trên metadata (category, price, attributes)
     */
    private double calculateMetadataSimilarity(Product p1, Product p2) {
        double similarity = 0.0;
        
        // 1. Category match (weight: 0.5)
        if (p1.getCategoryId().equals(p2.getCategoryId())) {
            similarity += 0.5;
        }
        
        // 2. Price similarity (weight: 0.3)
        double price1 = p1.getFinalPrice() != null ? p1.getFinalPrice() : p1.getPrice();
        double price2 = p2.getFinalPrice() != null ? p2.getFinalPrice() : p2.getPrice();
        
        double priceDiff = Math.abs(price1 - price2);
        double avgPrice = (price1 + price2) / 2;
        double priceSimilarity = 1 - Math.min(priceDiff / avgPrice, 1.0);
        similarity += 0.3 * priceSimilarity;
        
        // 3. Rating similarity (weight: 0.2)
        double rating1 = p1.getAverageRating() != null ? p1.getAverageRating() : 0;
        double rating2 = p2.getAverageRating() != null ? p2.getAverageRating() : 0;
        
        if (rating1 > 0 && rating2 > 0) {
            double ratingDiff = Math.abs(rating1 - rating2);
            double ratingSimilarity = 1 - (ratingDiff / 5.0);
            similarity += 0.2 * ratingSimilarity;
        }
        
        return similarity;
    }

    /**
     * Popularity scoring cho user mới
     */
    public List<Product> getPopularityBasedRecommendations(int limit) {
        log.debug("Getting popularity-based recommendations");
        
        // Kết hợp nhiều metrics: interactions, reviews, ratings
        List<Product> allProducts = productRepository
                .findAllBy(PageRequest.of(0, 100))
                .getContent();
        
        Map<String, Double> popularityScores = new HashMap<>();
        
        for (Product product : allProducts) {
            if (!product.getIsPublished()) {
                continue;
            }
            
            double score = 0.0;
            
            // Interaction count
            long interactionCount = userInteractionRepository.countByProductId(product.getId());
            score += interactionCount * 1.0;
            
            // Review count
            if (product.getReviewCount() != null) {
                score += product.getReviewCount() * 2.0;
            }
            
            // Average rating
            if (product.getAverageRating() != null) {
                score += product.getAverageRating() * 5.0;
            }
            
            // Purchase count
            if (product.getPurchaseCount() != null) {
                score += product.getPurchaseCount() * 3.0;
            }
            
            popularityScores.put(product.getId(), score);
        }
        
        return allProducts.stream()
                .filter(p -> popularityScores.getOrDefault(p.getId(), 0.0) > 0)
                .sorted((p1, p2) -> {
                    double score1 = popularityScores.get(p1.getId());
                    double score2 = popularityScores.get(p2.getId());
                    return Double.compare(score2, score1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}
