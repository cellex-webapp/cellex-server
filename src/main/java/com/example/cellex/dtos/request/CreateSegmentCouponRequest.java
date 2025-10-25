package com.example.cellex.dtos.request;

import com.example.cellex.enums.DiscountType;
import com.example.cellex.enums.ScheduleFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo coupon cho phân khúc khách hàng với cấu hình lịch phát linh hoạt")
public class CreateSegmentCouponRequest {
    
    @Schema(description = "ID của customer segment", example = "65a1b2c3d4e5f6g7h8i9j0k1", required = true)
    @NotBlank(message = "Segment ID không được để trống")
    private String segmentId;
    
    @Schema(description = "Tiền tố cho mã coupon (VD: VIP, GOLD, SILVER). Mã cuối cùng sẽ là: {codePrefix}-{UUID}", 
            example = "GOLD")
    private String codePrefix;
    
    @Schema(description = "Tiêu đề coupon", example = "Giảm 15% cho Gold Member", required = true)
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @Schema(description = "Mô tả chi tiết về coupon", example = "Ưu đãi đặc biệt dành cho thành viên Gold")
    private String description;
    
    @Schema(description = "Loại giảm giá: PERCENTAGE (giảm theo %), FIXED (giảm số tiền cố định)", 
            example = "PERCENTAGE", 
            allowableValues = {"PERCENTAGE", "FIXED"},
            required = true)
    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;
    
    @Schema(description = "Giá trị giảm (% nếu PERCENTAGE, số tiền nếu FIXED). Nếu PERCENTAGE thì phải <= 100", 
            example = "15", 
            required = true)
    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Min(value = 0, message = "Giá trị giảm giá phải >= 0")
    private Double discountValue;
    
    @Schema(description = "Giá trị đơn hàng tối thiểu để áp dụng coupon", example = "500000")
    @Min(value = 0, message = "Giá trị đơn hàng tối thiểu phải >= 0")
    private Double minOrderAmount;
    
    @Schema(description = "Số giờ có hiệu lực kể từ khi phát (ưu tiên hơn startDate/endDate). VD: 720 = 30 ngày", 
            example = "720")
    @Min(value = 1, message = "Số giờ hiệu lực phải >= 1")
    private Integer validHours;
    
    @Schema(description = "Ngày bắt đầu có hiệu lực (nếu không dùng validHours)", example = "2024-01-01")
    private LocalDate startDate;
    
    @Schema(description = "Ngày kết thúc có hiệu lực (nếu không dùng validHours)", example = "2024-12-31")
    private LocalDate endDate;
    
    @Schema(description = "Trạng thái active của coupon", example = "true", defaultValue = "true")
    private Boolean isActive;
    
    // ========== Cấu hình phát coupon tự động ==========
    
    @Schema(description = "Tự động phát coupon khi user nâng hạng lên segment này", 
            example = "true", 
            defaultValue = "false")
    private Boolean isAutoOnUpgrade;
    
    @Schema(description = """
            Tần suất phát coupon theo lịch:
            - NONE: Không phát theo lịch (chỉ manual hoặc auto-on-upgrade)
            - DAILY: Phát hàng ngày (cần scheduleTime)
            - WEEKLY: Phát hàng tuần (cần scheduleDayOfWeek và scheduleTime)
            - MONTHLY: Phát hàng tháng (cần scheduleDayOfMonth và scheduleTime)
            - YEARLY: Phát hàng năm (cần scheduleMonthDay và scheduleTime)
            """,
            example = "MONTHLY",
            allowableValues = {"NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"},
            required = true)
    @NotNull(message = "Tần suất lịch phát không được để trống")
    private ScheduleFrequency scheduleFrequency;
    
    @Schema(description = """
            Thứ trong tuần để phát (cho WEEKLY).
            Giá trị: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
            """,
            example = "FRIDAY",
            allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"})
    private DayOfWeek scheduleDayOfWeek;
    
    @Schema(description = "Ngày trong tháng để phát (cho MONTHLY). Giá trị từ 1-31", 
            example = "1",
            minimum = "1",
            maximum = "31")
    @Min(value = 1, message = "Ngày trong tháng phải từ 1-31")
    private Integer scheduleDayOfMonth;
    
    @Schema(description = "Ngày và tháng để phát hàng năm (cho YEARLY). Format: MM-DD. VD: 01-01 là ngày 1/1, 12-25 là ngày 25/12", 
            example = "01-01",
            pattern = "^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")
    private String scheduleMonthDay;
    
    @Schema(description = "Giờ trong ngày để phát coupon (áp dụng cho tất cả frequency). Format: HH:mm:ss", 
            example = "09:00:00",
            defaultValue = "00:00:00")
    private LocalTime scheduleTime;
    
    @Schema(description = "Số lần tối đa 1 user có thể nhận coupon này. Null = unlimited", 
            example = "3",
            minimum = "1")
    @Min(value = 1, message = "Số lần nhận tối đa phải >= 1")
    private Integer maxUsesPerUser;
}

