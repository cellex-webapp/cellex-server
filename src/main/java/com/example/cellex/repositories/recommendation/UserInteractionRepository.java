package com.example.cellex.repositories.recommendation;

import com.example.cellex.models.recommendation.UserInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInteractionRepository extends MongoRepository<UserInteraction, String> {

    // Tìm interaction giữa user và product
    Optional<UserInteraction> findByUserIdAndProductId(String userId, String productId);

    // Lấy tất cả interaction của user
    List<UserInteraction> findByUserId(String userId);

    // Lấy interaction của user theo điểm số
    List<UserInteraction> findByUserIdOrderByTotalScoreDesc(String userId);

    // Lấy user đã tương tác với product này
    List<UserInteraction> findByProductId(String productId);

    // Đếm số user đã tương tác với product
    long countByProductId(String productId);

    // Kiểm tra user có interaction không
    boolean existsByUserId(String userId);

    // Lấy top categories user quan tâm
    @Query("{'user_id': ?0}")
    List<UserInteraction> findByUserIdGroupByCategoryId(String userId);
}
