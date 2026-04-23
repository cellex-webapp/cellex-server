package com.example.cellex.services.segment;

import com.example.cellex.enums.IssuedVia;
import com.example.cellex.enums.ScheduleFrequency;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.segment.CustomerSegment;
import com.example.cellex.models.coupon.SegmentCoupon;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.coupon.SegmentCouponRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.coupon.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSegmentationService {

    private final UserRepository userRepository;
    private final CustomerSegmentService customerSegmentService;
    private final SegmentCouponRepository segmentCouponRepository;
    private final UserCouponService userCouponService;
    private final SegmentCouponService segmentCouponService;

    @Transactional
    public void updateUserSpend(String userId, Double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Double oldTotalSpend = user.getTotalSpend() != null ? user.getTotalSpend() : 0.0;
        Double newTotalSpend = oldTotalSpend + amount;

        user.setTotalSpend(newTotalSpend);

        // Lấy segment cũ từ user (nếu có)
        CustomerSegment oldSegment = null;
        String oldSegmentId = user.getCustomerSegmentId();
        if (oldSegmentId != null) {
            try {
                oldSegment = customerSegmentService.getSegmentEntityById(oldSegmentId);
            } catch (Exception e) {
                log.warn("Không tìm thấy segment cũ {} cho user {}", oldSegmentId, userId);
            }
        }

        // Tìm segment mới dựa trên tổng chi tiêu mới
        CustomerSegment newSegment = customerSegmentService.findSegmentForSpend(newTotalSpend);

        // Kiểm tra xem segment có thay đổi không
        boolean segmentChanged = false;
        if (newSegment != null) {
            // Nếu user chưa có segment hoặc segment ID khác nhau
            if (oldSegmentId == null || !newSegment.getId().equals(oldSegmentId)) {
                segmentChanged = true;
            }
        }

        if (segmentChanged) {
            // CẬP NHẬT SEGMENT MỚI VÀO USER
            user.setCustomerSegmentId(newSegment.getId());

            // Note: Segment history is now managed in the user_segment_history table (PostgreSQL).
            // TODO: Implement UserSegmentHistoryRepository when segment module is migrated.
            String note = "Initial";
            if (oldSegment != null && newSegment.getLevel() != null && oldSegment.getLevel() != null) {
                note = newSegment.getLevel() > oldSegment.getLevel() ? "Upgraded" : "Downgraded";
            } else if (oldSegmentId != null) {
                note = "Changed";
            }
            log.info("User {} segment history: {} → {} ({})", userId,
                    oldSegmentId != null ? oldSegmentId : "null",
                    newSegment.getId(), note);

            // Lưu user với segment mới
            userRepository.save(user);

            log.info("User {} đã thay đổi segment từ {} sang {} ({})",
                    userId,
                    oldSegmentId != null ? oldSegmentId : "null",
                    newSegment.getId(),
                    newSegment.getName());

            // Phát coupon tự động nếu là nâng hạng
            boolean isUpgrade = oldSegment == null ||
                (oldSegment != null && newSegment.getLevel() != null && oldSegment.getLevel() != null &&
                 newSegment.getLevel() > oldSegment.getLevel());

            if (isUpgrade) {
                log.info("Đang phát coupon nâng hạng cho user {}", userId);
                issueUpgradeCoupons(user, newSegment);
            }
        } else {
            // Không có thay đổi segment, chỉ cập nhật totalSpend
            userRepository.save(user);
            log.info("User {} cập nhật totalSpend thành {} (segment không đổi)", userId, newTotalSpend);
        }
    }

    private void issueUpgradeCoupons(User user, CustomerSegment segment) {
        List<SegmentCoupon> upgradeCoupons = segmentCouponRepository
                .findBySegmentIdAndIsAutoOnUpgradeAndIsActive(segment.getId(), true, true);

        for (SegmentCoupon coupon : upgradeCoupons) {
            try {
                userCouponService.issueCouponToUser(user.getId(), coupon.getId(), IssuedVia.AUTO_ON_UPGRADE, null);
            } catch (Exception e) {
                log.error("Lỗi khi phát coupon {} cho user {} khi nâng hạng: {}", 
                        coupon.getId(), user.getId(), e.getMessage());
            }
        }

        if (!upgradeCoupons.isEmpty()) {
            log.info("Đã phát {} coupon cho user {} khi nâng hạng lên {}", 
                    upgradeCoupons.size(), user.getId(), segment.getName());
        }
    }

    public void issueScheduledCoupons() {
        LocalDateTime now = LocalDateTime.now();
        
        List<SegmentCoupon> dueCoupons = segmentCouponRepository
                .findByScheduleFrequencyNotAndNextScheduledDateBeforeAndIsActive(
                        ScheduleFrequency.NONE, 
                        now, 
                        true
                );

        log.info("Tìm thấy {} coupon cần phát theo lịch", dueCoupons.size());

        for (SegmentCoupon coupon : dueCoupons) {
            try {
                // Tìm tất cả users thuộc segment này
                List<User> eligibleUsers = userRepository.findByCustomerSegmentId(UUID.fromString(coupon.getSegmentId()));

                int issuedCount = 0;
                for (User user : eligibleUsers) {
                    try {
                        userCouponService.issueCouponToUser(user.getId(), coupon.getId(), IssuedVia.SCHEDULED, null);
                        issuedCount++;
                    } catch (Exception e) {
                        log.error("Lỗi khi phát coupon {} cho user {}: {}", 
                                coupon.getId(), user.getId(), e.getMessage());
                    }
                }

                // Cập nhật nextScheduledDate
                LocalDateTime nextScheduled = segmentCouponService.calculateNextScheduledDate(coupon);
                coupon.setNextScheduledDate(nextScheduled);
                segmentCouponRepository.save(coupon);

                log.info("Đã phát coupon {} cho {} users (segment: {})", 
                        coupon.getId(), issuedCount, coupon.getSegmentId());

            } catch (Exception e) {
                log.error("Lỗi khi xử lý scheduled coupon {}: {}", coupon.getId(), e.getMessage());
            }
        }
    }
}
