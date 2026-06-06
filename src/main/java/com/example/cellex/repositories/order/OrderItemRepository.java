package com.example.cellex.repositories.order;

import com.example.cellex.models.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    
    // Spring Data JPA sẽ tự động cung cấp sẵn các hàm như findById, save, v.v.
    
}