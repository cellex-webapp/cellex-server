package com.example.cellex.repositories.review;

import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.models.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    // Kiểm tra user đã review product trong order này chưa
    boolean existsByUserIdAndOrderIdAndProductId(String userId, String orderId, String productId);

    // Lấy reviews của một sản phẩm
    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    // Lấy reviews của một shop
    Page<Review> findByShopIdOrderByCreatedAtDesc(String shopId, Pageable pageable);

    // Lấy reviews của một user
    Page<Review> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Lấy review theo orderId và productId
    Optional<Review> findByOrderIdAndProductId(String orderId, String productId);

    // Lấy tất cả reviews của một order
    List<Review> findByOrderId(String orderId);

    // Đếm số lượng review theo từng rating của sản phẩm
    long countByProductIdAndRating(String productId, Integer rating);

    // Lấy tất cả reviews của một sản phẩm (không phân trang)
    List<Review> findByProductId(String productId);

    // ============= PUBLIC API QUERIES (Only approved reviews) =============

    // Lấy reviews công khai của một sản phẩm (chỉ APPROVED và APPROVED_BY_ADMIN)
    Page<Review> findByProductIdAndStatusInOrderByCreatedAtDesc(
            String productId, List<ReviewStatus> statuses, Pageable pageable);

    // Lấy reviews công khai của một shop (chỉ APPROVED và APPROVED_BY_ADMIN)
    Page<Review> findByShopIdAndStatusInOrderByCreatedAtDesc(
            String shopId, List<ReviewStatus> statuses, Pageable pageable);

    // Lấy reviews công khai của một user (chỉ APPROVED và APPROVED_BY_ADMIN)
    Page<Review> findByUserIdAndStatusInOrderByCreatedAtDesc(
            String userId, List<ReviewStatus> statuses, Pageable pageable);

    // Đếm số lượng review công khai theo từng rating của sản phẩm
    long countByProductIdAndRatingAndStatusIn(String productId, Integer rating, List<ReviewStatus> statuses);

    // Lấy tất cả reviews công khai của một sản phẩm (không phân trang)
    List<Review> findByProductIdAndStatusIn(String productId, List<ReviewStatus> statuses);

    // ============= ADMIN QUERIES =============

    // Lấy reviews theo status
    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    // Lấy reviews theo nhiều status
    Page<Review> findByStatusInOrderByCreatedAtDesc(List<ReviewStatus> statuses, Pageable pageable);

    // Lấy reviews của một sản phẩm theo status (cho admin)
    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(
            String productId, ReviewStatus status, Pageable pageable);

    // Lấy reviews của một user theo status (cho admin)
    Page<Review> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId, ReviewStatus status, Pageable pageable);

    // Lấy reviews theo khoảng thời gian
    Page<Review> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Lấy reviews theo khoảng thời gian và status
    Page<Review> findByCreatedAtBetweenAndStatusOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate, ReviewStatus status, Pageable pageable);

    // Lấy reviews theo sản phẩm và khoảng thời gian
    Page<Review> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String productId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Lấy reviews theo user và khoảng thời gian
    Page<Review> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Đếm reviews theo status
    long countByStatus(ReviewStatus status);

    // Đếm reviews pending moderation
    long countByStatusIn(List<ReviewStatus> statuses);

    // ============= SEARCH QUERIES =============

    // Tìm kiếm theo tên người dùng (case-insensitive partial match)
    Page<Review> findByUserNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String userName, Pageable pageable);

    // Tìm kiếm theo tên người dùng và status
    Page<Review> findByUserNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String userName, ReviewStatus status, Pageable pageable);
}
