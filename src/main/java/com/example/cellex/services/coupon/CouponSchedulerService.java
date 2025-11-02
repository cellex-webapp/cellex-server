package com.example.cellex.services.coupon;

import com.example.cellex.services.segment.CustomerSegmentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponSchedulerService {

    private final CustomerSegmentationService customerSegmentationService;
    private final UserCouponService userCouponService;

    /**
     * Chạy mỗi giờ để kiểm tra và phát phiếu giảm giá theo lịch
     * Cron: 0 phút, mỗi giờ, mỗi ngày
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleIssueCoupons() {
        log.info("========== BẮT ĐẦU KIỂM TRA VÀ PHÁT PHIẾU GIẢM GIÁ THEO LỊCH ==========");
        try {
            customerSegmentationService.issueScheduledCoupons();
            log.info("========== HOÀN TẤT PHÁT PHIẾU GIẢM GIÁ THEO LỊCH ==========");
        } catch (Exception e) {
            log.error("Lỗi khi phát phiếu giảm giá theo lịch: {}", e.getMessage(), e);
        }
    }

    /**
     * Chạy mỗi ngày lúc 00:00 để cập nhật status các phiếu giảm giá đã hết hạn
     * Cron: 0 phút, 0 giờ, mỗi ngày
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleUpdateExpiredCoupons() {
        log.info("========== BẮT ĐẦU CẬP NHẬT PHIẾU GIẢM GIÁ HẾT HẠN ==========");
        try {
            userCouponService.updateExpiredCoupons();
            log.info("========== HOÀN TẤT CẬP NHẬT PHIẾU GIẢM GIÁ HẾT HẠN ==========");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật phiếu giảm giá hết hạn: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra và cập nhật phiếu giảm giá hết hạn mỗi 6 giờ (bổ sung)
     * Cron: 0 phút, mỗi 6 giờ (0, 6, 12, 18), mỗi ngày
     */
    @Scheduled(cron = "0 0 0,6,12,18 * * *")
    public void scheduleUpdateExpiredCouponsFrequent() {
        log.info("========== KIỂM TRA PHIẾU GIẢM GIÁ HẾT HẠN (6H) ==========");
        try {
            userCouponService.updateExpiredCoupons();
            log.info("========== HOÀN TẤT KIỂM TRA PHIẾU GIẢM GIÁ HẾT HẠN ==========");
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra phiếu giảm giá hết hạn: {}", e.getMessage(), e);
        }
    }
}
