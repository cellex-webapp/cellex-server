package com.example.cellex.dtos.request.coupon;

import com.example.cellex.enums.DiscountType;
import com.example.cellex.enums.ScheduleFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request cập nhật coupon. Tất cả các trường đều optional, chỉ gửi trường cần cập nhật.")
public class UpdateSegmentCouponRequest {
    
    @Schema(description = "Tiền tố cho mã coupon", example = "GOLD")
    private String codePrefix;
    
    @Schema(description = "Tiêu đề coupon", example = "Giảm 20% cho Gold Member")
    private String title;
    
    @Schema(description = "Mô tả chi tiết", example = "Ưu đãi đặc biệt")
    private String description;
    
    @Schema(description = "Loại giảm giá", example = "PERCENTAGE", allowableValues = {"PERCENTAGE", "FIXED"})
    private DiscountType discountType;
    
    @Schema(description = "Giá trị giảm (% hoặc số tiền)", example = "20")
    @Min(value = 0, message = "Giá trị giảm giá phải >= 0")
    private Double discountValue;
    
    @Schema(description = "Giá trị đơn hàng tối thiểu", example = "500000")
    @Min(value = 0, message = "Giá trị đơn hàng tối thiểu phải >= 0")
    private Double minOrderAmount;
    
    @Schema(description = "Số giờ có hiệu lực", example = "720")
    @Min(value = 1, message = "Số giờ hiệu lực phải >= 1")
    private Integer validHours;
    
    @Schema(description = "Ngày bắt đầu", example = "2024-01-01")
    private LocalDate startDate;
    
    @Schema(description = "Ngày kết thúc", example = "2024-12-31")
    private LocalDate endDate;
    
    @Schema(description = "Trạng thái active", example = "false")
    private Boolean isActive;
    
    // ========== Cấu hình phát coupon tự động ==========
    
    @Schema(description = "Tự động phát khi nâng hạng", example = "true")
    private Boolean isAutoOnUpgrade;
    
    @Schema(description = "Tần suất phát: NONE, DAILY, WEEKLY, MONTHLY, YEARLY", 
            example = "WEEKLY",
            allowableValues = {"NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"})
    private ScheduleFrequency scheduleFrequency;
    
    @Schema(description = "Thứ trong tuần (cho WEEKLY)", 
            example = "MONDAY",
            allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"})
    private DayOfWeek scheduleDayOfWeek;
    
    @Schema(description = "Ngày trong tháng (cho MONTHLY): 1-31", example = "15", minimum = "1", maximum = "31")
    @Min(value = 1, message = "Ngày trong tháng phải từ 1-31")
    private Integer scheduleDayOfMonth;
    
    @Schema(description = "Ngày-tháng hàng năm (cho YEARLY): format MM-DD", example = "12-25", pattern = "^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")
    private String scheduleMonthDay;
    
    @Schema(description = "Giờ phát trong ngày: format HH:mm:ss", example = "14:30:00")
    private LocalTime scheduleTime;
    
    @Schema(description = "Số lần nhận tối đa. Null = unlimited", example = "5", minimum = "1")
    @Min(value = 1, message = "Số lần nhận tối đa phải >= 1")
    private Integer maxUsesPerUser;
}

