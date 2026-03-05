package com.example.cellex.models.coupon;

import com.example.cellex.enums.DiscountType;
import com.example.cellex.enums.ScheduleFrequency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "segment_coupons")
public class SegmentCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "segment_id", length = 50)
    private String segmentId;

    @Column(name = "code_prefix", length = 50)
    private String codePrefix;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 50)
    private DiscountType discountType;

    @Column(name = "discount_value")
    private Double discountValue;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "valid_hours")
    private Integer validHours;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ============ Cấu hình phát coupon tự động ============

    @Column(name = "is_auto_on_upgrade")
    @Builder.Default
    private Boolean isAutoOnUpgrade = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_frequency", length = 50)
    @Builder.Default
    private ScheduleFrequency scheduleFrequency = ScheduleFrequency.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_day_of_week", length = 20)
    private DayOfWeek scheduleDayOfWeek;

    @Column(name = "schedule_day_of_month")
    private Integer scheduleDayOfMonth;

    @Column(name = "schedule_month_day", length = 10)
    private String scheduleMonthDay;

    @Column(name = "schedule_time")
    @Builder.Default
    private LocalTime scheduleTime = LocalTime.of(0, 0);

    @Column(name = "next_scheduled_date")
    private LocalDateTime nextScheduledDate;

    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Backward-compatible ID accessors ====================

    @JsonProperty("id")
    public String getId() {
        return uuid != null ? uuid.toString() : null;
    }

    @JsonIgnore
    public void setId(String id) {
        this.uuid = id != null ? UUID.fromString(id) : null;
    }
}

