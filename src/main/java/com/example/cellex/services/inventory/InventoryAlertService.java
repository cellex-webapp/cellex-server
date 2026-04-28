package com.example.cellex.services.inventory;

import com.example.cellex.enums.NotificationType;
import com.example.cellex.enums.Role;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.product.ProductSku;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAlertService {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public void pushLowStockAlerts() {
        List<ProductSku> lowStockSkus = inventoryService.findLowStockSkus();
        if (lowStockSkus.isEmpty()) {
            log.info("No low-stock SKU found");
            return;
        }

        List<User> admins = userRepository.findByRole(Role.ADMIN, Pageable.unpaged());

        for (ProductSku sku : lowStockSkus) {
            Product product = productRepository.findById(sku.getProductId()).orElse(null);
            Shop shop = shopRepository.findById(sku.getShopId()).orElse(null);

            String productName = product != null ? product.getName() : "San pham";
            String shopName = shop != null ? shop.getShopName() : "Cua hang";
            String title = "Canh bao ton kho thap";
            String message = String.format(
                    "%s - SKU %s dang sap het hang (available=%d, safety=%d)",
                    productName,
                    sku.getSkuCode(),
                    sku.getAvailableStock(),
                    sku.getSafetyStock()
            );
            String metadata = String.format(
                    "{\"skuId\":\"%s\",\"productId\":\"%s\",\"shopId\":\"%s\",\"available\":%d,\"safety\":%d}",
                    sku.getId(),
                    sku.getProductId(),
                    sku.getShopId(),
                    sku.getAvailableStock(),
                    sku.getSafetyStock()
            );

            for (User admin : admins) {
                notificationService.sendNotificationToUser(
                        admin,
                        title,
                        "[ADMIN] " + shopName + ": " + message,
                        NotificationType.PRODUCT_RESTOCK,
                        metadata,
                        "/admin/inventory/import",
                        null
                );
            }

            if (shop != null) {
                userRepository.findById(shop.getVendorId()).ifPresent(vendor ->
                        notificationService.sendNotificationToUser(
                                vendor,
                                title,
                                message,
                                NotificationType.PRODUCT_RESTOCK,
                                metadata,
                                "/vendor/products",
                                null
                        )
                );
            }
        }

        log.info("Pushed low-stock alerts for {} SKU", lowStockSkus.size());
    }
}
