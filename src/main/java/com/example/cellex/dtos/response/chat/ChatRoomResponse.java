package com.example.cellex.dtos.response.chat;

import com.example.cellex.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO response cho chat room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {

    /**
     * ID của chat room
     */
    private String id;

    /**
     * ID của người chat cùng (không phải current user)
     */
    private String partnerId;

    /**
     * Tên của người chat cùng
     */
    private String partnerName;

    /**
     * Avatar URL của người chat cùng
     */
    private String partnerAvatar;

    /**
     * Role của người chat cùng
     */
    private Role partnerRole;

    /**
     * Tin nhắn cuối cùng (preview)
     */
    private String lastMessage;

    /**
     * Thời gian tin nhắn cuối cùng
     */
    private LocalDateTime lastMessageAt;

    /**
     * ID của người gửi tin nhắn cuối cùng
     */
    private String lastMessageSenderId;

    /**
     * Số tin nhắn chưa đọc
     */
    private Integer unreadCount;

    /**
     * Thời gian tạo chat room
     */
    private LocalDateTime createdAt;
}
