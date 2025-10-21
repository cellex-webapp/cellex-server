package com.example.cellex.repositories;

import com.example.cellex.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Page<Product> findByCategoryIdAndIsPublishedTrue(String categoryId, Pageable pageable);

    Page<Product> findByShopIdAndIsPublishedTrue(String shopId, Pageable pageable);

    Page<Product> findByShopId(String shopId, Pageable pageable);

    @Query("{'name': {$regex: ?0, $options: 'i'}, 'isPublished': true}")
    Page<Product> findByNameContainingIgnoreCaseAndIsPublishedTrue(String name, Pageable pageable);

    // Tìm sản phẩm theo thuộc tính cụ thể (để so sánh)
    @Query("{'categoryId': ?0, 'attributeValues.attributeKey': ?1, 'attributeValues.value': ?2, 'isPublished': true}")
    List<Product> findByCategoryAndAttributeValue(String categoryId, String attributeKey, String attributeValue);

    // Lấy tất cả sản phẩm với phân trang
    Page<Product> findAllBy(Pageable pageable);
}
