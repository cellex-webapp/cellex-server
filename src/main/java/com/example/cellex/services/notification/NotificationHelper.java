package com.example.cellex.services.notification;

import com.example.cellex.enums.NotificationType;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Helper service để gửi notifications cho các sự kiện trong hệ thống
 * Sử dụng service này trong các service khác khi cần gửi notification
 */
@Service
@RequiredArgsConstructor
public class NotificationHelper {

    private final NotificationService notificationService;

    /**
     * Gửi notification khi order được tạo thành công
     */
    public void notifyOrderCreated(Order order, User user) {
        String title = "Đơn hàng đã được tạo";
        String message = String.format("Đơn hàng #%s của bạn đã được tạo thành công. Tổng tiền: %,.0f VNĐ", 
                order.getOrderCode(), order.getTotalAmount());
        String metadata = String.format("{\"orderId\": \"%s\"}", order.getId());
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.ORDER_CREATED,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi order được xác nhận
     */
    public void notifyOrderConfirmed(Order order, User user) {
        String title = "Đơn hàng đã được xác nhận";
        String message = String.format("Đơn hàng #%s đã được xác nhận và đang được chuẩn bị", 
                order.getOrderCode());
        String metadata = String.format("{\"orderId\": \"%s\"}", order.getId());
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.ORDER_CONFIRMED,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi order đang được giao
     */
    public void notifyOrderShipping(Order order, User user) {
        String title = "Đơn hàng đang được giao";
        String message = String.format("Đơn hàng #%s đang trên đường giao đến bạn", 
                order.getOrderCode());
        String metadata = String.format("{\"orderId\": \"%s\"}", order.getId());
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.ORDER_SHIPPING,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi order được giao thành công
     */
    public void notifyOrderDelivered(Order order, User user) {
        String title = "Đơn hàng đã được giao";
        String message = String.format("Đơn hàng #%s đã được giao thành công. Cảm ơn bạn đã mua hàng!", 
                order.getOrderCode());
        String metadata = String.format("{\"orderId\": \"%s\"}", order.getId());
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.ORDER_DELIVERED,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi order bị hủy
     */
    public void notifyOrderCancelled(Order order, User user, String reason) {
        String title = "Đơn hàng đã bị hủy";
        String message = String.format("Đơn hàng #%s đã bị hủy. Lý do: %s", 
                order.getOrderCode(), reason != null ? reason : "Không có lý do");
        String metadata = String.format("{\"orderId\": \"%s\", \"reason\": \"%s\"}", 
                order.getId(), reason);
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.ORDER_CANCELLED,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi thanh toán thành công
     */
    public void notifyPaymentSuccess(Order order, User user) {
        String title = "Thanh toán thành công";
        String message = String.format("Thanh toán cho đơn hàng #%s đã được xử lý thành công", 
                order.getOrderCode());
        String metadata = String.format("{\"orderId\": \"%s\"}", order.getId());
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.PAYMENT_SUCCESS,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi thanh toán thất bại
     */
    public void notifyPaymentFailed(Order order, User user, String reason) {
        String title = "Thanh toán thất bại";
        String message = String.format("Thanh toán cho đơn hàng #%s không thành công. %s", 
                order.getOrderCode(), reason != null ? reason : "Vui lòng thử lại");
        String metadata = String.format("{\"orderId\": \"%s\", \"reason\": \"%s\"}", 
                order.getId(), reason);
        String actionUrl = "/orders/" + order.getId();

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.PAYMENT_FAILED,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification khi có coupon mới
     */
    public void notifyCouponAvailable(User user, String couponCode, String description) {
        String title = "Bạn có mã giảm giá mới!";
        String message = String.format("Mã giảm giá %s: %s", couponCode, description);
        String metadata = String.format("{\"couponCode\": \"%s\"}", couponCode);
        String actionUrl = "/coupons";

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.COUPON_AVAILABLE,
                metadata,
                actionUrl,
                null
        );
    }

    /**
     * Gửi notification yêu cầu đánh giá sản phẩm
     */
    public void notifyReviewRequest(Order order, User user) {
        String title = "Đánh giá đơn hàng của bạn";
        String message = String.format("Hãy cho chúng tôi biết trải nghiệm của bạn với đơn hàng #%s", 
                order.getOrderCode());
        String metadata = String.format("{\"orderId\": \"%s\"}", order.getId());
        String actionUrl = "/orders/" + order.getId() + "/review";

        notificationService.sendNotificationToUser(
                user,
                title,
                message,
                NotificationType.REVIEW_REQUEST,
                metadata,
                actionUrl,
                null
        );
    }
}
