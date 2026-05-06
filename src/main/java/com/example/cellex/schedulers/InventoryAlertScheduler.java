package com.example.cellex.schedulers;

import com.example.cellex.services.inventory.InventoryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryAlertScheduler {

    private final InventoryAlertService inventoryAlertService;

    @Scheduled(cron = "0 0 2 * * *")
    public void scanLowStockAndNotify() {
        log.info("=== Starting low stock scan job ===");
        try {
            inventoryAlertService.pushLowStockAlerts();
            log.info("=== Low stock scan job completed ===");
        } catch (Exception e) {
            log.error("Error during low stock scan job", e);
        }
    }
}
