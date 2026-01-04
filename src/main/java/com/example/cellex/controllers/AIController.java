package com.example.cellex.controllers;

import com.example.cellex.dtos.request.ai.AIChatRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.ai.AIChatResponse;
import com.example.cellex.models.chat.AIConversation;
import com.example.cellex.models.chat.AIMessage;
import com.example.cellex.models.user.User;
import com.example.cellex.services.ai.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API cho AI Chatbot
 * 
 * REST APIs:
 * - POST /api/v1/ai/chat: Gửi tin nhắn cho AI
 * - GET /api/v1/ai/conversations: Lấy danh sách conversations
 * - GET /api/v1/ai/conversations/{id}/messages: Lấy tin nhắn của conversation
 * - DELETE /api/v1/ai/conversations/{id}: Xóa conversation
 * 
 * @author Cellex Team
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Chat", description = "APIs cho Cellex AI Assistant")
public class AIController {

    private final AIService aiService;

    /**
     * Gửi tin nhắn cho AI và nhận phản hồi
     */
    @PostMapping("/chat")
    @Operation(summary = "Chat với AI", 
               description = "Gửi tin nhắn cho AI Assistant và nhận phản hồi thông minh dựa trên role của user")
    public ResponseEntity<ApiResponse<AIChatResponse>> chat(
            @AuthenticationPrincipal User currentUser,
            @RequestBody AIChatRequest request
    ) {
        log.info("AI Chat request from user: {}, role: {}", currentUser.getId(), currentUser.getRole());
        
        AIChatResponse response = aiService.chat(request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.<AIChatResponse>builder()
                .code(200)
                .message("Thành công")
                .result(response)
                .build());
    }

    /**
     * Lấy danh sách conversations của user
     */
    @GetMapping("/conversations")
    @Operation(summary = "Lấy danh sách conversations", 
               description = "Lấy lịch sử các cuộc hội thoại AI của user")
    public ResponseEntity<ApiResponse<PageResponse<AIConversation>>> getConversations(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "20") Integer limit
    ) {
        int pageNumber = Math.max(page - 1, 0);
        Page<AIConversation> conversations = aiService.getConversations(currentUser.getId(), pageNumber, limit);
        
        PageResponse<AIConversation> pageResponse = PageResponse.<AIConversation>builder()
                .content(conversations.getContent())
                .currentPage(conversations.getNumber())
                .pageSize(conversations.getSize())
                .totalElements(conversations.getTotalElements())
                .totalPages(conversations.getTotalPages())
                .isLast(conversations.isLast())
                .isFirst(conversations.isFirst())
                .hasNext(conversations.hasNext())
                .hasPrevious(conversations.hasPrevious())
                .isEmpty(conversations.isEmpty())
                .build();

        return ResponseEntity.ok(ApiResponse.<PageResponse<AIConversation>>builder()
                .code(200)
                .message("Lấy danh sách conversations thành công")
                .result(pageResponse)
                .build());
    }

    /**
     * Lấy tin nhắn của một conversation
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Lấy tin nhắn của conversation", 
               description = "Lấy lịch sử tin nhắn trong một cuộc hội thoại AI")
    public ResponseEntity<ApiResponse<PageResponse<AIMessage>>> getMessages(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của conversation") @PathVariable String conversationId,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "50") Integer limit
    ) {
        int pageNumber = Math.max(page - 1, 0);
        Page<AIMessage> messages = aiService.getMessages(conversationId, currentUser.getId(), pageNumber, limit);
        
        PageResponse<AIMessage> pageResponse = PageResponse.<AIMessage>builder()
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

        return ResponseEntity.ok(ApiResponse.<PageResponse<AIMessage>>builder()
                .code(200)
                .message("Lấy tin nhắn thành công")
                .result(pageResponse)
                .build());
    }

    /**
     * Xóa một conversation
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Xóa conversation", 
               description = "Xóa (soft delete) một cuộc hội thoại AI")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID của conversation") @PathVariable String conversationId
    ) {
        aiService.deleteConversation(conversationId, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Xóa conversation thành công")
                .build());
    }
    
    /**
     * Tạo conversation mới
     */
    @PostMapping("/conversations")
    @Operation(summary = "Tạo conversation mới", 
               description = "Tạo một cuộc hội thoại AI mới")
    public ResponseEntity<ApiResponse<AIChatResponse>> createConversation(
            @AuthenticationPrincipal User currentUser,
            @RequestBody AIChatRequest request
    ) {
        // Đặt conversationId = null để tạo mới
        request.setConversationId(null);
        AIChatResponse response = aiService.chat(request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.<AIChatResponse>builder()
                .code(200)
                .message("Tạo conversation thành công")
                .result(response)
                .build());
    }
}
