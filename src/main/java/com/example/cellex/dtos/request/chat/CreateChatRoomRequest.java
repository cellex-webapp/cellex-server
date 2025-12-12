package com.example.cellex.dtos.request.chat;

import lombok.*;

/**
 * DTO cho yêu cầu tạo hoặc lấy chat room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatRoomRequest {

    /**
     * ID của người muốn chat cùng
     */
    private String participantId;
}
