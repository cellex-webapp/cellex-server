package com.example.cellex.repositories.coupon;

import com.example.cellex.enums.ScheduleFrequency;
import com.example.cellex.models.coupon.SegmentCoupon;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SegmentCouponRepository extends MongoRepository<SegmentCoupon, String> {
    
    List<SegmentCoupon> findBySegmentIdAndIsActive(String segmentId, Boolean isActive);
    
    // Tìm các coupon cần tự động phát khi nâng hạng
    List<SegmentCoupon> findBySegmentIdAndIsAutoOnUpgradeAndIsActive(String segmentId, Boolean isAutoOnUpgrade, Boolean isActive);
    
    // Tìm các coupon cần phát theo lịch
    List<SegmentCoupon> findByScheduleFrequencyNotAndNextScheduledDateBeforeAndIsActive(
            ScheduleFrequency scheduleFrequency, 
            LocalDateTime nextScheduledDate, 
            Boolean isActive
    );
}

