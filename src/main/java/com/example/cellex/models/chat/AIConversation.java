package com.example.cellex.models.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ AI Conversation
 */
@Document(collection = "ai_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversation {

    @Id
    private String id;

    /**
     * ID của user sở hữu conversation
     */
    @Indexed
    @Field("user_id")
    private String userId;

    /**
     * Tiêu đề conversation (tự động tạo từ tin nhắn đầu tiên)
     */
    @Field("title")
    private String title;

    /**
     * Role của user trong conversation này
     */
    @Field("user_role")
    private String userRole;

    /**
     * Shop ID (nếu là VENDOR)
     */
    @Field("shop_id")
    private String shopId;

    /**
     * Số lượng tin nhắn trong conversation
     */
    @Field("message_count")
    @Builder.Default
    private Integer messageCount = 0;

    /**
     * Tin nhắn cuối cùng (preview)
     */
    @Field("last_message")
    private String lastMessage;

    /**
     * Thời gian tin nhắn cuối
     */
    @Field("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * Conversation có đang active không
     */
    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
