package com.example.cellex.enums;

public enum NotificationType {
    SYSTEM,           // Thông báo hệ thống từ admin
    ORDER_CREATED,    // Đơn hàng mới được tạo
    ORDER_CONFIRMED,  // Đơn hàng được xác nhận
    ORDER_READY_TO_SHIP, // Đơn chờ GHN lấy
    ORDER_SHIPPING,   // Đơn hàng đang giao
    ORDER_SHIPPING_UPDATE, // Cập nhật hành trình (mỗi bưu cục)
    ORDER_DELIVERED,  // Đơn hàng đã giao
    ORDER_DELIVERY_FAILED, // Giao không thành công
    ORDER_RETURNED,   // Hàng đã hoàn về kho
    ORDER_CANCELLED,  // Đơn hàng bị hủy
    PAYMENT_SUCCESS,  // Thanh toán thành công
    PAYMENT_FAILED,   // Thanh toán thất bại
    COUPON_AVAILABLE, // Có coupon mới
    PROMOTION,        // Khuyến mãi
    PRODUCT_RESTOCK,  // Sản phẩm có hàng trở lại
    REVIEW_REQUEST,   // Yêu cầu đánh giá
    REVIEW,           // Đánh giá sản phẩm
    CUSTOM,           // Tùy chỉnh
    CHAT_MESSAGE,     // Tin nhắn chat
    WARRANTY_CREATED, // Khách tạo yêu cầu bảo hành
    WARRANTY_UPDATED  // Shop phản hồi / cập nhật bảo hành
}
