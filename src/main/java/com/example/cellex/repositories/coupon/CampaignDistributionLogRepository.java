package com.example.cellex.repositories.coupon;

import com.example.cellex.models.coupon.CampaignDistributionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignDistributionLogRepository extends JpaRepository<CampaignDistributionLog, UUID> {

    List<CampaignDistributionLog> findByCampaignIdOrderByCreatedAtDesc(String campaignId);
    org.springframework.data.domain.Page<CampaignDistributionLog> findByCampaignIdOrderByCreatedAtDesc(String campaignId, org.springframework.data.domain.Pageable pageable);

    List<CampaignDistributionLog> findByAdminIdOrderByCreatedAtDesc(String adminId);
    org.springframework.data.domain.Page<CampaignDistributionLog> findByAdminIdOrderByCreatedAtDesc(String adminId, org.springframework.data.domain.Pageable pageable);

    // ==================== Backward-compatible methods ====================

    default Optional<CampaignDistributionLog> findById(String id) {
        return findById(UUID.fromString(id));
    }
}

