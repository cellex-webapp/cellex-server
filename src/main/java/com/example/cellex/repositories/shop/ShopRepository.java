package com.example.cellex.repositories.shop;

import com.example.cellex.enums.ShopStatus;
import com.example.cellex.models.shop.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Shop entity (PostgreSQL/Supabase).
 * Migrated from MongoRepository. All method signatures preserved for backward compat.
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    // ==================== Backward-compat String ID lookup ====================

    /**
     * Backward-compatible findById(String) — converts String → UUID internally.
     */
    default Optional<Shop> findById(String id) {
        try {
            return findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ==================== Vendor lookups ====================

    @Query("SELECT s FROM Shop s WHERE s.ownerUuid = :ownerUuid")
    Optional<Shop> findByOwnerUuid(@Param("ownerUuid") UUID ownerUuid);

    /**
     * Backward-compat: findByVendorId(String) — used by 12+ services.
     */
    default Optional<Shop> findByVendorId(String vendorId) {
        try {
            return findByOwnerUuid(UUID.fromString(vendorId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Query("SELECT s FROM Shop s WHERE s.ownerUuid = :ownerUuid AND s.status = :status")
    Optional<Shop> findByOwnerUuidAndStatus(@Param("ownerUuid") UUID ownerUuid, @Param("status") ShopStatus status);

    /**
     * Backward-compat: findByVendorIdAndStatus(String, ShopStatus).
     */
    default Optional<Shop> findByVendorIdAndStatus(String vendorId, ShopStatus status) {
        try {
            return findByOwnerUuidAndStatus(UUID.fromString(vendorId), status);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Shop s WHERE s.ownerUuid = :ownerUuid")
    boolean existsByOwnerUuid(@Param("ownerUuid") UUID ownerUuid);

    /**
     * Backward-compat: existsByVendorId(String).
     */
    default boolean existsByVendorId(String vendorId) {
        try {
            return existsByOwnerUuid(UUID.fromString(vendorId));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ==================== Status queries ====================

    List<Shop> findByStatus(ShopStatus status);

    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);

    // ==================== Analytics Methods ====================

    /**
     * Đếm số shop theo status
     */
    long countByStatus(ShopStatus status);

    /**
     * Đếm số shop theo status và tạo trong khoảng thời gian
     */
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.status = :status AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    long countByStatusAndCreatedAtBetween(
            @Param("status") ShopStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số shop theo status và tạo trước thời điểm
     */
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.status = :status AND s.createdAt <= :beforeDate")
    long countByStatusAndCreatedAtBefore(
            @Param("status") ShopStatus status,
            @Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Đếm số shop tạo trong khoảng thời gian
     */
    @Query("SELECT COUNT(s) FROM Shop s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate")
    long countByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
