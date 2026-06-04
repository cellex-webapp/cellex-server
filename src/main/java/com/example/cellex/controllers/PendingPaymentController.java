package com.example.cellex.controllers;

import com.example.cellex.dtos.response.order.OrderResponse;
import com.example.cellex.dtos.response.vnpay.VnpayPaymentResponse;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.order.Order;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.services.payment.vnpay.VnpayService;
import com.example.cellex.utils.VnpayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders/pending-payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pending Payment", description = "APIs for managing pending payments")
public class PendingPaymentController {

    private final OrderRepository orderRepository;
    private final VnpayService vnpayService;

    @Operation(summary = "Lấy danh sách đơn hàng đang chờ thanh toán", description = "Lấy danh sách các đơn hàng VNPay đang pending và chưa hết hạn 10 phút")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getPendingPaymentOrders(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        
        com.example.cellex.models.user.User user = (com.example.cellex.models.user.User) authentication.getPrincipal();
        String userId = user.getId();
        log.info("Fetching pending payment orders for user: {}", userId);

        List<Order> pendingOrders = orderRepository.findPendingPaymentOrdersByUserId(
                userId, OrderStatus.PENDING, LocalDateTime.now());

        List<OrderResponse> responses = pendingOrders.stream()
                .map(this::mapToSimpleResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Lấy lại URL thanh toán VNPay cho đơn pending", description = "Tạo lại link VNPay cho đơn hàng đang chờ thanh toán")
    @PostMapping("/{orderId}/payment-url")
    public ResponseEntity<VnpayPaymentResponse> getRepaymentUrl(
            @PathVariable String orderId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        com.example.cellex.models.user.User user = (com.example.cellex.models.user.User) authentication.getPrincipal();
        String userId = user.getId();
        log.info("Generating repayment URL for order: {} by user: {}", orderId, userId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }

        if (order.getStatus() != OrderStatus.PENDING || Boolean.TRUE.equals(order.getIsPaid())) {
            return ResponseEntity.badRequest().body(VnpayPaymentResponse.builder()
                    .code("99")
                    .message("Đơn hàng không ở trạng thái chờ thanh toán")
                    .build());
        }

        if (order.getPaymentExpiresAt() != null && order.getPaymentExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(VnpayPaymentResponse.builder()
                    .code("99")
                    .message("Đơn hàng đã hết hạn thanh toán")
                    .build());
        }

        try {
            String ipAddress = VnpayUtil.getIpAddress(request);
            String orderInfo = "Thanh toan lai don hang " + order.getOrderCode();

            VnpayPaymentResponse response = vnpayService.createPaymentUrl(
                    order.getId(),
                    order.getTotalAmount().longValue(),
                    orderInfo,
                    ipAddress,
                    "vn"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating repayment URL", e);
            return ResponseEntity.internalServerError().body(VnpayPaymentResponse.builder()
                    .code("99")
                    .message("Lỗi tạo URL thanh toán: " + e.getMessage())
                    .build());
        }
    }

    private OrderResponse mapToSimpleResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .isPaid(order.getIsPaid())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .paymentExpiresAt(order.getPaymentExpiresAt())
                .build();
    }
}
