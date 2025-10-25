package com.example.cellex.repositories;

import com.example.cellex.models.CampaignDistributionLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignDistributionLogRepository extends MongoRepository<CampaignDistributionLog, String> {
    
    List<CampaignDistributionLog> findByCampaignIdOrderByCreatedAtDesc(String campaignId);
    
    List<CampaignDistributionLog> findByAdminIdOrderByCreatedAtDesc(String adminId);
}

