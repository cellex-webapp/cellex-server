package com.example.cellex.services.recommendation;

import com.example.cellex.models.recommendation.ProductSimilarity;
import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.repositories.recommendation.ProductSimilarityRepository;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tính toán Item-based Collaborative Filtering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborativeFilteringService {

    private final UserInteractionRepository userInteractionRepository;
    private final ProductSimilarityRepository productSimilarityRepository;

    /**
     * Tính toán Item-based Collaborative Filtering (Offline computation)
     * Tính cosine similarity giữa các sản phẩm dựa trên user interaction
     */
    @Transactional
    public void computeItemSimilarities() {
        log.info("Starting item-based collaborative filtering computation...");

        // 1. Lấy tất cả interactions
        List<UserInteraction> allInteractions = userInteractionRepository.findAll();
        
        if (allInteractions.isEmpty()) {
            log.warn("No user interactions found. Skipping CF computation.");
            return;
        }

        // 2. Tạo ma trận user-item (Map<ProductId, Map<UserId, Score>>)
        Map<String, Map<String, Double>> productUserMatrix = buildProductUserMatrix(allInteractions);

        // 3. Lấy danh sách tất cả productIds
        List<String> productIds = new ArrayList<>(productUserMatrix.keySet());
        
        log.info("Computing similarities for {} products...", productIds.size());

        // 4. Tính cosine similarity cho mỗi cặp sản phẩm
        int totalPairs = 0;
        for (int i = 0; i < productIds.size(); i++) {
            String productId1 = productIds.get(i);
            
            // Lấy top-K similar products cho productId1
            List<ProductSimilarity> similarities = new ArrayList<>();
            
            for (int j = 0; j < productIds.size(); j++) {
                if (i == j) continue; // Bỏ qua chính nó
                
                String productId2 = productIds.get(j);
                
                // Tính cosine similarity
                double similarity = calculateCosineSimilarity(
                    productUserMatrix.get(productId1),
                    productUserMatrix.get(productId2)
                );
                
                // Chỉ lưu nếu similarity > threshold (0.1)
                if (similarity > 0.1) {
                    similarities.add(ProductSimilarity.builder()
                            .productId(productId1)
                            .similarProductId(productId2)
                            .similarityScore(similarity)
                            .calculationMethod("COLLABORATIVE_FILTERING")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build());
                    totalPairs++;
                }
            }
            
            // Lưu top-50 similar products
            if (!similarities.isEmpty()) {
                // Xóa similarities cũ của product này
                productSimilarityRepository.deleteByProductId(productId1);
                
                // Sắp xếp và lấy top 50
                similarities.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
                List<ProductSimilarity> top50 = similarities.stream()
                        .limit(50)
                        .collect(Collectors.toList());
                
                productSimilarityRepository.saveAll(top50);
            }
        }

        log.info("Item-based CF computation completed. Total similarity pairs: {}", totalPairs);
    }

    /**
     * Xây dựng ma trận Product-User từ interactions
     */
    private Map<String, Map<String, Double>> buildProductUserMatrix(List<UserInteraction> interactions) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        for (UserInteraction interaction : interactions) {
            String productId = interaction.getProductId();
            String userId = interaction.getUserId();
            Double score = interaction.getTotalScore();

            if (score == null || score <= 0) continue;

            matrix.computeIfAbsent(productId, k -> new HashMap<>())
                  .put(userId, score);
        }

        return matrix;
    }

    /**
     * Tính Cosine Similarity giữa 2 vectors
     * Cosine Similarity = (A · B) / (||A|| * ||B||)
     */
    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        // Tìm users chung
        Set<String> commonUsers = new HashSet<>(vector1.keySet());
        commonUsers.retainAll(vector2.keySet());

        if (commonUsers.isEmpty()) {
            return 0.0;
        }

        // Tính dot product và magnitudes
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (String user : commonUsers) {
            double score1 = vector1.get(user);
            double score2 = vector2.get(user);

            dotProduct += score1 * score2;
        }

        for (Double score : vector1.values()) {
            magnitude1 += score * score;
        }

        for (Double score : vector2.values()) {
            magnitude2 += score * score;
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Lấy sản phẩm tương tự cho một sản phẩm
     */
    public List<ProductSimilarity> getSimilarProducts(String productId, int limit) {
        List<ProductSimilarity> similarities = 
            productSimilarityRepository.findByProductIdOrderBySimilarityScoreDesc(productId);
        
        return similarities.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
