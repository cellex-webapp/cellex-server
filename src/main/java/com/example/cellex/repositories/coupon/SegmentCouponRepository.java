package com.example.cellex.repositories.coupon;

import com.example.cellex.enums.ScheduleFrequency;
import com.example.cellex.models.coupon.SegmentCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SegmentCouponRepository extends JpaRepository<SegmentCoupon, UUID> {

    List<SegmentCoupon> findBySegmentIdAndIsActive(String segmentId, Boolean isActive);
    org.springframework.data.domain.Page<SegmentCoupon> findBySegmentIdAndIsActive(String segmentId, Boolean isActive, org.springframework.data.domain.Pageable pageable);

    List<SegmentCoupon> findBySegmentIdAndIsAutoOnUpgradeAndIsActive(String segmentId, Boolean isAutoOnUpgrade, Boolean isActive);

    List<SegmentCoupon> findByScheduleFrequencyNotAndNextScheduledDateBeforeAndIsActive(
            ScheduleFrequency scheduleFrequency,
            LocalDateTime nextScheduledDate,
            Boolean isActive
    );
    org.springframework.data.domain.Page<SegmentCoupon> findByScheduleFrequencyNotAndNextScheduledDateBeforeAndIsActive(
            ScheduleFrequency scheduleFrequency,
            LocalDateTime nextScheduledDate,
            Boolean isActive,
            org.springframework.data.domain.Pageable pageable
    );

    // ==================== Backward-compatible methods ====================

    default Optional<SegmentCoupon> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default boolean existsById(String id) {
        return existsById(UUID.fromString(id));
    }

    default void deleteById(String id) {
        deleteById(UUID.fromString(id));
    }
}

