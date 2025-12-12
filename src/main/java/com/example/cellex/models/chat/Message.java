package com.example.cellex.models.chat;

import com.example.cellex.enums.MessageStatus;
import com.example.cellex.enums.MessageType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho một tin nhắn trong hệ thống chat
 */
@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    /**
     * ID của phòng chat chứa tin nhắn này
     */
    @Indexed
    @Field("chat_room_id")
    private String chatRoomId;

    /**
     * ID của người gửi tin nhắn
     */
    @Indexed
    @Field("sender_id")
    private String senderId;

    /**
     * ID của người nhận tin nhắn
     */
    @Indexed
    @Field("receiver_id")
    private String receiverId;

    /**
     * Nội dung tin nhắn
     */
    @Field("content")
    private String content;

    /**
     * Loại tin nhắn (TEXT, IMAGE, FILE, SYSTEM)
     */
    @Field("type")
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    /**
     * Trạng thái tin nhắn (SENT, DELIVERED, READ)
     */
    @Field("status")
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    /**
     * URL của file đính kèm (nếu có)
     */
    @Field("attachment_url")
    private String attachmentUrl;

    /**
     * Tên file đính kèm (nếu có)
     */
    @Field("attachment_name")
    private String attachmentName;

    /**
     * Tin nhắn đã bị xóa hay chưa (soft delete)
     */
    @Field("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời gian gửi tin nhắn
     */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * Thời gian đọc tin nhắn
     */
    @Field("read_at")
    private LocalDateTime readAt;
}
