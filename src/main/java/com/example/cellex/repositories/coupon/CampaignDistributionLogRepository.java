package com.example.cellex.repositories.coupon;

import com.example.cellex.models.coupon.CampaignDistributionLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignDistributionLogRepository extends MongoRepository<CampaignDistributionLog, String> {
    
    List<CampaignDistributionLog> findByCampaignIdOrderByCreatedAtDesc(String campaignId);
    org.springframework.data.domain.Page<CampaignDistributionLog> findByCampaignIdOrderByCreatedAtDesc(String campaignId, org.springframework.data.domain.Pageable pageable);

    List<CampaignDistributionLog> findByAdminIdOrderByCreatedAtDesc(String adminId);
    org.springframework.data.domain.Page<CampaignDistributionLog> findByAdminIdOrderByCreatedAtDesc(String adminId, org.springframework.data.domain.Pageable pageable);
}

