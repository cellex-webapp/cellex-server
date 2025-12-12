package com.example.cellex.dtos.request.chat;

import com.example.cellex.enums.MessageType;
import lombok.*;

/**
 * DTO cho yêu cầu gửi tin nhắn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {

    /**
     * ID của người nhận tin nhắn
     * Bắt buộc phải có
     */
    private String receiverId;

    /**
     * Nội dung tin nhắn
     * Bắt buộc phải có, tối đa 1000 ký tự
     */
    private String content;

    /**
     * Loại tin nhắn (TEXT, IMAGE, FILE)
     * Mặc định là TEXT
     */
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    /**
     * URL của file đính kèm (nếu type là IMAGE hoặc FILE)
     */
    private String attachmentUrl;

    /**
     * Tên file đính kèm (nếu type là FILE)
     */
    private String attachmentName;
}
