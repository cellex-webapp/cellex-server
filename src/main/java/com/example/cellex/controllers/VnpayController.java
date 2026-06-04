package com.example.cellex.controllers;

import com.example.cellex.config.VnpayConfig;
import com.example.cellex.dtos.request.vnpay.VnpayPaymentRequest;
import com.example.cellex.dtos.response.vnpay.VnpayCallbackResponse;
import com.example.cellex.dtos.response.vnpay.VnpayPaymentResponse;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.order.Order;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.services.payment.vnpay.VnpayService;
import com.example.cellex.utils.VnpayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vnpay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "VNPay Payment", description = "VNPay payment integration APIs")
public class VnpayController {

    private final VnpayService vnpayService;
    private final OrderRepository orderRepository;
    private final VnpayConfig vnpayConfig;

    @Operation(summary = "Create VNPay payment URL", description = "Generate payment URL for VNPay gateway")
    @PostMapping("/create-payment")
    public ResponseEntity<VnpayPaymentResponse> createPayment(
            @RequestBody VnpayPaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            log.info("Creating VNPay payment for order: {}", request.getOrderId());

            String ipAddress = VnpayUtil.getIpAddress(httpRequest);

            VnpayPaymentResponse response = vnpayService.createPaymentUrl(
                    request.getOrderId(),
                    request.getAmount(),
                    request.getOrderInfo(),
                    ipAddress,
                    request.getLocale()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating VNPay payment", e);
            return ResponseEntity.badRequest()
                    .body(VnpayPaymentResponse.builder()
                            .code("99")
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "VNPay IPN callback", description = "Instant Payment Notification callback from VNPay")
    @GetMapping("/ipn")
    public ResponseEntity<VnpayCallbackResponse> vnpayIpn(HttpServletRequest request) {
        try {
            log.info("Received VNPay IPN callback");

            Map<String, Object> result = vnpayService.verifyIpnCall(request);
            boolean isSuccess = (boolean) result.get("isSuccess");

            if (isSuccess) {
                String orderId = (String) result.get("orderId");
                String transactionNo = (String) result.get("transactionNo");
                String bankCode = (String) result.get("bankCode");
                String payDate = (String) result.get("payDate");
                String responseCode = (String) result.get("responseCode");

                // Update order in database
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    order.setIsPaid(true);
                    order.setPaidAt(LocalDateTime.now());
                    order.setVnpayTransactionId(transactionNo);
                    order.setVnpayResponseCode(responseCode);
                    order.setVnpayBankCode(bankCode);
                    order.setVnpayPayDate(payDate);

                    // Update order status to COMPLETED if payment successful
                    if ("00".equals(responseCode)) {
                        order.setStatus(OrderStatus.CONFIRMED);
                        order.setConfirmedAt(LocalDateTime.now());
                        order.setPaymentExpiresAt(null); // Xóa bộ đếm
                    }

                    orderRepository.save(order);
                    log.info("Order {} payment confirmed via VNPay", orderId);
                } else {
                    log.warn("Order {} not found for VNPay IPN", orderId);
                }

                return ResponseEntity.ok(VnpayCallbackResponse.builder()
                        .rspCode("00")
                        .message("Confirm Success")
                        .build());
            } else {
                log.error("VNPay IPN verification failed: {}", result.get("message"));
                return ResponseEntity.ok(VnpayCallbackResponse.builder()
                        .rspCode("97")
                        .message("Invalid Signature")
                        .build());
            }

        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            return ResponseEntity.ok(VnpayCallbackResponse.builder()
                    .rspCode("99")
                    .message("Unknown Error")
                    .build());
        }
    }

    @Operation(summary = "VNPay return URL", description = "Return URL handler after customer completes payment on VNPay")
    @GetMapping("/return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            log.info("Received VNPay return callback");

            Map<String, Object> result = vnpayService.verifyReturnUrl(request);
            boolean isSuccess = (boolean) result.get("isSuccess");
            String orderId = "";
            String responseCode = "99";

            if (isSuccess) {
                orderId = (String) result.get("orderId");
                responseCode = (String) result.get("responseCode");
                String transactionNo = (String) result.get("transactionNo");
                String bankCode = (String) result.get("bankCode");
                String payDate = (String) result.get("payDate");
                
                // Update order in database
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null && "00".equals(responseCode)) {
                    order.setIsPaid(true);
                    order.setPaidAt(LocalDateTime.now());
                    order.setVnpayTransactionId(transactionNo);
                    order.setVnpayResponseCode(responseCode);
                    order.setVnpayBankCode(bankCode);
                    order.setVnpayPayDate(payDate);
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setConfirmedAt(LocalDateTime.now());
                    order.setPaymentExpiresAt(null); // Xóa bộ đếm
                    
                    orderRepository.save(order);
                    log.info("Order {} payment confirmed and completed via VNPay", orderId);
                }
                
                log.info("Order {} payment return from VNPay with code {}", orderId, responseCode);
            }

            // Redirect to frontend with query parameters
            String redirectUrl;
            if ("00".equals(responseCode)) {
                redirectUrl = vnpayConfig.getFrontendSuccessUrl() + 
                    "?orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8) +
                    "&responseCode=" + responseCode +
                    "&message=" + URLEncoder.encode("Thanh toán thành công", StandardCharsets.UTF_8);
            } else {
                redirectUrl = vnpayConfig.getFrontendFailureUrl() + 
                    "?orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8) +
                    "&responseCode=" + responseCode +
                    "&message=" + URLEncoder.encode(vnpayService.getTransactionStatusMessage(responseCode), StandardCharsets.UTF_8);
            }

            log.info("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error processing VNPay return", e);
            String errorUrl = vnpayConfig.getFrontendFailureUrl() + 
                "?message=" + URLEncoder.encode("Lỗi xử lý thanh toán", StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }

    @Operation(summary = "Get payment status", description = "Get payment status by order ID")
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "isPaid", order.getIsPaid(),
                    "paidAt", order.getPaidAt(),
                    "vnpayTransactionId", order.getVnpayTransactionId() != null ? order.getVnpayTransactionId() : "",
                    "vnpayResponseCode", order.getVnpayResponseCode() != null ? order.getVnpayResponseCode() : "",
                    "status", order.getStatus()
            ));

        } catch (Exception e) {
            log.error("Error getting payment status", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }
}
