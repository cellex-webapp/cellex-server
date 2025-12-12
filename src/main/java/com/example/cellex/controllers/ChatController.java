package com.example.cellex.controllers;

import com.example.cellex.dtos.request.chat.CreateChatRoomRequest;
import com.example.cellex.dtos.request.chat.MarkReadRequest;
import com.example.cellex.dtos.request.chat.MessageRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.chat.ChatRoomResponse;
import com.example.cellex.dtos.response.chat.MessageResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller xử lý các API và WebSocket messages cho chức năng Chat
 * 
 * REST APIs:
 * - GET /api/v1/chat/rooms: Lấy danh sách chat rooms
 * - GET /api/v1/chat/rooms/all: Lấy tất cả chat rooms (không phân trang)
 * - POST /api/v1/chat/rooms: Tạo hoặc lấy chat room với một user
 * - GET /api/v1/chat/rooms/{roomId}/messages: Lấy tin nhắn của một room
 * - POST /api/v1/chat/messages: Gửi tin nhắn (REST alternative)
 * - POST /api/v1/chat/rooms/{roomId}/read: Đánh dấu đã đọc
 * - GET /api/v1/chat/unread-count: Đếm tin nhắn chưa đọc
 * 
 * WebSocket Endpoints:
 * - /app/chat.send: Gửi tin nhắn
 * - /app/chat.typing/{roomId}: Thông báo đang gõ
 * - /app/chat.read/{roomId}: Đánh dấu đã đọc
 * 
 * @author Cellex Team
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "APIs cho chức năng Chat Realtime")
public class ChatController {

    private final ChatService chatService;

    // ==================== REST APIs ====================

    /**
     * Lấy danh sách chat rooms của user hiện tại (có phân trang)
     */
    @GetMapping("/rooms")
    @Operation(summary = "Lấy danh sách chat rooms", 
               description = "Lấy danh sách các cuộc hội thoại của user hiện tại với phân trang")
    public ResponseEntity<ApiResponse<PageResponse<ChatRoomResponse>>> getChatRooms(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChatRoomResponse> chatRooms = chatService.getChatRooms(currentUser.getId(), page, size);
        
        PageResponse<ChatRoomResponse> pageResponse = PageResponse.<ChatRoomResponse>builder()
                .content(chatRooms.getContent())
                .currentPage(chatRooms.getNumber())
                .pageSize(chatRooms.getSize())
                .totalElements(chatRooms.getTotalElements())
                .totalPages(chatRooms.getTotalPages())
                .isLast(chatRooms.isLast())
                .isFirst(chatRooms.isFirst())
                .hasNext(chatRooms.hasNext())
                .hasPrevious(chatRooms.hasPrevious())
                .isEmpty(chatRooms.isEmpty())
                .build();

        return ResponseEntity.ok(ApiResponse.<PageResponse<ChatRoomResponse>>builder()
                .code(200)
                .message("Lấy danh sách chat rooms thành công")
                .result(pageResponse)
                .build());
    }

    /**
     * Lấy tất cả chat rooms (không phân trang)
     */
    @GetMapping("/rooms/all")
    @Operation(summary = "Lấy tất cả chat rooms", 
               description = "Lấy tất cả các cuộc hội thoại của user hiện tại")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getAllChatRooms(
            @AuthenticationPrincipal User currentUser
    ) {
        List<ChatRoomResponse> chatRooms = chatService.getAllChatRooms(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.<List<ChatRoomResponse>>builder()
                .code(200)
                .message("Lấy danh sách chat rooms thành công")
                .result(chatRooms)
                .build());
    }

    /**
     * Tạo hoặc lấy chat room với một user cụ thể
     */
    @PostMapping("/rooms")
    @Operation(summary = "Tạo/Lấy chat room", 
               description = "Tạo mới hoặc lấy chat room hiện có với một user")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetChatRoom(
            @AuthenticationPrincipal User currentUser,
            @RequestBody CreateChatRoomRequest request
    ) {
        ChatRoomResponse chatRoom = chatService.createOrGetChatRoom(request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.<ChatRoomResponse>builder()
                .code(200)
                .message("Tạo/Lấy chat room thành công")
                .result(chatRoom)
                .build());
    }

    /**
     * Lấy lịch sử tin nhắn của một chat room
     */
    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Lấy tin nhắn", 
               description = "Lấy lịch sử tin nhắn của một cuộc hội thoại với phân trang")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của chat room") @PathVariable String roomId,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "50") int size
    ) {
        Page<MessageResponse> messages = chatService.getMessages(roomId, currentUser.getId(), page, size);
        
        PageResponse<MessageResponse> pageResponse = PageResponse.<MessageResponse>builder()
                .content(messages.getContent())
                .currentPage(messages.getNumber())
                .pageSize(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .isLast(messages.isLast())
                .isFirst(messages.isFirst())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .isEmpty(messages.isEmpty())
                .build();

        return ResponseEntity.ok(ApiResponse.<PageResponse<MessageResponse>>builder()
                .code(200)
                .message("Lấy tin nhắn thành công")
                .result(pageResponse)
                .build());
    }

    /**
     * Gửi tin nhắn qua REST API (alternative cho WebSocket)
     */
    @PostMapping("/messages")
    @Operation(summary = "Gửi tin nhắn", 
               description = "Gửi tin nhắn đến một user (có thể dùng thay thế WebSocket)")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal User currentUser,
            @RequestBody MessageRequest request
    ) {
        MessageResponse message = chatService.sendMessage(request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.<MessageResponse>builder()
                .code(200)
                .message("Gửi tin nhắn thành công")
                .result(message)
                .build());
    }

    /**
     * Đánh dấu tất cả tin nhắn trong chat room là đã đọc
     */
    @PostMapping("/rooms/{roomId}/read")
    @Operation(summary = "Đánh dấu đã đọc", 
               description = "Đánh dấu tất cả tin nhắn trong cuộc hội thoại là đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của chat room") @PathVariable String roomId
    ) {
        chatService.markMessagesAsRead(roomId, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Đã đánh dấu đã đọc thành công")
                .build());
    }

    /**
     * Đếm tổng số tin nhắn chưa đọc
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Đếm tin nhắn chưa đọc", 
               description = "Đếm tổng số tin nhắn chưa đọc của user")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser
    ) {
        long count = chatService.countTotalUnreadMessages(currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .code(200)
                .message("Lấy số tin nhắn chưa đọc thành công")
                .result(Map.of("unreadCount", count))
                .build());
    }

    // ==================== WebSocket Message Handlers ====================

    /**
     * Xử lý tin nhắn gửi qua WebSocket
     * Client gửi đến: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleSendMessage(
            @Payload MessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        User currentUser = getCurrentUserFromHeader(headerAccessor);
        if (currentUser != null) {
            chatService.sendMessage(request, currentUser);
            log.debug("WebSocket message processed from user: {}", currentUser.getId());
        }
    }

    /**
     * Xử lý sự kiện đang gõ
     * Client gửi đến: /app/chat.typing/{roomId}
     */
    @MessageMapping("/chat.typing/{roomId}")
    public void handleTyping(
            @DestinationVariable String roomId,
            @Payload Map<String, Boolean> payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        User currentUser = getCurrentUserFromHeader(headerAccessor);
        if (currentUser != null) {
            boolean isTyping = payload.getOrDefault("typing", false);
            chatService.sendTypingEvent(roomId, currentUser.getId(), isTyping);
        }
    }

    /**
     * Xử lý sự kiện đánh dấu đã đọc qua WebSocket
     * Client gửi đến: /app/chat.read/{roomId}
     */
    @MessageMapping("/chat.read/{roomId}")
    public void handleMarkRead(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        User currentUser = getCurrentUserFromHeader(headerAccessor);
        if (currentUser != null) {
            chatService.markMessagesAsRead(roomId, currentUser.getId());
        }
    }

    /**
     * Lấy User từ WebSocket session
     */
    private User getCurrentUserFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() != null) {
            Object principal = headerAccessor.getUser();
            if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
                if (auth.getPrincipal() instanceof User user) {
                    return user;
                }
            }
        }
        return null;
    }
}
