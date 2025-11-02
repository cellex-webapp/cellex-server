package com.example.cellex.dtos.response.coupon;

import com.example.cellex.enums.DiscountType;
import com.example.cellex.enums.ScheduleFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentCouponResponse {
    private String id;
    private String segmentId;
    private String codePrefix;
    private String title;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Integer validHours;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Boolean isAutoOnUpgrade;
    private ScheduleFrequency scheduleFrequency;
    private DayOfWeek scheduleDayOfWeek;
    private Integer scheduleDayOfMonth;
    private String scheduleMonthDay;
    private LocalTime scheduleTime;
    private LocalDateTime nextScheduledDate;
    private Integer maxUsesPerUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

