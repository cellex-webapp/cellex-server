package com.example.cellex.services.livestream;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivestreamEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Data
    @Builder
    public static class LiveEventPayload {
        private String type; // VD: PIN_PRODUCT, UNPIN_PRODUCT, NEW_ORDER
        private String productId;
        private String message;
    }

    public void broadcastPinProduct(String sessionId, String productId) {
        LiveEventPayload payload = LiveEventPayload.builder()
                .type("PIN_PRODUCT")
                .productId(productId)
                .build();
        messagingTemplate.convertAndSend("/topic/live/" + sessionId + "/events", payload);
    }

    public void broadcastUnpinProduct(String sessionId) {
        LiveEventPayload payload = LiveEventPayload.builder()
                .type("UNPIN_PRODUCT")
                .build();
        messagingTemplate.convertAndSend("/topic/live/" + sessionId + "/events", payload);
    }

    public void broadcastNewOrder(String sessionId, String message) {
        LiveEventPayload payload = LiveEventPayload.builder()
                .type("NEW_ORDER")
                .message(message)
                .build();
        messagingTemplate.convertAndSend("/topic/live/" + sessionId + "/events", payload);
    }
}