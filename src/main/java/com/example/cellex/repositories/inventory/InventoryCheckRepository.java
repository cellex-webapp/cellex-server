package com.example.cellex.repositories.inventory;

import com.example.cellex.enums.InventoryCheckStatus;
import com.example.cellex.models.inventory.InventoryCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, UUID> {

    @Query("SELECT c FROM InventoryCheck c WHERE c.shopUuid = :shopUuid ORDER BY c.createdAt DESC")
    Page<InventoryCheck> findByShopUuidOrderByCreatedAtDesc(@Param("shopUuid") UUID shopUuid, Pageable pageable);

    @Query("SELECT c FROM InventoryCheck c WHERE c.shopUuid = :shopUuid AND c.status = :status ORDER BY c.createdAt DESC")
    Page<InventoryCheck> findByShopUuidAndStatus(
            @Param("shopUuid") UUID shopUuid,
            @Param("status") InventoryCheckStatus status,
            Pageable pageable
    );

        List<InventoryCheck> findAllByOrderByCreatedAtDesc();

    default Optional<InventoryCheck> findById(String checkId) {
        try {
            return findById(UUID.fromString(checkId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
