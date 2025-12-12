package com.example.cellex.repositories.order;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    List<Order> findByUserId(String userId, Sort sort);

    Page<Order> findByShopId(String shopId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);

    List<Order> findByUserIdAndStatus(String userId, OrderStatus status, Sort sort);

    Page<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Pageable pageable);

    List<Order> findByShopId(String shopId, Sort sort);

    List<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Sort sort);

    List<Order> findByStatus(OrderStatus status, Sort sort);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, String userId);

    Optional<Order> findByIdAndShopId(String id, String shopId);

    List<Order> findByUserCouponId(String userCouponId);

    // ==================== Analytics Methods ====================

    /**
     * Đếm số đơn hàng theo shopId và status
     */
    long countByShopIdAndStatus(String shopId, OrderStatus status);

    /**
     * Đếm tổng số đơn hàng của shop
     */
    long countByShopId(String shopId);

    /**
     * Đếm đơn hàng theo status (toàn hệ thống)
     */
    long countByStatus(OrderStatus status);

    /**
     * Tìm các đơn hàng của shop đã hoàn thành và thanh toán trong khoảng thời gian
     */
    @Query("{'shop_id': ?0, 'status': 'DELIVERED', 'is_paid': true, 'created_at': {$gte: ?1, $lte: ?2}}")
    List<Order> findCompletedPaidOrdersByShopIdAndDateRange(String shopId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tìm tất cả đơn hàng của shop đã hoàn thành và thanh toán
     */
    @Query("{'shop_id': ?0, 'status': 'DELIVERED', 'is_paid': true}")
    List<Order> findCompletedPaidOrdersByShopId(String shopId);

    /**
     * Tìm tất cả đơn hàng đã hoàn thành và thanh toán (toàn hệ thống)
     */
    @Query("{'status': 'DELIVERED', 'is_paid': true}")
    List<Order> findAllCompletedPaidOrders();

    /**
     * Tìm tất cả đơn hàng đã hoàn thành và thanh toán trong khoảng thời gian
     */
    @Query("{'status': 'DELIVERED', 'is_paid': true, 'created_at': {$gte: ?0, $lte: ?1}}")
    List<Order> findCompletedPaidOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Đếm đơn hàng trong khoảng thời gian (toàn hệ thống)
     */
    @Query(value = "{'created_at': {$gte: ?0, $lte: ?1}}", count = true)
    long countOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tìm các đơn hàng gần đây nhất
     */
    List<Order> findTop5ByOrderByCreatedAtDesc();

    /**
     * Tìm các đơn hàng gần đây nhất của shop
     */
    List<Order> findTop5ByShopIdOrderByCreatedAtDesc(String shopId);

    /**
     * Đếm đơn hàng theo status và khoảng thời gian
     */
    @Query(value = "{'status': ?0, 'created_at': {$gte: ?1, $lte: ?2}}", count = true)
    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Đếm đơn hàng hoàn thành và đã thanh toán trong khoảng thời gian
     */
    @Query(value = "{'status': 'DELIVERED', 'is_paid': true, 'created_at': {$gte: ?0, $lte: ?1}}", count = true)
    long countCompletedPaidOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tìm tất cả đơn hàng trong khoảng thời gian
     */
    @Query("{'created_at': {$gte: ?0, $lte: ?1}}")
    List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
