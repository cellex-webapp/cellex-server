package com.example.cellex.dtos.response.chat;

import com.example.cellex.enums.MessageStatus;
import com.example.cellex.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO response cho tin nhắn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    /**
     * ID của tin nhắn
     */
    private String id;

    /**
     * ID của chat room chứa tin nhắn
     */
    private String chatRoomId;

    /**
     * ID của người gửi
     */
    private String senderId;

    /**
     * Tên người gửi
     */
    private String senderName;

    /**
     * Avatar URL người gửi
     */
    private String senderAvatar;

    /**
     * ID của người nhận
     */
    private String receiverId;

    /**
     * Tên người nhận
     */
    private String receiverName;

    /**
     * Nội dung tin nhắn
     */
    private String content;

    /**
     * Loại tin nhắn
     */
    private MessageType type;

    /**
     * Trạng thái tin nhắn
     */
    private MessageStatus status;

    /**
     * URL file đính kèm
     */
    private String attachmentUrl;

    /**
     * Tên file đính kèm
     */
    private String attachmentName;

    /**
     * Thời gian gửi tin nhắn
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian đọc tin nhắn
     */
    private LocalDateTime readAt;
}
