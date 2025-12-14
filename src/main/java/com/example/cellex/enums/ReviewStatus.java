package com.example.cellex.enums;

public enum ReviewStatus {
    PENDING_MODERATION,     // Đang chờ kiểm duyệt tự động
    APPROVED,               // Đã được duyệt tự động
    REJECTED_AUTO,          // Bị từ chối bởi hệ thống tự động
    APPROVED_BY_ADMIN,      // Được admin duyệt thủ công
    REJECTED_BY_ADMIN,      // Bị admin từ chối
    HIDDEN                  // Bị ẩn bởi admin
}
