package com.example.cellex.models.coupon;

import com.example.cellex.enums.DiscountType;
import com.example.cellex.enums.ScheduleFrequency;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "segment_coupons")
public class SegmentCoupon {

    @Id
    private String id;

    @Field("segment_id")
    private String segmentId; // ID của customer segment

    @Field("code_prefix")
    private String codePrefix; // Tiền tố cho mã coupon (VD: "VIP", "GOLD"), sẽ kết hợp với UUID

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("discount_type")
    private DiscountType discountType;

    @Field("discount_value")
    private Double discountValue; // Giá trị giảm (% hoặc số tiền cố định)

    @Field("min_order_amount")
    private Double minOrderAmount; // Giá trị đơn hàng tối thiểu để áp dụng

    @Field("valid_hours")
    private Integer validHours; // Số giờ có hiệu lực từ khi phát, null = dùng startDate/endDate

    @Field("start_date")
    private LocalDate startDate; // Ngày bắt đầu có hiệu lực (nếu không dùng validHours)

    @Field("end_date")
    private LocalDate endDate; // Ngày kết thúc có hiệu lực (nếu không dùng validHours)

    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ============ Cấu hình phát coupon tự động ============

    @Field("is_auto_on_upgrade")
    @Builder.Default
    private Boolean isAutoOnUpgrade = false; // Tự động phát khi user nâng hạng lên segment này

    @Field("schedule_frequency")
    @Builder.Default
    private ScheduleFrequency scheduleFrequency = ScheduleFrequency.NONE;

    @Field("schedule_day_of_week")
    private DayOfWeek scheduleDayOfWeek; // Cho WEEKLY: thứ mấy (MONDAY, TUESDAY...)

    @Field("schedule_day_of_month")
    private Integer scheduleDayOfMonth; // Cho MONTHLY: ngày bao nhiêu (1-31)

    @Field("schedule_month_day")
    private String scheduleMonthDay; // Cho YEARLY: format "MM-DD" (VD: "01-01" cho ngày 1/1)

    @Field("schedule_time")
    @Builder.Default
    private LocalTime scheduleTime = LocalTime.of(0, 0); // Giờ phát trong ngày

    @Field("next_scheduled_date")
    private LocalDateTime nextScheduledDate; // Lần phát tiếp theo

    @Field("max_uses_per_user")
    private Integer maxUsesPerUser; // Số lần tối đa 1 user có thể nhận coupon này, null = unlimited

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}

