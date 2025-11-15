package com.example.cellex.services.segment;

import com.example.cellex.dtos.request.coupon.CreateSegmentCouponRequest;
import com.example.cellex.dtos.request.coupon.UpdateSegmentCouponRequest;
import com.example.cellex.dtos.response.coupon.SegmentCouponResponse;
import com.example.cellex.dtos.response.segment.CustomerSegmentResponse;
import com.example.cellex.enums.ScheduleFrequency;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.coupon.SegmentCoupon;
import com.example.cellex.repositories.segment.CustomerSegmentRepository;
import com.example.cellex.repositories.coupon.SegmentCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SegmentCouponService {

    private final SegmentCouponRepository segmentCouponRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final CustomerSegmentService customerSegmentService;

    public SegmentCouponResponse createCoupon(CreateSegmentCouponRequest request) {
        // Validate segment exists
        customerSegmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new AppException(ErrorCode.SEGMENT_NOT_FOUND));

        // Validate discount value
        if (request.getDiscountType().name().equals("PERCENTAGE") && request.getDiscountValue() > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Phần trăm giảm giá không được vượt quá 100%");
        }

        SegmentCoupon coupon = SegmentCoupon.builder()
                .segmentId(request.getSegmentId())
                .codePrefix(request.getCodePrefix())
                .title(request.getTitle())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .validHours(request.getValidHours())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isAutoOnUpgrade(request.getIsAutoOnUpgrade() != null ? request.getIsAutoOnUpgrade() : false)
                .scheduleFrequency(request.getScheduleFrequency())
                .scheduleDayOfWeek(request.getScheduleDayOfWeek())
                .scheduleDayOfMonth(request.getScheduleDayOfMonth())
                .scheduleMonthDay(request.getScheduleMonthDay())
                .scheduleTime(request.getScheduleTime() != null ? request.getScheduleTime() : LocalTime.of(0, 0))
                .maxUsesPerUser(request.getMaxUsesPerUser())
                .build();

        // Tính nextScheduledDate nếu có schedule
        if (coupon.getScheduleFrequency() != null && coupon.getScheduleFrequency() != ScheduleFrequency.NONE) {
            coupon.setNextScheduledDate(calculateNextScheduledDate(coupon));
        }

        coupon = segmentCouponRepository.save(coupon);
        return mapToResponse(coupon);
    }

    public SegmentCouponResponse updateCoupon(String id, UpdateSegmentCouponRequest request) {
        SegmentCoupon coupon = segmentCouponRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        if (request.getCodePrefix() != null) {
            coupon.setCodePrefix(request.getCodePrefix());
        }
        if (request.getTitle() != null) {
            coupon.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            coupon.setDescription(request.getDescription());
        }
        if (request.getDiscountType() != null) {
            coupon.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            coupon.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMinOrderAmount() != null) {
            coupon.setMinOrderAmount(request.getMinOrderAmount());
        }
        if (request.getValidHours() != null) {
            coupon.setValidHours(request.getValidHours());
        }
        if (request.getStartDate() != null) {
            coupon.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            coupon.setEndDate(request.getEndDate());
        }
        if (request.getIsActive() != null) {
            coupon.setIsActive(request.getIsActive());
        }
        if (request.getIsAutoOnUpgrade() != null) {
            coupon.setIsAutoOnUpgrade(request.getIsAutoOnUpgrade());
        }
        if (request.getScheduleFrequency() != null) {
            coupon.setScheduleFrequency(request.getScheduleFrequency());
        }
        if (request.getScheduleDayOfWeek() != null) {
            coupon.setScheduleDayOfWeek(request.getScheduleDayOfWeek());
        }
        if (request.getScheduleDayOfMonth() != null) {
            coupon.setScheduleDayOfMonth(request.getScheduleDayOfMonth());
        }
        if (request.getScheduleMonthDay() != null) {
            coupon.setScheduleMonthDay(request.getScheduleMonthDay());
        }
        if (request.getScheduleTime() != null) {
            coupon.setScheduleTime(request.getScheduleTime());
        }
        if (request.getMaxUsesPerUser() != null) {
            coupon.setMaxUsesPerUser(request.getMaxUsesPerUser());
        }

        // Validate discount value
        if (coupon.getDiscountType().name().equals("PERCENTAGE") && coupon.getDiscountValue() > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Phần trăm giảm giá không được vượt quá 100%");
        }

        // Tính lại nextScheduledDate nếu có thay đổi schedule
        if (coupon.getScheduleFrequency() != null && coupon.getScheduleFrequency() != ScheduleFrequency.NONE) {
            coupon.setNextScheduledDate(calculateNextScheduledDate(coupon));
        }

        coupon = segmentCouponRepository.save(coupon);
        return mapToResponse(coupon);
    }

    public void deleteCoupon(String id) {
        SegmentCoupon coupon = segmentCouponRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        segmentCouponRepository.delete(coupon);
    }

    public SegmentCouponResponse getCouponById(String id) {
        SegmentCoupon coupon = segmentCouponRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        return mapToResponse(coupon);
    }

    public List<SegmentCouponResponse> getAllCoupons() {
        return segmentCouponRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SegmentCouponResponse> getCouponsBySegmentId(String segmentId) {
        return segmentCouponRepository.findBySegmentIdAndIsActive(segmentId, true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public org.springframework.data.domain.Page<SegmentCouponResponse> getAllCoupons(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<SegmentCoupon> page = segmentCouponRepository.findAll(pageable);
        return page.map(this::mapToResponse);
    }

    public org.springframework.data.domain.Page<SegmentCouponResponse> getCouponsBySegmentId(String segmentId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<SegmentCoupon> page = segmentCouponRepository.findBySegmentIdAndIsActive(segmentId, true, pageable);
        return page.map(this::mapToResponse);
    }

    public LocalDateTime calculateNextScheduledDate(SegmentCoupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime scheduleTime = coupon.getScheduleTime() != null ? coupon.getScheduleTime() : LocalTime.of(0, 0);

        switch (coupon.getScheduleFrequency()) {
            case DAILY:
                LocalDateTime nextDaily = LocalDateTime.of(LocalDate.now(), scheduleTime);
                if (nextDaily.isBefore(now)) {
                    nextDaily = nextDaily.plusDays(1);
                }
                return nextDaily;

            case WEEKLY:
                if (coupon.getScheduleDayOfWeek() == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Chọn phát theo lịch hàng tuần phải chọn thứ trong tuần phát khuyến mãi");
                }
                LocalDateTime nextWeekly = LocalDateTime.of(
                        LocalDate.now().with(TemporalAdjusters.nextOrSame(coupon.getScheduleDayOfWeek())),
                        scheduleTime
                );
                if (nextWeekly.isBefore(now)) {
                    nextWeekly = nextWeekly.plusWeeks(1);
                }
                return nextWeekly;

            case MONTHLY:
                if (coupon.getScheduleDayOfMonth() == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Chọn phát theo lịch hàng tháng phải chọn ngày trong tháng phát khuyến mãi");
                }
                LocalDateTime nextMonthly = LocalDateTime.of(
                        LocalDate.now().withDayOfMonth(Math.min(coupon.getScheduleDayOfMonth(), LocalDate.now().lengthOfMonth())),
                        scheduleTime
                );
                if (nextMonthly.isBefore(now)) {
                    nextMonthly = nextMonthly.plusMonths(1);
                    nextMonthly = nextMonthly.withDayOfMonth(Math.min(coupon.getScheduleDayOfMonth(), nextMonthly.toLocalDate().lengthOfMonth()));
                }
                return nextMonthly;

            case YEARLY:
                if (coupon.getScheduleMonthDay() == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Chọn phát theo lịch hàng năm phải chọn ngày-tháng phát khuyến mãi");
                }
                String[] parts = coupon.getScheduleMonthDay().split("-");
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                LocalDateTime nextYearly = LocalDateTime.of(
                        LocalDate.of(LocalDate.now().getYear(), month, day),
                        scheduleTime
                );
                if (nextYearly.isBefore(now)) {
                    nextYearly = nextYearly.plusYears(1);
                }
                return nextYearly;

            default:
                return null;
        }
    }

    private SegmentCouponResponse mapToResponse(SegmentCoupon coupon) {
        // Try to fetch full segment response; fallback to null if not found
        CustomerSegmentResponse segmentResp = null;
        try {
            segmentResp = customerSegmentService.getSegmentById(coupon.getSegmentId());
        } catch (Exception ignored) {
        }

        return SegmentCouponResponse.builder()
                .id(coupon.getId())
                .segment(segmentResp)
                .codePrefix(coupon.getCodePrefix())
                .title(coupon.getTitle())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .validHours(coupon.getValidHours())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .isActive(coupon.getIsActive())
                .isAutoOnUpgrade(coupon.getIsAutoOnUpgrade())
                .scheduleFrequency(coupon.getScheduleFrequency())
                .scheduleDayOfWeek(coupon.getScheduleDayOfWeek())
                .scheduleDayOfMonth(coupon.getScheduleDayOfMonth())
                .scheduleMonthDay(coupon.getScheduleMonthDay())
                .scheduleTime(coupon.getScheduleTime())
                .nextScheduledDate(coupon.getNextScheduledDate())
                .maxUsesPerUser(coupon.getMaxUsesPerUser())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
