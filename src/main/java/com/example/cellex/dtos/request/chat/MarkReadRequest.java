package com.example.cellex.dtos.request.chat;

import lombok.*;

/**
 * DTO cho yêu cầu đánh dấu tin nhắn đã đọc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkReadRequest {

    /**
     * ID của chat room cần đánh dấu đã đọc
     */
    private String chatRoomId;
}
