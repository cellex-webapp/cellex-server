package com.example.cellex.repositories.recommendation;

import com.example.cellex.models.recommendation.ProductSimilarity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSimilarityRepository extends MongoRepository<ProductSimilarity, String> {

    // Lấy top N sản phẩm tương tự
    List<ProductSimilarity> findTopByProductIdOrderBySimilarityScoreDesc(String productId);

    // Lấy N sản phẩm tương tự nhất
    @Query("{'product_id': ?0}")
    List<ProductSimilarity> findByProductIdOrderBySimilarityScoreDesc(String productId);

    // Xóa similarity cũ của một product
    void deleteByProductId(String productId);

    // Kiểm tra similarity đã tồn tại chưa
    boolean existsByProductIdAndSimilarProductId(String productId, String similarProductId);
}
