package com.example.cellex.services;

import com.example.cellex.enums.ScheduleFrequency;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.CustomerSegment;
import com.example.cellex.models.SegmentCoupon;
import com.example.cellex.models.User;
import com.example.cellex.repositories.SegmentCouponRepository;
import com.example.cellex.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        // Kiểm tra và cập nhật segment
        CustomerSegment oldSegment = null;
        if (user.getCustomerSegmentId() != null) {
            try {
                oldSegment = customerSegmentService.findSegmentForSpend(oldTotalSpend);
            } catch (Exception e) {
                log.warn("Không tìm thấy segment cũ cho user {}", userId);
            }
        }

        CustomerSegment newSegment = customerSegmentService.findSegmentForSpend(newTotalSpend);

        // Nếu segment thay đổi
        if (newSegment != null && !newSegment.getId().equals(user.getCustomerSegmentId())) {
            String oldSegmentId = user.getCustomerSegmentId();
            user.setCustomerSegmentId(newSegment.getId());

            // Lưu vào lịch sử
            if (user.getSegmentHistory() == null) {
                user.setSegmentHistory(new ArrayList<>());
            }

            // Đóng segment cũ
            if (oldSegmentId != null && oldSegment != null) {
                user.getSegmentHistory().stream()
                        .filter(h -> h.getSegmentId().equals(oldSegmentId) && h.getTo() == null)
                        .forEach(h -> h.setTo(LocalDateTime.now()));
            }

            // Thêm segment mới
            User.SegmentHistory newHistory = User.SegmentHistory.builder()
                    .segmentId(newSegment.getId())
                    .segmentName(newSegment.getName())
                    .from(LocalDateTime.now())
                    .to(null)
                    .note(oldSegmentId != null ? 
                            (newSegment.getLevel() > oldSegment.getLevel() ? "Upgraded" : "Downgraded") 
                            : "Initial")
                    .build();
            user.getSegmentHistory().add(newHistory);

            userRepository.save(user);

            log.info("User {} đã được nâng hạng lên segment {} ({})", userId, newSegment.getId(), newSegment.getName());

            // Phát coupon tự động nếu có
            if (newSegment.getLevel() != null && (oldSegment == null || newSegment.getLevel() > oldSegment.getLevel())) {
                issueUpgradeCoupons(user, newSegment);
            }
        } else {
            userRepository.save(user);
        }
    }

    private void issueUpgradeCoupons(User user, CustomerSegment segment) {
        List<SegmentCoupon> upgradeCoupons = segmentCouponRepository
                .findBySegmentIdAndIsAutoOnUpgradeAndIsActive(segment.getId(), true, true);

        for (SegmentCoupon coupon : upgradeCoupons) {
            try {
                userCouponService.issueCouponToUser(user.getId(), coupon.getId(), "UPGRADE");
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
                List<User> eligibleUsers = userRepository.findByCustomerSegmentId(coupon.getSegmentId());

                int issuedCount = 0;
                for (User user : eligibleUsers) {
                    try {
                        userCouponService.issueCouponToUser(user.getId(), coupon.getId(), "SCHEDULED");
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

