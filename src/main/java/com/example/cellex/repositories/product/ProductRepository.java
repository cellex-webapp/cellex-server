package com.example.cellex.repositories.product;

import com.example.cellex.models.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Page<Product> findByCategoryIdAndIsPublishedTrue(String categoryId, Pageable pageable);

    @Query("{'categoryId': ?0, 'isPublished': true, 'shopId': {$in: ?1}}")
    Page<Product> findByCategoryIdAndIsPublishedTrueAndShopIdIn(String categoryId, List<String> shopIds, Pageable pageable);

    Page<Product> findByShopIdAndIsPublishedTrue(String shopId, Pageable pageable);

    Page<Product> findByShopId(String shopId, Pageable pageable);

    @Query("{'name': {$regex: ?0, $options: 'i'}, 'isPublished': true}")
    Page<Product> findByNameContainingIgnoreCaseAndIsPublishedTrue(String name, Pageable pageable);

    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<Product> findByNameContainingIgnoreCase(String name);

    // Tìm sản phẩm theo thuộc tính cụ thể (để so sánh)
    @Query("{'categoryId': ?0, 'attributeValues.attributeKey': ?1, 'attributeValues.value': ?2, 'isPublished': true}")
    List<Product> findByCategoryAndAttributeValue(String categoryId, String attributeKey, String attributeValue);

    // Lấy tất cả sản phẩm với phân trang
    Page<Product> findAllBy(Pageable pageable);

    int countByShopId(String shopId);

    // ==================== Analytics Methods ====================

    /**
     * Đếm sản phẩm đã publish của shop
     */
    long countByShopIdAndIsPublishedTrue(String shopId);

    /**
     * Đếm tổng số sản phẩm đã publish (toàn hệ thống)
     */
    long countByIsPublishedTrue();

    /**
     * Đếm sản phẩm đã publish tạo trong khoảng thời gian
     */
    @Query(value = "{'isPublished': true, 'createdAt': {$gte: ?0, $lte: ?1}}", count = true)
    long countByIsPublishedTrueAndCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Đếm sản phẩm đã publish tạo trước thời điểm
     */
    @Query(value = "{'isPublished': true, 'createdAt': {$lte: ?0}}", count = true)
    long countByIsPublishedTrueAndCreatedAtBefore(java.time.LocalDateTime beforeDate);

    /**
     * Tìm tất cả sản phẩm đã publish
     */
    @Query("{'isPublished': true}")
    List<Product> findAllByIsPublishedTrue();

    /**
     * Tìm tất cả sản phẩm đã publish với phân trang
     */
    @Query("{'isPublished': true}")
    List<Product> findAllByIsPublishedTrue(org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm tất cả sản phẩm đã publish của shop
     */
    List<Product> findByShopIdAndIsPublishedTrue(String shopId);
}
