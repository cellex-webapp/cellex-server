package com.example.cellex.services;

import com.example.cellex.dtos.response.UserCouponResponse;
import com.example.cellex.enums.CouponStatus;
import com.example.cellex.enums.CouponType;
import com.example.cellex.enums.IssuedVia;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.SegmentCoupon;
import com.example.cellex.models.User;
import com.example.cellex.models.UserCoupon;
import com.example.cellex.repositories.SegmentCouponRepository;
import com.example.cellex.repositories.UserCouponRepository;
import com.example.cellex.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final SegmentCouponRepository segmentCouponRepository;

    public UserCouponResponse issueCouponToUser(String userId, String segmentCouponId, IssuedVia issuedVia, String issuedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        SegmentCoupon segmentCoupon = segmentCouponRepository.findById(segmentCouponId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        if (!segmentCoupon.getIsActive()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Coupon không còn hoạt động");
        }

        // Kiểm tra giới hạn số lần nhận
        if (segmentCoupon.getMaxUsesPerUser() != null) {
            long usageCount = userCouponRepository.countByUserIdAndSegmentCouponId(userId, segmentCouponId);
            if (usageCount >= segmentCoupon.getMaxUsesPerUser()) {
                log.info("User {} đã vượt quá giới hạn nhận coupon {}", userId, segmentCouponId);
                throw new AppException(ErrorCode.INVALID_REQUEST, "Đã vượt quá số lần nhận coupon này");
            }
        }

        // Tạo mã coupon unique
        String code = generateCouponCode(segmentCoupon.getCodePrefix());

        // Tính thời gian hết hạn
        LocalDateTime expiresAt;
        if (segmentCoupon.getValidHours() != null) {
            expiresAt = LocalDateTime.now().plusHours(segmentCoupon.getValidHours());
        } else if (segmentCoupon.getEndDate() != null) {
            expiresAt = segmentCoupon.getEndDate().atTime(23, 59, 59);
        } else {
            expiresAt = LocalDateTime.now().plusYears(1); // Mặc định 1 năm
        }

        // Convert DiscountType sang CouponType
        CouponType couponType = convertDiscountTypeToCouponType(segmentCoupon.getDiscountType());

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .segmentCouponId(segmentCouponId)
                .code(code)
                .title(segmentCoupon.getTitle())
                .description(segmentCoupon.getDescription())
                .couponType(couponType)
                .discountValue(segmentCoupon.getDiscountValue())
                .minOrderAmount(segmentCoupon.getMinOrderAmount())
                .issuedDate(LocalDateTime.now())
                .expiresAt(expiresAt)
                .status(CouponStatus.ACTIVE)
                .issuedVia(issuedVia)
                .issuedBy(issuedBy)
                .build();

        userCoupon = userCouponRepository.save(userCoupon);
        log.info("Đã phát coupon {} cho user {} (via: {})", code, userId, issuedVia);

        return mapToResponse(userCoupon);
    }

    public List<UserCouponResponse> getUserCoupons(String userId) {
        return userCouponRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserCouponResponse> getUserActiveCoupons(String userId) {
        return userCouponRepository.findByUserIdAndStatus(userId, CouponStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserCouponResponse redeemCoupon(String code, String orderId) {
        UserCoupon coupon = userCouponRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Coupon không còn hiệu lực");
        }

        if (coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            coupon.setStatus(CouponStatus.EXPIRED);
            userCouponRepository.save(coupon);
            throw new AppException(ErrorCode.INVALID_REQUEST, "Coupon đã hết hạn");
        }

        coupon.setStatus(CouponStatus.REDEEMED);
        coupon.setRedeemedOrderId(orderId);
        coupon.setRedeemedAt(LocalDateTime.now());

        coupon = userCouponRepository.save(coupon);
        log.info("Coupon {} đã được sử dụng cho đơn hàng {}", code, orderId);

        return mapToResponse(coupon);
    }

    public void updateExpiredCoupons() {
        List<UserCoupon> expiredCoupons = userCouponRepository.findByStatusAndExpiresAtBefore(
                CouponStatus.ACTIVE, 
                LocalDateTime.now()
        );

        for (UserCoupon coupon : expiredCoupons) {
            coupon.setStatus(CouponStatus.EXPIRED);
        }

        if (!expiredCoupons.isEmpty()) {
            userCouponRepository.saveAll(expiredCoupons);
            log.info("Đã cập nhật {} coupon hết hạn", expiredCoupons.size());
        }
    }

    private String generateCouponCode(String prefix) {
        String code;
        int attempts = 0;
        do {
            String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            code = (prefix != null ? prefix + "-" : "") + uuid;
            attempts++;
        } while (userCouponRepository.findByCode(code).isPresent() && attempts < 10);

        if (attempts >= 10) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo mã coupon unique");
        }

        return code;
    }

    private CouponType convertDiscountTypeToCouponType(com.example.cellex.enums.DiscountType discountType) {
        switch (discountType) {
            case PERCENTAGE:
                return CouponType.PERCENTAGE;
            case FIXED:
                return CouponType.FIXED;
            default:
                return CouponType.FIXED;
        }
    }

    private UserCouponResponse mapToResponse(UserCoupon coupon) {
        return UserCouponResponse.builder()
                .id(coupon.getId())
                .userId(coupon.getUserId())
                .segmentCouponId(coupon.getSegmentCouponId())
                .campaignId(coupon.getCampaignId())
                .code(coupon.getCode())
                .title(coupon.getTitle())
                .description(coupon.getDescription())
                .couponType(coupon.getCouponType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .applicableProductIds(coupon.getApplicableProductIds())
                .applicableCategoryIds(coupon.getApplicableCategoryIds())
                .issuedDate(coupon.getIssuedDate())
                .expiresAt(coupon.getExpiresAt())
                .status(coupon.getStatus())
                .redeemedOrderId(coupon.getRedeemedOrderId())
                .redeemedAt(coupon.getRedeemedAt())
                .issuedVia(coupon.getIssuedVia())
                .issuedBy(coupon.getIssuedBy())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}

