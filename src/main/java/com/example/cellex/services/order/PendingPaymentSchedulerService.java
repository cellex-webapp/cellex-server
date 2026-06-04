package com.example.cellex.services.order;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.services.product.ProductSkuService;
import com.example.cellex.repositories.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentSchedulerService {

    private final OrderRepository orderRepository;
    private final ProductSkuService productSkuService;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelay = 60_000) // Chạy mỗi 1 phút
    @Transactional
    public void cancelExpiredPendingOrders() {
        log.info("Running scheduled task: Cancel expired pending orders...");

        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(OrderStatus.PENDING, LocalDateTime.now());

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired pending orders", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // Hủy order
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason("Quá thời gian thanh toán (10 phút)");
                order.setCancelledAt(LocalDateTime.now());
                order.setPaymentExpiresAt(null); // Xóa timer để không lặp lại

                // Cập nhật lịch sử
                Order.StatusHistory history = Order.StatusHistory.builder()
                        .status(OrderStatus.CANCELLED)
                        .note("Hệ thống tự động hủy do quá hạn thanh toán")
                        .updatedBy("SYSTEM")
                        .updatedAt(LocalDateTime.now())
                        .build();
                order.getStatusHistory().add(history);

                // Release stock
                for (OrderItem item : order.getItems()) {
                    if (item.getSkuId() != null && !item.getSkuId().isBlank()) {
                        productSkuService.releaseReservedStock(item.getSkuId(), item.getQuantity());
                        refreshProductStockFromSkus(item.getProductId());
                    } else {
                        productRepository.findById(item.getProductId()).ifPresent(product -> {
                            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                            productRepository.save(product);
                        });
                    }
                }

                orderRepository.save(order);
                log.info("Successfully cancelled expired order: {}", order.getId());

            } catch (Exception e) {
                log.error("Failed to cancel expired order: {}", order.getId(), e);
            }
        }
    }

    private void refreshProductStockFromSkus(String productId) {
        if (productId == null || productId.isBlank()) return;
        productRepository.findById(productId).ifPresent(product -> {
            int available = productSkuService.sumAvailableStockByProduct(productId);
            product.setStockQuantity(available);
            productRepository.save(product);
        });
    }
}
