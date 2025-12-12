package com.example.cellex.schedulers;

import com.example.cellex.services.recommendation.CollaborativeFilteringService;
import com.example.cellex.services.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler để tính toán offline Collaborative Filtering và Recommendations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationScheduler {

    private final CollaborativeFilteringService cfService;
    private final RecommendationService recommendationService;

    /**
     * Tính toán item similarities mỗi ngày lúc 2:00 AM
     * Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void computeItemSimilarities() {
        log.info("=== Starting scheduled item similarity computation ===");
        
        try {
            long startTime = System.currentTimeMillis();
            
            cfService.computeItemSimilarities();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Item similarity computation completed in {} ms ===", duration);
            
        } catch (Exception e) {
            log.error("Error during scheduled item similarity computation", e);
        }
    }

    /**
     * Tính toán recommendations cho tất cả users mỗi ngày lúc 3:00 AM
     * Chạy sau khi tính similarities xong
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void computeRecommendations() {
        log.info("=== Starting scheduled recommendation computation ===");
        
        try {
            long startTime = System.currentTimeMillis();
            
            recommendationService.computeRecommendationsForAllUsers();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Recommendation computation completed in {} ms ===", duration);
            
        } catch (Exception e) {
            log.error("Error during scheduled recommendation computation", e);
        }
    }

    /**
     * Làm mới recommendations mỗi 6 giờ (tùy chọn - nếu muốn update thường xuyên hơn)
     * Uncomment để sử dụng
     */
    // @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    // public void refreshRecommendations() {
    //     log.info("=== Starting recommendation refresh ===");
    //     
    //     try {
    //         recommendationService.computeRecommendationsForAllUsers();
    //         log.info("=== Recommendation refresh completed ===");
    //     } catch (Exception e) {
    //         log.error("Error during recommendation refresh", e);
    //     }
    // }
}
