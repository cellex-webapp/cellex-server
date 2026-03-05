package com.example.cellex.repositories.order;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Order entity (PostgreSQL/Supabase).
 * Migrated from MongoRepository. All method signatures preserved for backward compat.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // ==================== Backward-compat String ID lookup ====================

    default Optional<Order> findById(String id) {
        try {
            return findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ==================== User queries (UUID-based internal + String backward-compat) ====================

    @Query("SELECT o FROM Order o WHERE o.userUuid = :userUuid")
    Page<Order> findByUserUuid(@Param("userUuid") UUID userUuid, Pageable pageable);

    default Page<Order> findByUserId(String userId, Pageable pageable) {
        try { return findByUserUuid(UUID.fromString(userId), pageable); }
        catch (IllegalArgumentException e) { return Page.empty(pageable); }
    }

    @Query("SELECT o FROM Order o WHERE o.userUuid = :userUuid")
    List<Order> findByUserUuid(@Param("userUuid") UUID userUuid, Sort sort);

    default List<Order> findByUserId(String userId, Sort sort) {
        try { return findByUserUuid(UUID.fromString(userId), sort); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    @Query("SELECT o FROM Order o WHERE o.userUuid = :userUuid AND o.status = :status")
    Page<Order> findByUserUuidAndStatus(@Param("userUuid") UUID userUuid, @Param("status") OrderStatus status, Pageable pageable);

    default Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable) {
        try { return findByUserUuidAndStatus(UUID.fromString(userId), status, pageable); }
        catch (IllegalArgumentException e) { return Page.empty(pageable); }
    }

    @Query("SELECT o FROM Order o WHERE o.userUuid = :userUuid AND o.status = :status")
    List<Order> findByUserUuidAndStatus(@Param("userUuid") UUID userUuid, @Param("status") OrderStatus status, Sort sort);

    default List<Order> findByUserIdAndStatus(String userId, OrderStatus status, Sort sort) {
        try { return findByUserUuidAndStatus(UUID.fromString(userId), status, sort); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    // ==================== Shop queries ====================

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid")
    Page<Order> findByShopUuid(@Param("shopUuid") UUID shopUuid, Pageable pageable);

    default Page<Order> findByShopId(String shopId, Pageable pageable) {
        try { return findByShopUuid(UUID.fromString(shopId), pageable); }
        catch (IllegalArgumentException e) { return Page.empty(pageable); }
    }

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid AND o.status = :status")
    Page<Order> findByShopUuidAndStatus(@Param("shopUuid") UUID shopUuid, @Param("status") OrderStatus status, Pageable pageable);

    default Page<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Pageable pageable) {
        try { return findByShopUuidAndStatus(UUID.fromString(shopId), status, pageable); }
        catch (IllegalArgumentException e) { return Page.empty(pageable); }
    }

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid")
    List<Order> findByShopUuid(@Param("shopUuid") UUID shopUuid, Sort sort);

    default List<Order> findByShopId(String shopId, Sort sort) {
        try { return findByShopUuid(UUID.fromString(shopId), sort); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid AND o.status = :status")
    List<Order> findByShopUuidAndStatus(@Param("shopUuid") UUID shopUuid, @Param("status") OrderStatus status, Sort sort);

    default List<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Sort sort) {
        try { return findByShopUuidAndStatus(UUID.fromString(shopId), status, sort); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    // ==================== Combined lookups ====================

    @Query("SELECT o FROM Order o WHERE o.uuid = :uuid AND o.userUuid = :userUuid")
    Optional<Order> findByUuidAndUserUuid(@Param("uuid") UUID uuid, @Param("userUuid") UUID userUuid);

    default Optional<Order> findByIdAndUserId(String id, String userId) {
        try { return findByUuidAndUserUuid(UUID.fromString(id), UUID.fromString(userId)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    @Query("SELECT o FROM Order o WHERE o.uuid = :uuid AND o.shopUuid = :shopUuid")
    Optional<Order> findByUuidAndShopUuid(@Param("uuid") UUID uuid, @Param("shopUuid") UUID shopUuid);

    default Optional<Order> findByIdAndShopId(String id, String shopId) {
        try { return findByUuidAndShopUuid(UUID.fromString(id), UUID.fromString(shopId)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    // ==================== Other lookups ====================

    List<Order> findByUserCouponId(String userCouponId);

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByStatus(OrderStatus status, Sort sort);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // ==================== Analytics Methods ====================

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shopUuid = :shopUuid AND o.status = :status")
    long countByShopUuidAndStatus(@Param("shopUuid") UUID shopUuid, @Param("status") OrderStatus status);

    default long countByShopIdAndStatus(String shopId, OrderStatus status) {
        try { return countByShopUuidAndStatus(UUID.fromString(shopId), status); }
        catch (IllegalArgumentException e) { return 0; }
    }

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shopUuid = :shopUuid")
    long countByShopUuid(@Param("shopUuid") UUID shopUuid);

    default long countByShopId(String shopId) {
        try { return countByShopUuid(UUID.fromString(shopId)); }
        catch (IllegalArgumentException e) { return 0; }
    }

    long countByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid AND o.status = 'DELIVERED' AND o.isPaid = true AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<Order> findCompletedPaidOrdersByShopUuidAndDateRange(@Param("shopUuid") UUID shopUuid, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    default List<Order> findCompletedPaidOrdersByShopIdAndDateRange(String shopId, LocalDateTime startDate, LocalDateTime endDate) {
        try { return findCompletedPaidOrdersByShopUuidAndDateRange(UUID.fromString(shopId), startDate, endDate); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid AND o.status = 'DELIVERED' AND o.isPaid = true")
    List<Order> findCompletedPaidOrdersByShopUuid(@Param("shopUuid") UUID shopUuid);

    default List<Order> findCompletedPaidOrdersByShopId(String shopId) {
        try { return findCompletedPaidOrdersByShopUuid(UUID.fromString(shopId)); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    @Query("SELECT o FROM Order o WHERE o.status = 'DELIVERED' AND o.isPaid = true")
    List<Order> findAllCompletedPaidOrders();

    @Query("SELECT o FROM Order o WHERE o.status = 'DELIVERED' AND o.isPaid = true AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<Order> findCompletedPaidOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid ORDER BY o.createdAt DESC LIMIT 5")
    List<Order> findTop5ByShopUuidOrderByCreatedAtDesc(@Param("shopUuid") UUID shopUuid);

    default List<Order> findTop5ByShopIdOrderByCreatedAtDesc(String shopId) {
        try { return findTop5ByShopUuidOrderByCreatedAtDesc(UUID.fromString(shopId)); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countByStatusAndCreatedAtBetween(@Param("status") OrderStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED' AND o.isPaid = true AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countCompletedPaidOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.shopUuid = :shopUuid AND o.createdAt > :createdAt")
    List<Order> findByShopUuidAndCreatedAtAfter(@Param("shopUuid") UUID shopUuid, @Param("createdAt") LocalDateTime createdAt);

    default List<Order> findByShopIdAndCreatedAtAfter(String shopId, LocalDateTime createdAt) {
        try { return findByShopUuidAndCreatedAtAfter(UUID.fromString(shopId), createdAt); }
        catch (IllegalArgumentException e) { return List.of(); }
    }

    List<Order> findByCreatedAtAfter(LocalDateTime createdAt);
}
