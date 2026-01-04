package com.example.cellex.repositories.chat;

import com.example.cellex.models.chat.AIConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho AI Conversations
 */
@Repository
public interface AIConversationRepository extends MongoRepository<AIConversation, String> {

    /**
     * Lấy tất cả conversations của user (mới nhất trước)
     */
    List<AIConversation> findByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(String userId);

    /**
     * Lấy conversations của user với phân trang
     */
    Page<AIConversation> findByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(String userId, Pageable pageable);

    /**
     * Tìm conversation theo ID và user ID
     */
    Optional<AIConversation> findByIdAndUserId(String id, String userId);

    /**
     * Đếm số conversations của user
     */
    long countByUserIdAndIsActiveTrue(String userId);

    /**
     * Tìm conversation active gần nhất của user
     */
    Optional<AIConversation> findFirstByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(String userId);
}
