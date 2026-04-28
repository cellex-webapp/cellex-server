package com.example.cellex.services.migration;

import com.example.cellex.services.order.OrderService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Migration service to generate orderCode for existing orders that don't have one.
 * This runs automatically on application startup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCodeMigrationService {

    private final OrderService orderService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            log.info("Starting order code migration on application startup...");
            orderService.migrateOrderCodes();
            log.info("Order code migration completed successfully.");
        } catch (Exception e) {
            log.error("Failed to run order code migration: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
        }
    }
}
