package com.example.cellex.services.coupon;

import com.example.cellex.dtos.request.coupon.CampaignRecipientFilter;
import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.models.coupon.CouponCampaign;
import com.example.cellex.repositories.coupon.CouponCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignSchedulerService {

    private final CouponCampaignRepository campaignRepository;
    private final CouponCampaignService campaignService;

    /**
     * Chạy mỗi 30 phút để kiểm tra campaigns cần phát theo lịch
     * Cron: 0 phút và 30 phút, mỗi giờ, mỗi ngày
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void processScheduledCampaigns() {
        log.info("========== BẮT ĐẦU KIỂM TRA CAMPAIGNS THEO LỊCH ==========");
        
        try {
            List<CouponCampaign> scheduledCampaigns = campaignRepository
                    .findByStatusAndScheduledAtBeforeAndIsActive(
                            CampaignStatus.SCHEDULED,
                            LocalDateTime.now(),
                            true
                    );

            log.info("Found {} scheduled campaigns to process", scheduledCampaigns.size());

            for (CouponCampaign campaign : scheduledCampaigns) {
                try {
                    log.info("Processing campaign: {} - {}", campaign.getId(), campaign.getTitle());
                    
                    // Tạo filter mặc định: phát cho tất cả users active
                    // Trong thực tế, có thể lưu filter criteria trong campaign
                    CampaignRecipientFilter filter = new CampaignRecipientFilter();
                    filter.setAll(true);

                    // Phát campaign
                    campaignService.distributeCampaign(
                            campaign.getId(),
                            filter,
                            campaign.getCreatedBy() // System auto-distribute
                    );

                    log.info("Successfully distributed scheduled campaign: {}", campaign.getId());

                } catch (Exception e) {
                    log.error("Error processing scheduled campaign {}: {}", 
                            campaign.getId(), e.getMessage(), e);
                    
                    // Mark campaign as failed
                    campaign.setStatus(CampaignStatus.CANCELLED);
                    campaign.setNote("Auto-distribution failed: " + e.getMessage());
                    campaignRepository.save(campaign);
                }
            }

            log.info("========== HOÀN TẤT KIỂM TRA CAMPAIGNS THEO LỊCH ==========");

        } catch (Exception e) {
            log.error("Error in scheduled campaigns processor: {}", e.getMessage(), e);
        }
    }
}

