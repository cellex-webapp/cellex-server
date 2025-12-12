package com.example.cellex.repositories.recommendation;

import com.example.cellex.models.recommendation.Recommendation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {

    // Lấy recommendations cho user theo score
    List<Recommendation> findByUserIdOrderByRecommendationScoreDesc(String userId, Pageable pageable);

    // Lấy recommendations theo reason
    List<Recommendation> findByUserIdAndRecommendationReasonOrderByRecommendationScoreDesc(
            String userId, String reason, Pageable pageable);

    // Xóa recommendations cũ của user
    void deleteByUserId(String userId);

    // Kiểm tra user có recommendations không
    boolean existsByUserId(String userId);

    // Lấy tất cả recommendations của user
    List<Recommendation> findByUserId(String userId);
}
