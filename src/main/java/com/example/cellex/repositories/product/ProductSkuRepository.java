package com.example.cellex.repositories.product;

import com.example.cellex.models.product.ProductSku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

    default Optional<ProductSku> findById(String id) {
        try {
            return findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    List<ProductSku> findByProductIdAndIsActiveTrueOrderByCreatedAtAsc(String productId);

    List<ProductSku> findByProductIdInAndIsActiveTrue(List<String> productIds);

    @Query("SELECT ps FROM ProductSku ps WHERE ps.shopUuid = :shopUuid AND ps.isActive = true")
    List<ProductSku> findByShopUuidAndIsActiveTrue(@Param("shopUuid") UUID shopUuid);

    default List<ProductSku> findByShopIdAndIsActiveTrue(String shopId) {
        try {
            return findByShopUuidAndIsActiveTrue(UUID.fromString(shopId));
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    @Query("SELECT ps FROM ProductSku ps WHERE ps.uuid = :skuUuid AND ps.isActive = true")
    Optional<ProductSku> findActiveByUuid(@Param("skuUuid") UUID skuUuid);

    default Optional<ProductSku> findActiveById(String skuId) {
        try {
            return findActiveByUuid(UUID.fromString(skuId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductSku ps WHERE ps.uuid = :skuUuid AND ps.isActive = true")
    Optional<ProductSku> findActiveByUuidForUpdate(@Param("skuUuid") UUID skuUuid);

    default Optional<ProductSku> findActiveByIdForUpdate(String skuId) {
        try {
            return findActiveByUuidForUpdate(UUID.fromString(skuId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }



    Optional<ProductSku> findBySkuCodeIgnoreCaseAndIsActiveTrue(String skuCode);

    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END FROM ProductSku ps WHERE LOWER(ps.skuCode) = LOWER(:skuCode)")
    boolean existsBySkuCodeIgnoreCase(@Param("skuCode") String skuCode);

    void deleteByProductId(String productId);

    @Query("SELECT ps FROM ProductSku ps WHERE ps.isActive = true " +
            "AND (:shopUuid IS NULL OR ps.shopUuid = :shopUuid) " +
            "AND LOWER(ps.skuCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY ps.updatedAt DESC")
    List<ProductSku> searchBySkuCode(
            @Param("keyword") String keyword,
            @Param("shopUuid") UUID shopUuid,
            Pageable pageable
    );

    default List<ProductSku> searchBySkuCode(String keyword, String shopId, Pageable pageable) {
        try {
            UUID shopUuid = (shopId == null || shopId.isBlank()) ? null : UUID.fromString(shopId);
            return searchBySkuCode(keyword, shopUuid, pageable);
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    @Query("SELECT ps FROM ProductSku ps WHERE ps.isActive = true " +
            "AND (COALESCE(ps.onHandStock, 0) - COALESCE(ps.reservedStock, 0)) <= COALESCE(ps.safetyStock, 0)")
    List<ProductSku> findLowStockSkus();
}
