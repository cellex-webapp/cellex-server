package com.example.cellex.repositories.shop;

import com.example.cellex.enums.ShopStatus;
import com.example.cellex.models.shop.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends MongoRepository<Shop, String> {
    Optional<Shop> findByVendorId(String vendorId);
    Optional<Shop> findByVendorIdAndStatus(String vendorId, ShopStatus status);
    List<Shop> findByStatus(ShopStatus status);
    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);
    boolean existsByVendorId(String vendorId);

    // ==================== Analytics Methods ====================

    /**
     * Đếm số shop theo status
     */
    long countByStatus(ShopStatus status);
}
