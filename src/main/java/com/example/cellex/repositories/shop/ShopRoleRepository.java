package com.example.cellex.repositories.shop;

import com.example.cellex.models.shop.ShopRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopRoleRepository extends JpaRepository<ShopRole, UUID> {
    List<ShopRole> findByShopUuidAndIsActiveTrue(UUID shopUuid);
    Optional<ShopRole> findByUuidAndShopUuidAndIsActiveTrue(UUID uuid, UUID shopUuid);
    boolean existsByShopUuidAndNameIgnoreCaseAndIsActiveTrue(UUID shopUuid, String name);
}

