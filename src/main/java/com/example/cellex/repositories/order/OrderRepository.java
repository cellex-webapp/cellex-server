package com.example.cellex.repositories.order;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

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
}
