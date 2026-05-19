package com.example.cellex.repositories.shop;

import com.example.cellex.models.shop.ShopStaffMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopStaffMemberRepository extends JpaRepository<ShopStaffMember, UUID> {
    Optional<ShopStaffMember> findByUserUuidAndIsActiveTrue(UUID userUuid);
    Optional<ShopStaffMember> findByShopUuidAndUserUuidAndIsActiveTrue(UUID shopUuid, UUID userUuid);
    List<ShopStaffMember> findByShopUuidAndIsActiveTrueOrderByCreatedAtDesc(UUID shopUuid);
    long countByShopRoleUuidAndIsActiveTrue(UUID shopRoleUuid);
    long countByUserUuidAndIsActiveTrue(UUID userUuid);
}

