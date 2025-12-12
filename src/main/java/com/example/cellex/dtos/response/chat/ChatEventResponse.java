package com.example.cellex.dtos.response.chat;

import lombok.*;

/**
 * DTO response cho WebSocket events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEventResponse {

    /**
     * Loại sự kiện: NEW_MESSAGE, MESSAGE_READ, TYPING, ONLINE_STATUS
     */
    private String eventType;

    /**
     * ID của chat room liên quan
     */
    private String chatRoomId;

    /**
     * Dữ liệu của sự kiện (có thể là MessageResponse, hoặc thông tin khác)
     */
    private Object data;

    /**
     * ID của người gửi sự kiện
     */
    private String senderId;

    /**
     * Thời gian sự kiện xảy ra
     */
    private Long timestamp;
}
