package com.example.cellex.controllers;

import com.example.cellex.dtos.request.shipping.GhnWebhookPayload;
import com.example.cellex.services.shipping.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class GhnWebhookController {

    private final ShipmentService shipmentService;

    @PostMapping("/ghn")
    public ResponseEntity<Void> handleGhnWebhook(@RequestBody GhnWebhookPayload payload) {
        log.info("GHN Webhook received: order={}, status={}", 
                 payload.getClientOrderCode(), payload.getStatus());
        try {
            shipmentService.processWebhook(payload);
        } catch (Exception e) {
            log.error("Error processing GHN webhook", e);
            // Vẫn trả về 200 OK để GHN không retry
        }
        return ResponseEntity.ok().build(); 
    }
}
