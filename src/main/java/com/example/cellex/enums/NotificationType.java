package com.example.cellex.enums;

public enum NotificationType {
    SYSTEM,           // Thông báo hệ thống từ admin
    ORDER_CREATED,    // Đơn hàng mới được tạo
    ORDER_CONFIRMED,  // Đơn hàng được xác nhận
    ORDER_SHIPPING,   // Đơn hàng đang giao
    ORDER_DELIVERED,  // Đơn hàng đã giao
    ORDER_CANCELLED,  // Đơn hàng bị hủy
    PAYMENT_SUCCESS,  // Thanh toán thành công
    PAYMENT_FAILED,   // Thanh toán thất bại
    COUPON_AVAILABLE, // Có coupon mới
    PROMOTION,        // Khuyến mãi
    PRODUCT_RESTOCK,  // Sản phẩm có hàng trở lại
    REVIEW_REQUEST,   // Yêu cầu đánh giá
    REVIEW, CUSTOM           // Tùy chỉnh
}
