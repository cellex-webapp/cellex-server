package com.example.cellex.enums;

public enum OrderStatus {
    PENDING,           // Chờ xác nhận
    CONFIRMED,         // Đã xác nhận
    READY_TO_SHIP,     // Đã chuẩn bị, đang chờ GHN lấy hàng
    SHIPPING,          // Đang vận chuyển
    DELIVERED,         // Đã giao hàng
    DELIVERY_FAILED,   // GHN giao không thành công
    RETURNING,         // GHN đang hoàn hàng về kho
    RETURNED,          // Hàng đã về kho → cộng lại tồn kho
    CANCELLED          // Đã hủy
}

