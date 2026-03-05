package com.example.cellex.repositories.coupon;

import com.example.cellex.enums.CouponStatus;
import com.example.cellex.models.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, UUID> {

    List<UserCoupon> findByUserIdAndStatus(String userId, CouponStatus status);

    List<UserCoupon> findByUserId(String userId);
    Page<UserCoupon> findByUserId(String userId, Pageable pageable);

    Optional<UserCoupon> findByCode(String code);

    Optional<UserCoupon> findByCodeAndUserId(String code, String userId);

    Page<UserCoupon> findByUserIdAndStatus(String userId, CouponStatus status, Pageable pageable);

    boolean existsByUserIdAndSegmentCouponId(String userId, String segmentCouponId);

    long countByUserIdAndSegmentCouponId(String userId, String segmentCouponId);

    List<UserCoupon> findByStatusAndExpiresAtBefore(CouponStatus status, LocalDateTime expiresAt);

    long countByUserIdAndCampaignId(String userId, String campaignId);

    boolean existsByUserIdAndCampaignId(String userId, String campaignId);

    // ==================== Backward-compatible methods ====================

    default Optional<UserCoupon> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default boolean existsById(String id) {
        return existsById(UUID.fromString(id));
    }

    default void deleteById(String id) {
        deleteById(UUID.fromString(id));
    }
}
