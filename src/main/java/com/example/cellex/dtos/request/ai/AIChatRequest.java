package com.example.cellex.dtos.request.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho AI Chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatRequest {
    
    /**
     * Nội dung tin nhắn từ user
     */
    private String message;
    
    /**
     * ID của conversation (để duy trì context)
     */
    private String conversationId;
    
    /**
     * Shop ID (dành cho VENDOR role)
     */
    private String shopId;
}
