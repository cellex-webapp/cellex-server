package com.example.cellex.repositories.coupon;

import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.models.coupon.CouponCampaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CouponCampaignRepository extends MongoRepository<CouponCampaign, String> {
    
    List<CouponCampaign> findByStatus(CampaignStatus status);
    org.springframework.data.domain.Page<CouponCampaign> findByStatus(CampaignStatus status, org.springframework.data.domain.Pageable pageable);
    
    List<CouponCampaign> findByCreatedBy(String createdBy);
    
    // Tìm campaigns cần phát theo lịch
    List<CouponCampaign> findByStatusAndScheduledAtBeforeAndIsActive(
            CampaignStatus status, 
            LocalDateTime scheduledAt, 
            Boolean isActive
    );
    
    List<CouponCampaign> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);
    org.springframework.data.domain.Page<CouponCampaign> findByIsActiveOrderByCreatedAtDesc(Boolean isActive, org.springframework.data.domain.Pageable pageable);
}

