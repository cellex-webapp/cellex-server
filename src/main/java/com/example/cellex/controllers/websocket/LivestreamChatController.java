package com.example.cellex.controllers.websocket;

import com.example.cellex.dtos.request.livestream.ChatMessagePayload;
import com.example.cellex.models.mongo.LivestreamCommentDocument;
import com.example.cellex.repositories.product.LivestreamCommentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LivestreamChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LivestreamCommentRepository commentRepository;

    // Client sẽ gửi message tới: /app/live/{sessionId}/chat
    @MessageMapping("/live/{sessionId}/chat")
    public void processChatMessage(@DestinationVariable String sessionId, @Payload ChatMessagePayload payload) {
        log.info("Received chat message in session {}: {}", sessionId, payload.getContent());

        // 1. Lưu vào MongoDB để log
        LivestreamCommentDocument savedComment = LivestreamCommentDocument.builder()
                .sessionId(sessionId)
                .userName(payload.getUserName())
                .content(payload.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        commentRepository.save(savedComment);

        // 2. Broadcast lại cho toàn bộ Viewer đang subscribe topic này
        // (Viewer subscribe: /topic/live/{sessionId}/chat)
        messagingTemplate.convertAndSend("/topic/live/" + sessionId + "/chat", savedComment);
    }
}