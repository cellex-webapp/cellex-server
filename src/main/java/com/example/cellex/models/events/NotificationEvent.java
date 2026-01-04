package com.example.cellex.models.events;

import com.example.cellex.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String recipientId;    // ID người nhận
    private String recipientEmail; // Email người nhận (để gửi mail)
    private String recipientName;  // Tên người nhận (để hiển thị trong mail)
    private String title;          // Tiêu đề
    private String message;        // Nội dung
    private NotificationType type; // CHAT, ORDER, SYSTEM, etc.
    private String imageUrl;       // Ảnh đính kèm (nếu có)
    private String actionUrl;      // Link trỏ đến khi click
    private Map<String, Object> metadata; // Dữ liệu phụ
}