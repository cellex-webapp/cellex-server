package com.example.cellex.repositories.chat;

import com.example.cellex.models.chat.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho AI Messages
 */
@Repository
public interface AIMessageRepository extends MongoRepository<AIMessage, String> {

    /**
     * Lấy tin nhắn theo conversation ID (cũ nhất trước - để hiển thị đúng trong chat)
     */
    List<AIMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * Lấy tin nhắn theo conversation ID với phân trang (cũ nhất trước)
     */
    Page<AIMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId, Pageable pageable);

    /**
     * Lấy N tin nhắn gần nhất của conversation (để làm context cho AI)
     */
    @Query("{'conversation_id': ?0}")
    List<AIMessage> findTopByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    /**
     * Đếm số tin nhắn trong conversation
     */
    long countByConversationId(String conversationId);

    /**
     * Lấy tất cả tin nhắn của user
     */
    List<AIMessage> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Xóa tất cả tin nhắn của conversation
     */
    void deleteByConversationId(String conversationId);
}
