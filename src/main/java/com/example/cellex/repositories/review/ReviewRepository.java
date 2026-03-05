package com.example.cellex.repositories.review;

import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.models.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserIdAndOrderIdAndProductId(String userId, String orderId, String productId);

    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    Page<Review> findByShopIdOrderByCreatedAtDesc(String shopId, Pageable pageable);
    Page<Review> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<Review> findByOrderIdAndProductId(String orderId, String productId);
    List<Review> findByOrderId(String orderId);

    long countByProductIdAndRating(String productId, Integer rating);
    List<Review> findByProductId(String productId);

    // ============= PUBLIC API QUERIES (Only approved reviews) =============

    Page<Review> findByProductIdAndStatusInOrderByCreatedAtDesc(
            String productId, List<ReviewStatus> statuses, Pageable pageable);

    Page<Review> findByShopIdAndStatusInOrderByCreatedAtDesc(
            String shopId, List<ReviewStatus> statuses, Pageable pageable);

    Page<Review> findByUserIdAndStatusInOrderByCreatedAtDesc(
            String userId, List<ReviewStatus> statuses, Pageable pageable);

    long countByProductIdAndRatingAndStatusIn(String productId, Integer rating, List<ReviewStatus> statuses);

    List<Review> findByProductIdAndStatusIn(String productId, List<ReviewStatus> statuses);

    // ============= ADMIN QUERIES =============

    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);
    Page<Review> findByStatusInOrderByCreatedAtDesc(List<ReviewStatus> statuses, Pageable pageable);

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(
            String productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId, ReviewStatus status, Pageable pageable);

    Page<Review> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Review> findByCreatedAtBetweenAndStatusOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String productId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Review> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    long countByStatus(ReviewStatus status);
    long countByStatusIn(List<ReviewStatus> statuses);

    // ============= SEARCH QUERIES =============

    Page<Review> findByUserNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String userName, Pageable pageable);

    Page<Review> findByUserNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String userName, ReviewStatus status, Pageable pageable);

    // ==================== Backward-compatible methods ====================

    default Optional<Review> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default boolean existsById(String id) {
        return existsById(UUID.fromString(id));
    }

    default void deleteById(String id) {
        deleteById(UUID.fromString(id));
    }
}
