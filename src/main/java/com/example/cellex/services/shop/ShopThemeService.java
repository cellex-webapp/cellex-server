package com.example.cellex.services.shop;

import com.example.cellex.dtos.request.shop.ShopThemeRequest;
import com.example.cellex.dtos.response.shop.ShopThemeResponse;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.models.shop.ShopTheme;
import com.example.cellex.repositories.shop.ShopThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for ShopTheme (SDUI configuration).
 * Handles business logic for theme management: create, update, retrieve, delete.
 * All methods accept UUID shopId (parsing is delegated to Spring's MethodArgumentConverter).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShopThemeService {

    private final ShopThemeRepository shopThemeRepository;

    /**
     * Get theme by shop ID.
     *
     * @param shopId the shop UUID
     * @return theme response DTO
     * @throws AppException if theme not found
     */
    @Transactional(readOnly = true)
    public ShopThemeResponse getThemeByShopId(UUID shopId) {
        ShopTheme theme = shopThemeRepository.findByShopId(shopId)
            .orElseThrow(() -> {
                log.warn("ShopTheme not found for shop: {}", shopId);
                return new AppException(ErrorCode.NOT_FOUND);
            });
        return mapToResponse(theme);
    }

    /**
     * Create a new theme for a shop.
     *
     * @param shopId the shop UUID
     * @param request the theme request DTO
     * @return created theme response DTO
     * @throws AppException if theme already exists
     */
    public ShopThemeResponse createTheme(UUID shopId, ShopThemeRequest request) {
        // Check if theme already exists
        if (shopThemeRepository.existsByShopId(shopId)) {
            log.warn("Theme already exists for shop: {}", shopId);
            throw new AppException(ErrorCode.SHOP_ALREADY_HAS_THEME);
        }

        // Build and save theme entity
        ShopTheme theme = ShopTheme.builder()
            .shopId(shopId)
            .primaryColor(request.getPrimaryColor())
            .secondaryColor(request.getSecondaryColor())
            .fontFamily(request.getFontFamily())
            .layoutConfig(request.getLayoutConfig())
            .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
            .build();

        ShopTheme saved = shopThemeRepository.save(theme);
        log.info("Created ShopTheme: {} for shop: {}", saved.getId(), shopId);

        return mapToResponse(saved);
    }

    /**
     * Update an existing theme for a shop.
     * Only non-null fields from the request are applied (partial update).
     *
     * @param shopId the shop UUID
     * @param request the updated theme request DTO
     * @return updated theme response DTO
     * @throws AppException if theme not found
     */
    public ShopThemeResponse updateTheme(UUID shopId, ShopThemeRequest request) {
        ShopTheme theme = shopThemeRepository.findByShopId(shopId)
            .orElseThrow(() -> {
                log.warn("ShopTheme not found for shop: {}", shopId);
                return new AppException(ErrorCode.NOT_FOUND);
            });

        // Apply non-null updates
        applyUpdates(theme, request);

        ShopTheme updated = shopThemeRepository.save(theme);
        log.info("Updated ShopTheme: {} for shop: {}", updated.getId(), shopId);

        return mapToResponse(updated);
    }

    /**
     * Delete theme by shop ID.
     *
     * @param shopId the shop UUID
     * @throws AppException if theme not found
     */
    public void deleteThemeByShopId(UUID shopId) {
        ShopTheme theme = shopThemeRepository.findByShopId(shopId)
            .orElseThrow(() -> {
                log.warn("ShopTheme not found for shop: {}", shopId);
                return new AppException(ErrorCode.NOT_FOUND);
            });

        shopThemeRepository.delete(theme);
        log.info("Deleted ShopTheme for shop: {}", shopId);
    }

    /**
     * Apply non-null fields from request DTO to theme entity.
     * This provides a clean, concise update mechanism without MapStruct.
     *
     * @param theme the theme entity to update (in-place mutation)
     * @param request the request DTO with new values
     */
    private void applyUpdates(ShopTheme theme, ShopThemeRequest request) {
        if (request.getPrimaryColor() != null) {
            theme.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            theme.setSecondaryColor(request.getSecondaryColor());
        }
        if (request.getFontFamily() != null) {
            theme.setFontFamily(request.getFontFamily());
        }
        if (request.getLayoutConfig() != null) {
            theme.setLayoutConfig(request.getLayoutConfig());
        }
        if (request.getIsPublished() != null) {
            theme.setIsPublished(request.getIsPublished());
        }
    }

    /**
     * Map ShopTheme entity to response DTO.
     *
     * @param theme the entity
     * @return response DTO
     */
    private ShopThemeResponse mapToResponse(ShopTheme theme) {
        return ShopThemeResponse.builder()
            .id(theme.getId().toString())
            .shopId(theme.getShopId().toString())
            .primaryColor(theme.getPrimaryColor())
            .secondaryColor(theme.getSecondaryColor())
            .fontFamily(theme.getFontFamily())
            .layoutConfig(theme.getLayoutConfig())
            .isPublished(theme.getIsPublished())
            .createdAt(theme.getCreatedAt())
            .updatedAt(theme.getUpdatedAt())
            .build();
    }
}
