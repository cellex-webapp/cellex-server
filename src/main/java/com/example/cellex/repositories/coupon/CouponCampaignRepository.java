package com.example.cellex.repositories.coupon;

import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.models.coupon.CouponCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponCampaignRepository extends JpaRepository<CouponCampaign, UUID> {

    List<CouponCampaign> findByStatus(CampaignStatus status);
    org.springframework.data.domain.Page<CouponCampaign> findByStatus(CampaignStatus status, org.springframework.data.domain.Pageable pageable);

    List<CouponCampaign> findByCreatedBy(String createdBy);

    List<CouponCampaign> findByStatusAndScheduledAtBeforeAndIsActive(
            CampaignStatus status,
            LocalDateTime scheduledAt,
            Boolean isActive
    );

    List<CouponCampaign> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);
    org.springframework.data.domain.Page<CouponCampaign> findByIsActiveOrderByCreatedAtDesc(Boolean isActive, org.springframework.data.domain.Pageable pageable);

    // ==================== Backward-compatible methods ====================

    default Optional<CouponCampaign> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default boolean existsById(String id) {
        return existsById(UUID.fromString(id));
    }

    default void deleteById(String id) {
        deleteById(UUID.fromString(id));
    }
}

