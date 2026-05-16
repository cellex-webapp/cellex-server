package com.example.cellex.repositories.shop;

import com.example.cellex.models.shop.ShopTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for ShopTheme entity (PostgreSQL/Supabase).
 * Handles database operations for SDUI configuration.
 */
@Repository
public interface ShopThemeRepository extends JpaRepository<ShopTheme, UUID> {

    /**
     * Find ShopTheme by shop ID.
     * Since shop_id is UNIQUE, there can be at most one theme per shop.
     *
     * @param shopId the shop UUID
     * @return Optional containing ShopTheme if exists, empty otherwise
     */
    Optional<ShopTheme> findByShopId(UUID shopId);

    /**
     * Check if a theme exists for a given shop.
     *
     * @param shopId the shop UUID
     * @return true if theme exists, false otherwise
     */
    boolean existsByShopId(UUID shopId);
}
