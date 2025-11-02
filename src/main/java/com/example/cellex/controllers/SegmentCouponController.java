package com.example.cellex.controllers;

import com.example.cellex.dtos.request.coupon.CreateSegmentCouponRequest;
import com.example.cellex.dtos.request.coupon.UpdateSegmentCouponRequest;
import com.example.cellex.dtos.response.coupon.SegmentCouponResponse;
import com.example.cellex.services.segment.SegmentCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/segment-coupons")
@RequiredArgsConstructor
@Tag(name = "05. Segment Coupons", description = "API quản lý coupon theo phân khúc khách hàng với cấu hình lịch phát linh hoạt")
public class SegmentCouponController {

    private final SegmentCouponService segmentCouponService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Tạo coupon cho phân khúc khách hàng",
        description = """
            Tạo coupon mới với cấu hình lịch phát linh hoạt. **Chỉ ADMIN có quyền.**
            
            **Lưu ý về các ENUM:**
            
            **1. DiscountType:**
            - PERCENTAGE: Giảm theo phần trăm (discountValue <= 100)
            - FIXED: Giảm số tiền cố định
            
            **2. ScheduleFrequency và các trường liên quan:**
            - NONE: Không phát theo lịch
              + Không cần trường nào thêm
            
            - DAILY: Phát hàng ngày
              + Bắt buộc: scheduleTime (giờ phát trong ngày)
              + VD: scheduleTime = "09:00:00" → phát lúc 9h sáng mỗi ngày
            
            - WEEKLY: Phát hàng tuần
              + Bắt buộc: scheduleDayOfWeek, scheduleTime
              + scheduleDayOfWeek: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
              + VD: scheduleDayOfWeek = FRIDAY, scheduleTime = "18:00:00" → phát mỗi thứ 6 lúc 6h chiều
            
            - MONTHLY: Phát hàng tháng
              + Bắt buộc: scheduleDayOfMonth (1-31), scheduleTime
              + VD: scheduleDayOfMonth = 1, scheduleTime = "00:00:00" → phát vào 0h ngày 1 hàng tháng
            
            - YEARLY: Phát hàng năm
              + Bắt buộc: scheduleMonthDay (format MM-DD), scheduleTime
              + VD: scheduleMonthDay = "01-01", scheduleTime = "00:00:00" → phát lúc 0h ngày 1/1 hàng năm
            
            **3. DayOfWeek (cho WEEKLY):**
            - MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
            
            **Business Rules:**
            - Nếu discountType = PERCENTAGE thì discountValue phải <= 100
            - validHours ưu tiên hơn startDate/endDate
            - isAutoOnUpgrade có thể kết hợp với scheduleFrequency
            - maxUsesPerUser = null nghĩa là không giới hạn
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo coupon thành công"),
            @ApiResponse(responseCode = "400", description = "Validation error hoặc dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN")
        }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Thông tin coupon cần tạo",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Coupon phát hàng ngày",
                    summary = "Phát mỗi ngày lúc 9h sáng",
                    value = """
                        {
                          "segmentId": "65a1b2c3d4e5f6g7h8i9j0k1",
                          "codePrefix": "DAILY",
                          "title": "Deal mỗi ngày",
                          "description": "Giảm 50,000đ mỗi ngày",
                          "discountType": "FIXED",
                          "discountValue": 50000,
                          "minOrderAmount": 400000,
                          "validHours": 24,
                          "isActive": true,
                          "isAutoOnUpgrade": false,
                          "scheduleFrequency": "DAILY",
                          "scheduleTime": "09:00:00",
                          "maxUsesPerUser": 365
                        }
                        """
                ),
                @ExampleObject(
                    name = "Coupon phát hàng tuần",
                    summary = "Phát mỗi thứ 6 lúc 18h",
                    value = """
                        {
                          "segmentId": "65a1b2c3d4e5f6g7h8i9j0k1",
                          "codePrefix": "WEEKEND",
                          "title": "Flash Sale cuối tuần",
                          "description": "Giảm 15% mỗi thứ 6",
                          "discountType": "PERCENTAGE",
                          "discountValue": 15,
                          "minOrderAmount": 500000,
                          "validHours": 72,
                          "isActive": true,
                          "isAutoOnUpgrade": false,
                          "scheduleFrequency": "WEEKLY",
                          "scheduleDayOfWeek": "FRIDAY",
                          "scheduleTime": "18:00:00",
                          "maxUsesPerUser": 52
                        }
                        """
                ),
                @ExampleObject(
                    name = "Coupon phát hàng tháng",
                    summary = "Phát vào ngày 1 hàng tháng",
                    value = """
                        {
                          "segmentId": "65a1b2c3d4e5f6g7h8i9j0k1",
                          "codePrefix": "MONTHLY",
                          "title": "Ưu đãi đầu tháng",
                          "description": "Giảm 10% mỗi đầu tháng",
                          "discountType": "PERCENTAGE",
                          "discountValue": 10,
                          "minOrderAmount": 300000,
                          "validHours": 168,
                          "isActive": true,
                          "isAutoOnUpgrade": false,
                          "scheduleFrequency": "MONTHLY",
                          "scheduleDayOfMonth": 1,
                          "scheduleTime": "00:00:00",
                          "maxUsesPerUser": 12
                        }
                        """
                ),
                @ExampleObject(
                    name = "Coupon phát hàng năm",
                    summary = "Phát ngày Tết (1/1)",
                    value = """
                        {
                          "segmentId": "65a1b2c3d4e5f6g7h8i9j0k1",
                          "codePrefix": "NEWYEAR",
                          "title": "Khuyến mãi đầu năm",
                          "description": "Giảm 500,000đ dịp năm mới",
                          "discountType": "FIXED",
                          "discountValue": 500000,
                          "minOrderAmount": 2000000,
                          "validHours": 168,
                          "isActive": true,
                          "isAutoOnUpgrade": false,
                          "scheduleFrequency": "YEARLY",
                          "scheduleMonthDay": "01-01",
                          "scheduleTime": "00:00:00",
                          "maxUsesPerUser": 1
                        }
                        """
                ),
                @ExampleObject(
                    name = "Coupon tự động khi nâng hạng",
                    summary = "Chỉ phát khi user nâng hạng",
                    value = """
                        {
                          "segmentId": "65a1b2c3d4e5f6g7h8i9j0k1",
                          "codePrefix": "GOLD",
                          "title": "Bonus nâng hạng Gold",
                          "description": "Giảm 300,000đ khi nâng hạng lên Gold",
                          "discountType": "FIXED",
                          "discountValue": 300000,
                          "minOrderAmount": 1000000,
                          "validHours": 720,
                          "isActive": true,
                          "isAutoOnUpgrade": true,
                          "scheduleFrequency": "NONE"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Coupon kết hợp",
                    summary = "Vừa phát khi nâng hạng, vừa phát hàng tháng",
                    value = """
                        {
                          "segmentId": "65a1b2c3d4e5f6g7h8i9j0k1",
                          "codePrefix": "VIP",
                          "title": "VIP Monthly Bonus",
                          "description": "Giảm 20% cho VIP",
                          "discountType": "PERCENTAGE",
                          "discountValue": 20,
                          "minOrderAmount": 800000,
                          "validHours": 720,
                          "isActive": true,
                          "isAutoOnUpgrade": true,
                          "scheduleFrequency": "MONTHLY",
                          "scheduleDayOfMonth": 15,
                          "scheduleTime": "12:00:00",
                          "maxUsesPerUser": 24
                        }
                        """
                )
            }
        )
    )
    public ResponseEntity<SegmentCouponResponse> createCoupon(
            @Valid @RequestBody CreateSegmentCouponRequest request
    ) {
        return ResponseEntity.ok(segmentCouponService.createCoupon(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cập nhật coupon",
        description = """
            Cập nhật thông tin coupon. **Chỉ ADMIN có quyền.**
            
            Tất cả các trường đều optional. Chỉ cập nhật các trường được gửi lên.
            
            Nếu thay đổi cấu hình lịch (scheduleFrequency hoặc các trường liên quan), 
            hệ thống sẽ tự động tính lại nextScheduledDate.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy coupon")
        }
    )
    public ResponseEntity<SegmentCouponResponse> updateCoupon(
            @Parameter(description = "ID của coupon cần cập nhật", required = true)
            @PathVariable String id,
            @Valid @RequestBody UpdateSegmentCouponRequest request
    ) {
        return ResponseEntity.ok(segmentCouponService.updateCoupon(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Xóa coupon",
        description = """
            Xóa coupon khỏi hệ thống. **Chỉ ADMIN có quyền.**
            
            ⚠️ Lưu ý: User coupons đã phát từ segment coupon này sẽ không bị xóa.
            """,
        responses = {
            @ApiResponse(responseCode = "204", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy coupon")
        }
    )
    public ResponseEntity<Void> deleteCoupon(
            @Parameter(description = "ID của coupon cần xóa", required = true)
            @PathVariable String id
    ) {
        segmentCouponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Lấy thông tin chi tiết coupon",
        description = "Lấy đầy đủ thông tin của một coupon theo ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy coupon")
        }
    )
    public ResponseEntity<SegmentCouponResponse> getCouponById(
            @Parameter(description = "ID của coupon", required = true)
            @PathVariable String id
    ) {
        return ResponseEntity.ok(segmentCouponService.getCouponById(id));
    }

    @GetMapping
    @Operation(
        summary = "Lấy danh sách tất cả coupon",
        description = """
            Lấy danh sách tất cả segment coupons trong hệ thống.
            
            Response bao gồm:
            - Coupon active và inactive
            - Thông tin cấu hình lịch phát
            - nextScheduledDate (lần phát tiếp theo)
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công")
        }
    )
    public ResponseEntity<List<SegmentCouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(segmentCouponService.getAllCoupons());
    }

    @GetMapping("/segment/{segmentId}")
    @Operation(
        summary = "Lấy danh sách coupon theo segment",
        description = """
            Lấy tất cả coupons ACTIVE của một customer segment cụ thể.
            
            Hữu ích để:
            - Xem các coupon đang áp dụng cho một phân khúc
            - Admin kiểm tra cấu hình coupon cho từng segment
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công")
        }
    )
    public ResponseEntity<List<SegmentCouponResponse>> getCouponsBySegmentId(
            @Parameter(description = "ID của customer segment", required = true)
            @PathVariable String segmentId
    ) {
        return ResponseEntity.ok(segmentCouponService.getCouponsBySegmentId(segmentId));
    }
}

