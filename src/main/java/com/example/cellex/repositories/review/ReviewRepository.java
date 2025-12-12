package com.example.cellex.repositories.review;

import com.example.cellex.models.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

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
}
