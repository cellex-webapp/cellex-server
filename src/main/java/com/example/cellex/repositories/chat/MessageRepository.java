package com.example.cellex.repositories.chat;

import com.example.cellex.models.chat.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository để thao tác với collection messages
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * Lấy danh sách tin nhắn theo chat room với phân trang
     * Sắp xếp theo thời gian tạo giảm dần (tin nhắn mới nhất trước)
     */
    Page<Message> findByChatRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(String chatRoomId, Pageable pageable);

    /**
     * Lấy tin nhắn mới nhất của một chat room
     */
    Message findFirstByChatRoomIdOrderByCreatedAtDesc(String chatRoomId);

    /**
     * Đếm số tin nhắn chưa đọc của người nhận trong một chat room
     */
    @Query(value = "{'chat_room_id': ?0, 'receiver_id': ?1, 'status': {$ne: 'READ'}, 'is_deleted': false}", count = true)
    long countUnreadMessages(String chatRoomId, String receiverId);

    /**
     * Lấy danh sách tin nhắn chưa đọc của người nhận
     */
    @Query("{'receiver_id': ?0, 'status': {$ne: 'READ'}, 'is_deleted': false}")
    List<Message> findUnreadMessagesByReceiverId(String receiverId);

    /**
     * Lấy danh sách tin nhắn chưa đọc trong một chat room của người nhận
     */
    @Query("{'chat_room_id': ?0, 'receiver_id': ?1, 'status': {$ne: 'READ'}, 'is_deleted': false}")
    List<Message> findUnreadMessagesByChatRoomAndReceiver(String chatRoomId, String receiverId);
}
