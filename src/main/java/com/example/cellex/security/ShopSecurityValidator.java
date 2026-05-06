package com.example.cellex.security;

import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Security validator for Shop ownership.
 * Prevents BOLA (Broken Object Level Authorization) attacks by verifying
 * that the authenticated user owns the shop they're trying to access.
 *
 * Usage in Controller:
 * @PreAuthorize("hasRole('VENDOR') and @shopSecurityValidator.isShopOwner(authentication, #shopId)")
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopSecurityValidator {

    private final ShopRepository shopRepository;

    /**
     * Checks if the authenticated user owns the given shop.
     *
     * @param authentication the Spring Security Authentication object
     * @param shopId the UUID of the shop to check ownership for
     * @return true if the authenticated user owns the shop, false otherwise
     */
    public boolean isShopOwner(Authentication authentication, UUID shopId) {
        // Validate authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access attempt: authentication is null or not authenticated");
            return false;
        }

        // Extract current user from principal
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.warn("Unauthorized access attempt: principal is not a User instance");
            return false;
        }

        User currentUser = (User) principal;
        String userId = currentUser.getId();

        if (userId == null || userId.isBlank()) {
            log.warn("Unauthorized access attempt: user ID is null or blank");
            return false;
        }

        try {
            // Parse user ID to UUID
            UUID userUuid = UUID.fromString(userId);

            // Fetch shop from database
            Shop shop = shopRepository.findById(shopId).orElse(null);

            if (shop == null) {
                log.warn("Shop not found: {}", shopId);
                return false;
            }

            // Check ownership
            boolean isOwner = shop.getOwnerUuid().equals(userUuid);

            if (!isOwner) {
                log.warn("Ownership check failed: User {} attempted to access Shop {} owned by {}",
                    userUuid, shopId, shop.getOwnerUuid());
            }

            return isOwner;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for user ID: {}", userId, e);
            return false;
        }
    }
}
