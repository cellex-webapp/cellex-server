package com.example.cellex.models.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity lưu trữ tin nhắn AI trong MongoDB
 */
@Document(collection = "ai_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "user_conversation_idx", def = "{'user_id': 1, 'conversation_id': 1}"),
    @CompoundIndex(name = "conversation_created_idx", def = "{'conversation_id': 1, 'created_at': -1}")
})
public class AIMessage {

    @Id
    private String id;

    /**
     * ID của user
     */
    @Indexed
    @Field("user_id")
    private String userId;

    /**
     * ID của conversation (để nhóm các tin nhắn lại)
     */
    @Indexed
    @Field("conversation_id")
    private String conversationId;

    /**
     * Loại tin nhắn: USER hoặc AI
     */
    @Field("message_type")
    private AIMessageType messageType;

    /**
     * Nội dung tin nhắn
     */
    @Field("content")
    private String content;

    /**
     * Role của user khi gửi tin nhắn
     */
    @Field("user_role")
    private String userRole;

    /**
     * Shop ID (nếu là VENDOR)
     */
    @Field("shop_id")
    private String shopId;

    /**
     * Metadata bổ sung (productIds, chartData, etc.)
     */
    @Field("metadata")
    private Map<String, Object> metadata;

    /**
     * Function được gọi (nếu có)
     */
    @Field("function_called")
    private String functionCalled;

    /**
     * Kết quả từ function (nếu có)
     */
    @Field("function_result")
    private String functionResult;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * Loại tin nhắn AI
     */
    public enum AIMessageType {
        USER,       // Tin nhắn từ user
        AI,         // Phản hồi từ AI
        SYSTEM      // Tin nhắn hệ thống (context, prompt)
    }
}
