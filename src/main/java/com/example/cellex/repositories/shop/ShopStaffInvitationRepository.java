package com.example.cellex.repositories.shop;

import com.example.cellex.enums.StaffInvitationStatus;
import com.example.cellex.models.shop.ShopStaffInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopStaffInvitationRepository extends JpaRepository<ShopStaffInvitation, UUID> {
    boolean existsByShopUuidAndInvitedUserUuidAndStatus(UUID shopUuid, UUID invitedUserUuid, StaffInvitationStatus status);
    List<ShopStaffInvitation> findByShopUuidOrderByCreatedAtDesc(UUID shopUuid);
    List<ShopStaffInvitation> findByInvitedUserUuidAndStatusOrderByCreatedAtDesc(UUID invitedUserUuid, StaffInvitationStatus status);
    Optional<ShopStaffInvitation> findByUuid(UUID uuid);
    List<ShopStaffInvitation> findByStatusAndExpiresAtBefore(StaffInvitationStatus status, LocalDateTime time);
}

