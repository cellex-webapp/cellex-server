package com.example.cellex.services.shipping;

import com.example.cellex.config.GhnConfig;
import com.example.cellex.dtos.request.shipping.GhnCreateOrderRequest;
import com.example.cellex.dtos.request.shipping.GhnWebhookPayload;
import com.example.cellex.dtos.request.shipping.PrepareShipmentRequest;
import com.example.cellex.dtos.response.shipping.GhnCreateOrderResponse;
import com.example.cellex.dtos.response.shipping.ShipmentResponse;
import com.example.cellex.dtos.response.shipping.TrackingResponse;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.notification.NotificationHelper;
import com.example.cellex.services.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final GhnClient ghnClient;
    private final GhnConfig ghnConfig;
    private final NotificationHelper notificationHelper;
    private final UserRepository userRepository;

    @Transactional
    public ShipmentResponse createShipment(String vendorId, String orderId, PrepareShipmentRequest request) {
        Order order = orderService.findOrderByVendor(vendorId, orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chỉ đơn hàng đã xác nhận mới có thể chuẩn bị giao");
        }

        // Build items
        List<GhnCreateOrderRequest.Item> ghnItems = order.getItems().stream()
                .map(item -> GhnCreateOrderRequest.Item.builder()
                        .name(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice().intValue())
                        .build())
                .collect(Collectors.toList());

        User user = userRepository.findById(order.getUserId()).orElse(null);
        String toName = user != null ? user.getFullName() : "Khách hàng";
        String toPhone = user != null ? user.getPhoneNumber() : "0987654321";

        // Hardcoded address for testing to bypass GHN master data requirement
        int toDistrictId = 1442; // Quận 1, HCM
        String toWardCode = "20110"; // P. Bến Nghé, Q1

        GhnCreateOrderRequest ghnRequest = GhnCreateOrderRequest.builder()
                .paymentTypeId(2) // 2: Buyer/Seller pay shipping fee? GHN API uses 1 for shop pay, 2 for buyer pay.
                .requiredNote("KHONGCHOXEMHANG")
                .note(request.getNote())
                .clientOrderCode(order.getId())
                .toName(toName)
                .toPhone(toPhone)
                .toAddress(order.getShippingAddress() != null ? order.getShippingAddress().getFullAddress() : "123 Lê Lợi, Quận 1, HCM")
                .toWardCode(toWardCode)
                .toDistrictId(toDistrictId)
                .codAmount(order.getIsPaid() ? 0 : order.getTotalAmount().intValue())
                .weight(request.getWeight())
                .length(request.getLength())
                .width(request.getWidth())
                .height(request.getHeight())
                .serviceTypeId(ghnConfig.getServiceTypeId())
                .items(ghnItems)
                .build();

        GhnCreateOrderResponse ghnResp = ghnClient.createOrder(ghnRequest);

        if (ghnResp.getCode() != 200 || ghnResp.getData() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "GHN error: " + ghnResp.getMessage());
        }

        String ghnOrderCode = ghnResp.getData().getOrderCode();
        // Fallback construct label URL if API doesn't return it directly
        String labelUrl = ghnResp.getData().getLabel() != null 
                ? ghnResp.getData().getLabel() 
                : "https://5sao.ghn.dev/print/A5/" + ghnOrderCode;

        order.setGhnOrderCode(ghnOrderCode);
        order.setGhnLabelUrl(labelUrl);
        order.setGhnTotalFee(ghnResp.getData().getTotalFee());
        order.setReadyToShipAt(LocalDateTime.now());
        
        // Tracking URL for user
        order.setTrackingUrl("https://donhang.ghn.vn/?order_code=" + ghnOrderCode);
        
        // Status update
        order.setStatus(OrderStatus.READY_TO_SHIP);
        
        Order.StatusHistory history = Order.StatusHistory.builder()
                .status(OrderStatus.READY_TO_SHIP)
                .note("Đã tạo vận đơn GHN: " + ghnOrderCode)
                .updatedBy(vendorId)
                .updatedAt(LocalDateTime.now())
                .build();
        order.getStatusHistory().add(history);
        
        orderRepository.save(order);

        if (user != null) {
            notificationHelper.notifyReadyToShip(order, user, ghnOrderCode);
        }

        return ShipmentResponse.builder()
                .ghnOrderCode(ghnOrderCode)
                .labelUrl(labelUrl)
                .expectedDelivery(LocalDateTime.now().plusDays(3)) // Dummy or parse from expectedDeliveryTime
                .build();
    }

    @Transactional
    public void processWebhook(GhnWebhookPayload payload) {
        if (payload.getClientOrderCode() == null || payload.getClientOrderCode().isEmpty()) {
            log.warn("Webhook ignored: missing client_order_code");
            return;
        }

        Order order = orderRepository.findById(payload.getClientOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Not found order from GHN webhook"));

        OrderStatus newStatus = mapGhnStatus(payload.getStatus());
        order.setCarrierStatus(payload.getStatus());
        
        Order.TrackingEvent event = Order.TrackingEvent.builder()
                .ghnStatus(payload.getStatus())
                .description(payload.getDescription())
                .warehouse(payload.getWarehouse())
                .eventTime(payload.getTime() != null ? payload.getTime() : LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();
        
        if (order.getTrackingEvents() == null) {
            order.setTrackingEvents(new ArrayList<>());
        }
        order.getTrackingEvents().add(event);

        User user = userRepository.findById(order.getUserId()).orElse(null);

        if (newStatus != null && newStatus != order.getStatus()) {
            order.setStatus(newStatus);
            
            Order.StatusHistory history = Order.StatusHistory.builder()
                    .status(newStatus)
                    .note("Cập nhật trạng thái từ GHN: " + payload.getDescription())
                    .updatedBy("GHN-Webhook")
                    .updatedAt(LocalDateTime.now())
                    .build();
            order.getStatusHistory().add(history);

            if (newStatus == OrderStatus.DELIVERED) {
                orderService.handleDelivered(order);
                if (user != null) notificationHelper.notifyOrderDelivered(order, user);
            } else if (newStatus == OrderStatus.RETURNED) {
                orderService.handleReturned(order);
                if (user != null) notificationHelper.notifyOrderReturned(order, user);
            } else if (newStatus == OrderStatus.DELIVERY_FAILED) {
                if (user != null) notificationHelper.notifyDeliveryFailed(order, user);
            } else if (newStatus == OrderStatus.SHIPPING) {
                if (user != null) notificationHelper.notifyOrderShipping(order, user);
            }
        } else {
            // Just a location update
            if (user != null) {
                notificationHelper.notifyShippingUpdate(order, user, payload.getDescription(), payload.getWarehouse());
            }
        }

        orderRepository.save(order);
    }

    private OrderStatus mapGhnStatus(String ghnStatus) {
        if (ghnStatus == null) return null;
        switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick":
            case "picking":
                return OrderStatus.READY_TO_SHIP;
            case "picked":
            case "storing":
            case "transporting":
            case "sorting":
            case "delivering":
                return OrderStatus.SHIPPING;
            case "delivered":
                return OrderStatus.DELIVERED;
            case "delivery_fail":
                return OrderStatus.DELIVERY_FAILED;
            case "waiting_to_return":
            case "return":
            case "returning":
                return OrderStatus.RETURNING;
            case "returned":
                return OrderStatus.RETURNED;
            case "cancel":
                return OrderStatus.CANCELLED;
            default:
                return null;
        }
    }

    public TrackingResponse getTracking(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        return TrackingResponse.builder()
                .ghnOrderCode(order.getGhnOrderCode())
                .trackingUrl(order.getTrackingUrl())
                .carrierStatus(order.getCarrierStatus())
                .events(order.getTrackingEvents())
                .build();
    }
}
