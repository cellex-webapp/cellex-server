package com.example.cellex.services.chat;

import com.example.cellex.dtos.request.chat.CreateChatRoomRequest;
import com.example.cellex.dtos.request.chat.MessageRequest;
import com.example.cellex.dtos.response.chat.ChatEventResponse;
import com.example.cellex.dtos.response.chat.ChatRoomResponse;
import com.example.cellex.dtos.response.chat.MessageResponse;
import com.example.cellex.enums.MessageStatus;
import com.example.cellex.enums.MessageType;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.chat.ChatRoom;
import com.example.cellex.models.chat.Message;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.chat.ChatRoomRepository;
import com.example.cellex.repositories.chat.MessageRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý logic nghiệp vụ cho chức năng Chat realtime
 * 
 * Business Rules implemented:
 * - BR-SUP-001: Validation of Routing/Communication Pairs
 * - BR-SUP-003 & BR-SUP-004: Validation of Message Content
 * 
 * @author Cellex Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Danh sách từ cấm (mocked)
    private static final List<String> PROHIBITED_WORDS = Arrays.asList("spam", "bad");

    // Độ dài tối đa của tin nhắn
    private static final int MAX_MESSAGE_LENGTH = 1000;

    /**
     * Gửi tin nhắn từ sender đến receiver
     * Thực hiện các validation theo business rules:
     * - BR-SUP-003 & BR-SUP-004: Kiểm tra nội dung tin nhắn
     * - BR-SUP-001: Kiểm tra routing hợp lệ
     *
     * @param request Thông tin tin nhắn cần gửi
     * @param sender User gửi tin nhắn
     * @return MessageResponse chứa thông tin tin nhắn đã gửi
     * @throws AppException nếu validation thất bại
     */
    @Transactional
    public MessageResponse sendMessage(MessageRequest request, User sender) {
        // Lấy thông tin người nhận
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Validate nội dung tin nhắn (BR-SUP-003 & BR-SUP-004)
        validateMessageContent(request.getContent());

        // 2. Validate routing (BR-SUP-001)
        validateCommunicationRoute(sender, receiver);

        // 3. Lấy hoặc tạo chat room
        ChatRoom chatRoom = getOrCreateChatRoom(sender, receiver);

        // 4. Tạo và lưu tin nhắn
        Message message = Message.builder()
                .chatRoomId(chatRoom.getId())
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .status(MessageStatus.SENT)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        message = messageRepository.save(message);

        // 5. Cập nhật chat room
        updateChatRoomLastMessage(chatRoom, message, sender.getId());

        // 6. Tạo response
        MessageResponse response = buildMessageResponse(message, sender, receiver);

        // 7. Gửi tin nhắn realtime qua WebSocket
        sendWebSocketMessage(response, receiver.getId());

        log.info("Message sent successfully from {} to {}", sender.getId(), receiver.getId());
        return response;
    }

    /**
     * Validate nội dung tin nhắn theo BR-SUP-003 & BR-SUP-004
     * - Kiểm tra tin nhắn không rỗng
     * - Kiểm tra độ dài không quá 1000 ký tự
     * - Kiểm tra không chứa từ cấm
     *
     * @param content Nội dung tin nhắn cần validate
     * @throws AppException nếu validation thất bại
     */
    private void validateMessageContent(String content) {
        // MSG11: Tin nhắn không được rỗng
        if (content == null || content.trim().isEmpty()) {
            log.warn("Message validation failed: empty content");
            throw new AppException(ErrorCode.MSG11_EMPTY_MESSAGE);
        }

        // MSG09/MSG12: Tin nhắn không được quá dài (tối đa 1000 ký tự)
        if (content.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Message validation failed: content too long ({} chars)", content.length());
            throw new AppException(ErrorCode.MSG12_MESSAGE_TOO_LONG);
        }

        // MSG10: Tin nhắn không được chứa từ cấm
        String lowerContent = content.toLowerCase();
        for (String prohibitedWord : PROHIBITED_WORDS) {
            if (lowerContent.contains(prohibitedWord)) {
                log.warn("Message validation failed: contains prohibited word '{}'", prohibitedWord);
                throw new AppException(ErrorCode.MSG10_INAPPROPRIATE_CONTENT);
            }
        }
    }

    /**
     * Validate routing/communication pairs theo BR-SUP-001
     * 
     * Các cặp được phép:
     * - USER (Client) -> VENDOR
     * - USER (Client) -> ADMIN
     * - VENDOR -> ADMIN
     * - VENDOR -> USER (trả lời khách hàng)
     * - ADMIN -> USER (hỗ trợ khách hàng)
     * - ADMIN -> VENDOR (hỗ trợ vendor)
     * 
     * Cặp bị cấm:
     * - USER -> USER (Client không được chat với Client khác)
     *
     * @param sender Người gửi
     * @param receiver Người nhận
     * @throws AppException nếu routing không hợp lệ
     */
    private void validateCommunicationRoute(User sender, User receiver) {
        Role senderRole = sender.getRole();
        Role receiverRole = receiver.getRole();

        // MSG09: Cấm USER gửi tin nhắn cho USER khác
        if (senderRole == Role.USER && receiverRole == Role.USER) {
            log.warn("Communication validation failed: USER {} cannot send to USER {}", 
                    sender.getId(), receiver.getId());
            throw new AppException(ErrorCode.MSG09_SENDING_FAILED);
        }

        // Các trường hợp hợp lệ:
        // 1. USER -> VENDOR: Khách hàng hỏi cửa hàng
        // 2. USER -> ADMIN: Khách hàng liên hệ hỗ trợ
        // 3. VENDOR -> USER: Cửa hàng trả lời khách hàng
        // 4. VENDOR -> ADMIN: Cửa hàng liên hệ hỗ trợ
        // 5. ADMIN -> USER: Admin hỗ trợ khách hàng
        // 6. ADMIN -> VENDOR: Admin hỗ trợ cửa hàng

        log.debug("Communication route validated: {} ({}) -> {} ({})", 
                sender.getId(), senderRole, receiver.getId(), receiverRole);
    }

    /**
     * Lấy chat room hiện có hoặc tạo mới nếu chưa tồn tại
     *
     * @param participant1 Người tham gia 1
     * @param participant2 Người tham gia 2
     * @return ChatRoom
     */
    @Transactional
    public ChatRoom getOrCreateChatRoom(User participant1, User participant2) {
        // Tìm chat room hiện có
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByParticipants(
                participant1.getId(), participant2.getId());

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        // Tạo chat room mới
        ChatRoom newRoom = ChatRoom.builder()
                .participantOneId(participant1.getId())
                .participantTwoId(participant2.getId())
                .participantOneName(participant1.getFullName())
                .participantTwoName(participant2.getFullName())
                .participantOneAvatar(participant1.getAvatarUrl())
                .participantTwoAvatar(participant2.getAvatarUrl())
                .participantOneRole(participant1.getRole())
                .participantTwoRole(participant2.getRole())
                .unreadCountOne(0)
                .unreadCountTwo(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return chatRoomRepository.save(newRoom);
    }

    /**
     * Cập nhật thông tin tin nhắn cuối cùng của chat room
     *
     * @param chatRoom Chat room cần cập nhật
     * @param message Tin nhắn mới nhất
     * @param senderId ID người gửi
     */
    private void updateChatRoomLastMessage(ChatRoom chatRoom, Message message, String senderId) {
        chatRoom.setLastMessage(message.getContent());
        chatRoom.setLastMessageSenderId(senderId);
        chatRoom.setLastMessageAt(message.getCreatedAt());
        chatRoom.setUpdatedAt(LocalDateTime.now());

        // Tăng unread count cho người nhận
        if (senderId.equals(chatRoom.getParticipantOneId())) {
            chatRoom.setUnreadCountTwo(chatRoom.getUnreadCountTwo() + 1);
        } else {
            chatRoom.setUnreadCountOne(chatRoom.getUnreadCountOne() + 1);
        }

        chatRoomRepository.save(chatRoom);
    }

    /**
     * Build MessageResponse từ Message entity
     */
    private MessageResponse buildMessageResponse(Message message, User sender, User receiver) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .receiverId(receiver.getId())
                .receiverName(receiver.getFullName())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
    }

    /**
     * Gửi tin nhắn qua WebSocket đến người nhận
     *
     * @param response MessageResponse cần gửi
     * @param receiverId ID người nhận
     */
    private void sendWebSocketMessage(MessageResponse response, String receiverId) {
        try {
            ChatEventResponse event = ChatEventResponse.builder()
                    .eventType("NEW_MESSAGE")
                    .chatRoomId(response.getChatRoomId())
                    .data(response)
                    .senderId(response.getSenderId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Gửi đến destination cá nhân của người nhận
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/messages",
                    event
            );

            // Gửi đến topic của chat room để cả hai người đều nhận được
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + response.getChatRoomId(),
                    event
            );

            log.debug("WebSocket message sent to user {} and room {}", 
                    receiverId, response.getChatRoomId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Lấy danh sách chat rooms của user hiện tại
     *
     * @param userId ID của user
     * @param page Số trang
     * @param size Kích thước trang
     * @return Page của ChatRoomResponse
     */
    public Page<ChatRoomResponse> getChatRooms(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<ChatRoom> chatRooms = chatRoomRepository.findByParticipantId(userId, pageable);

        return chatRooms.map(room -> buildChatRoomResponse(room, userId));
    }

    /**
     * Lấy tất cả chat rooms của user (không phân trang)
     *
     * @param userId ID của user
     * @return List của ChatRoomResponse
     */
    public List<ChatRoomResponse> getAllChatRooms(String userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByParticipantId(userId);
        return chatRooms.stream()
                .map(room -> buildChatRoomResponse(room, userId))
                .sorted((a, b) -> {
                    if (a.getLastMessageAt() == null) return 1;
                    if (b.getLastMessageAt() == null) return -1;
                    return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                })
                .toList();
    }

    /**
     * Build ChatRoomResponse từ ChatRoom entity
     * Tự động xác định partner (người chat cùng) dựa vào userId hiện tại
     */
    private ChatRoomResponse buildChatRoomResponse(ChatRoom room, String currentUserId) {
        boolean isParticipantOne = room.getParticipantOneId().equals(currentUserId);

        return ChatRoomResponse.builder()
                .id(room.getId())
                .partnerId(isParticipantOne ? room.getParticipantTwoId() : room.getParticipantOneId())
                .partnerName(isParticipantOne ? room.getParticipantTwoName() : room.getParticipantOneName())
                .partnerAvatar(isParticipantOne ? room.getParticipantTwoAvatar() : room.getParticipantOneAvatar())
                .partnerRole(isParticipantOne ? room.getParticipantTwoRole() : room.getParticipantOneRole())
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .lastMessageSenderId(room.getLastMessageSenderId())
                .unreadCount(isParticipantOne ? room.getUnreadCountOne() : room.getUnreadCountTwo())
                .createdAt(room.getCreatedAt())
                .build();
    }

    /**
     * Lấy lịch sử tin nhắn của một chat room
     *
     * @param chatRoomId ID của chat room
     * @param userId ID của user yêu cầu (để kiểm tra quyền)
     * @param page Số trang
     * @param size Kích thước trang
     * @return Page của MessageResponse
     */
    public Page<MessageResponse> getMessages(String chatRoomId, String userId, int page, int size) {
        // Kiểm tra chat room tồn tại và user có quyền truy cập
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // Kiểm tra user có phải là participant của chat room không
        if (!chatRoom.getParticipantOneId().equals(userId) && 
            !chatRoom.getParticipantTwoId().equals(userId)) {
            throw new AppException(ErrorCode.MSG08_NO_PERMISSION);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findByChatRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(
                chatRoomId, pageable);

        return messages.map(msg -> {
            User sender = userRepository.findById(msg.getSenderId()).orElse(null);
            User receiver = userRepository.findById(msg.getReceiverId()).orElse(null);
            return MessageResponse.builder()
                    .id(msg.getId())
                    .chatRoomId(msg.getChatRoomId())
                    .senderId(msg.getSenderId())
                    .senderName(sender != null ? sender.getFullName() : "Unknown")
                    .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                    .receiverId(msg.getReceiverId())
                    .receiverName(receiver != null ? receiver.getFullName() : "Unknown")
                    .content(msg.getContent())
                    .type(msg.getType())
                    .status(msg.getStatus())
                    .attachmentUrl(msg.getAttachmentUrl())
                    .attachmentName(msg.getAttachmentName())
                    .createdAt(msg.getCreatedAt())
                    .readAt(msg.getReadAt())
                    .build();
        });
    }

    /**
     * Đánh dấu tất cả tin nhắn trong chat room là đã đọc
     *
     * @param chatRoomId ID của chat room
     * @param userId ID của user (người đọc)
     */
    @Transactional
    public void markMessagesAsRead(String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // Kiểm tra user có quyền
        if (!chatRoom.getParticipantOneId().equals(userId) && 
            !chatRoom.getParticipantTwoId().equals(userId)) {
            throw new AppException(ErrorCode.MSG08_NO_PERMISSION);
        }

        // Lấy và cập nhật các tin nhắn chưa đọc
        List<Message> unreadMessages = messageRepository
                .findUnreadMessagesByChatRoomAndReceiver(chatRoomId, userId);

        LocalDateTime now = LocalDateTime.now();
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(now);
        }
        messageRepository.saveAll(unreadMessages);

        // Reset unread count
        if (chatRoom.getParticipantOneId().equals(userId)) {
            chatRoom.setUnreadCountOne(0);
        } else {
            chatRoom.setUnreadCountTwo(0);
        }
        chatRoomRepository.save(chatRoom);

        // Gửi event đã đọc qua WebSocket
        String partnerId = chatRoom.getParticipantOneId().equals(userId) 
                ? chatRoom.getParticipantTwoId() 
                : chatRoom.getParticipantOneId();

        sendReadReceiptEvent(chatRoomId, userId, partnerId);

        log.info("Marked {} messages as read in room {} for user {}", 
                unreadMessages.size(), chatRoomId, userId);
    }

    /**
     * Gửi sự kiện đã đọc qua WebSocket
     */
    private void sendReadReceiptEvent(String chatRoomId, String readerId, String partnerId) {
        try {
            ChatEventResponse event = ChatEventResponse.builder()
                    .eventType("MESSAGE_READ")
                    .chatRoomId(chatRoomId)
                    .senderId(readerId)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSendToUser(partnerId, "/queue/messages", event);
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, event);
        } catch (Exception e) {
            log.error("Failed to send read receipt event: {}", e.getMessage());
        }
    }

    /**
     * Tạo hoặc lấy chat room với một user cụ thể
     *
     * @param request Request chứa participantId
     * @param currentUser User hiện tại
     * @return ChatRoomResponse
     */
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(CreateChatRoomRequest request, User currentUser) {
        User partner = userRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate routing
        validateCommunicationRoute(currentUser, partner);

        ChatRoom chatRoom = getOrCreateChatRoom(currentUser, partner);
        return buildChatRoomResponse(chatRoom, currentUser.getId());
    }

    /**
     * Gửi sự kiện đang gõ (typing) qua WebSocket
     *
     * @param chatRoomId ID của chat room
     * @param userId ID của user đang gõ
     * @param isTyping Đang gõ hay không
     */
    public void sendTypingEvent(String chatRoomId, String userId, boolean isTyping) {
        try {
            ChatEventResponse event = ChatEventResponse.builder()
                    .eventType("TYPING")
                    .chatRoomId(chatRoomId)
                    .senderId(userId)
                    .data(isTyping)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, event);
        } catch (Exception e) {
            log.error("Failed to send typing event: {}", e.getMessage());
        }
    }

    /**
     * Đếm tổng số tin nhắn chưa đọc của user
     *
     * @param userId ID của user
     * @return Số tin nhắn chưa đọc
     */
    public long countTotalUnreadMessages(String userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByParticipantId(userId);
        long total = 0;
        for (ChatRoom room : chatRooms) {
            if (room.getParticipantOneId().equals(userId)) {
                total += room.getUnreadCountOne();
            } else {
                total += room.getUnreadCountTwo();
            }
        }
        return total;
    }

    /**
     * Đồng bộ avatar và fullName của user vào tất cả chat rooms liên quan
     * Được gọi khi user cập nhật profile
     *
     * @param userId ID của user đã cập nhật profile
     * @param newAvatarUrl Avatar URL mới
     * @param newFullName Tên đầy đủ mới
     */
    @Transactional
    public void syncUserInfoInChatRooms(String userId, String newAvatarUrl, String newFullName) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByParticipantId(userId);
        
        if (chatRooms.isEmpty()) {
            log.debug("No chat rooms found for user: {}", userId);
            return;
        }

        int updatedCount = 0;
        for (ChatRoom room : chatRooms) {
            boolean updated = false;
            
            // Cập nhật nếu user là participant one
            if (room.getParticipantOneId().equals(userId)) {
                if (newAvatarUrl != null && !newAvatarUrl.equals(room.getParticipantOneAvatar())) {
                    room.setParticipantOneAvatar(newAvatarUrl);
                    updated = true;
                }
                if (newFullName != null && !newFullName.equals(room.getParticipantOneName())) {
                    room.setParticipantOneName(newFullName);
                    updated = true;
                }
            }
            // Cập nhật nếu user là participant two
            else if (room.getParticipantTwoId().equals(userId)) {
                if (newAvatarUrl != null && !newAvatarUrl.equals(room.getParticipantTwoAvatar())) {
                    room.setParticipantTwoAvatar(newAvatarUrl);
                    updated = true;
                }
                if (newFullName != null && !newFullName.equals(room.getParticipantTwoName())) {
                    room.setParticipantTwoName(newFullName);
                    updated = true;
                }
            }
            
            if (updated) {
                room.setUpdatedAt(LocalDateTime.now());
                chatRoomRepository.save(room);
                updatedCount++;
            }
        }
        
        log.info("✅ Synced user info for {} chat rooms (userId: {})", updatedCount, userId);
    }
}
