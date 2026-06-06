package com.example.cellex.repositories.warranty;

import com.example.cellex.enums.WarrantyStatus;
import com.example.cellex.models.warranty.WarrantyClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, UUID> {

    // ==================== Các hàm base dùng UUID ====================
    List<WarrantyClaim> findByUserId(UUID userId);
    List<WarrantyClaim> findByShopId(UUID shopId);
    List<WarrantyClaim> findByUserIdAndStatus(UUID userId, WarrantyStatus status);
    List<WarrantyClaim> findByShopIdAndStatus(UUID shopId, WarrantyStatus status);

    // ==================== Paginated queries (cho vendor dashboard) ====================
    Page<WarrantyClaim> findByShopId(UUID shopId, Pageable pageable);
    Page<WarrantyClaim> findByShopIdAndStatus(UUID shopId, WarrantyStatus status, Pageable pageable);

    // ==================== Backward-compat String ID lookup (Học từ OrderRepository) ====================
    default List<WarrantyClaim> findByUserId(String userId) {
        try {
            return findByUserId(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    default List<WarrantyClaim> findByShopId(String shopId) {
        try {
            return findByShopId(UUID.fromString(shopId));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    default Page<WarrantyClaim> findByShopId(String shopId, Pageable pageable) {
        try {
            return findByShopId(UUID.fromString(shopId), pageable);
        } catch (IllegalArgumentException e) {
            return Page.empty(pageable);
        }
    }

    default Page<WarrantyClaim> findByShopIdAndStatus(String shopId, WarrantyStatus status, Pageable pageable) {
        try {
            return findByShopIdAndStatus(UUID.fromString(shopId), status, pageable);
        } catch (IllegalArgumentException e) {
            return Page.empty(pageable);
        }
    }
}