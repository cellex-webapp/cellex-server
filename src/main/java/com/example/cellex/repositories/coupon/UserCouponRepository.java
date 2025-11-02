package com.example.cellex.repositories.coupon;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.models.coupon.UserCoupon;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCouponRepository extends MongoRepository<UserCoupon, String> {
    
    List<UserCoupon> findByUserIdAndStatus(String userId, CouponStatus status);
    
    List<UserCoupon> findByUserId(String userId);
    
    Optional<UserCoupon> findByCode(String code);
    
    // Kiểm tra user đã nhận coupon này chưa
    boolean existsByUserIdAndSegmentCouponId(String userId, String segmentCouponId);
    
    // Đếm số lần user đã nhận coupon này
    long countByUserIdAndSegmentCouponId(String userId, String segmentCouponId);
    
    // Tìm các coupon đã hết hạn để cập nhật status
    List<UserCoupon> findByStatusAndExpiresAtBefore(CouponStatus status, LocalDateTime expiresAt);
    
    // Đếm số lần user đã nhận coupon từ campaign này
    long countByUserIdAndCampaignId(String userId, String campaignId);
    
    // Kiểm tra user đã nhận coupon từ campaign này chưa
    boolean existsByUserIdAndCampaignId(String userId, String campaignId);
}

