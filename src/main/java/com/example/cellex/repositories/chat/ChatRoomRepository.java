package com.example.cellex.repositories.chat;

import com.example.cellex.models.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository để thao tác với collection chat_rooms
 */
@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    /**
     * Tìm chat room theo cặp participant (không phân biệt thứ tự)
     */
    @Query("{'$or': [" +
           "{'participant_one_id': ?0, 'participant_two_id': ?1}, " +
           "{'participant_one_id': ?1, 'participant_two_id': ?0}" +
           "]}")
    Optional<ChatRoom> findByParticipants(String participantOneId, String participantTwoId);

    /**
     * Lấy danh sách chat rooms của một user với phân trang
     * Sắp xếp theo tin nhắn cuối cùng giảm dần
     */
    @Query("{'$or': [{'participant_one_id': ?0}, {'participant_two_id': ?0}], 'is_active': true}")
    Page<ChatRoom> findByParticipantId(String participantId, Pageable pageable);

    /**
     * Lấy danh sách tất cả chat rooms của một user
     */
    @Query("{'$or': [{'participant_one_id': ?0}, {'participant_two_id': ?0}], 'is_active': true}")
    java.util.List<ChatRoom> findAllByParticipantId(String participantId);

    /**
     * Kiểm tra xem chat room có tồn tại giữa hai user không
     */
    @Query(value = "{'$or': [" +
           "{'participant_one_id': ?0, 'participant_two_id': ?1}, " +
           "{'participant_one_id': ?1, 'participant_two_id': ?0}" +
           "]}", exists = true)
    boolean existsByParticipants(String participantOneId, String participantTwoId);
}
