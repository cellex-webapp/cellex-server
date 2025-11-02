package com.example.cellex.services.coupon;

import com.example.cellex.dtos.request.coupon.CampaignRecipientFilter;
import com.example.cellex.dtos.request.coupon.CreateCampaignRequest;
import com.example.cellex.dtos.request.coupon.UpdateCampaignRequest;
import com.example.cellex.dtos.response.coupon.CampaignDistributionResponse;
import com.example.cellex.dtos.response.coupon.CouponCampaignResponse;
import com.example.cellex.enums.CampaignStatus;
import com.example.cellex.enums.DistributionType;
import com.example.cellex.enums.IssuedVia;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.coupon.CampaignDistributionLog;
import com.example.cellex.models.coupon.CouponCampaign;
import com.example.cellex.models.user.User;
import com.example.cellex.models.coupon.UserCoupon;
import com.example.cellex.repositories.coupon.CampaignDistributionLogRepository;
import com.example.cellex.repositories.coupon.CouponCampaignRepository;
import com.example.cellex.repositories.coupon.UserCouponRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCampaignService {

    private final CouponCampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final CampaignDistributionLogRepository distributionLogRepository;
    private final MongoTemplate mongoTemplate;

    public CouponCampaignResponse createCampaign(CreateCampaignRequest request, String adminId) {
        // Validation
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
        }

        if (request.getCouponType().name().equals("PERCENTAGE") && request.getDiscountValue() > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Giảm giá % không được vượt quá 100");
        }

        CampaignStatus initialStatus = request.getScheduledAt() != null 
                ? CampaignStatus.SCHEDULED 
                : CampaignStatus.DRAFT;

        CouponCampaign campaign = CouponCampaign.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .codeTemplate(request.getCodeTemplate())
                .couponType(request.getCouponType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .applicableProductIds(request.getApplicableProductIds())
                .applicableCategoryIds(request.getApplicableCategoryIds())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .distributionType(request.getDistributionType())
                .maxTotalIssuance(request.getMaxTotalIssuance())
                .perUserLimit(request.getPerUserLimit())
                .currentIssuance(0)
                .status(initialStatus)
                .scheduledAt(request.getScheduledAt())
                .isActive(true)
                .createdBy(adminId)
                .note(request.getNote())
                .build();

        campaign = campaignRepository.save(campaign);
        log.info("Created campaign: {} by admin: {}", campaign.getId(), adminId);

        return mapToResponse(campaign);
    }

    public CouponCampaignResponse updateCampaign(String id, UpdateCampaignRequest request) {
        CouponCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND, "Chiến dịch khuyến mãi không tìm thấy"));

        // Không cho update campaign đã phát
        if (campaign.getStatus() == CampaignStatus.ACTIVE || campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể sửa chiến dịch khuyến mãi đã phát hoặc hoàn thành");
        }

        // Update fields
        if (request.getTitle() != null) campaign.setTitle(request.getTitle());
        if (request.getDescription() != null) campaign.setDescription(request.getDescription());
        if (request.getCodeTemplate() != null) campaign.setCodeTemplate(request.getCodeTemplate());
        if (request.getCouponType() != null) campaign.setCouponType(request.getCouponType());
        if (request.getDiscountValue() != null) campaign.setDiscountValue(request.getDiscountValue());
        if (request.getMinOrderAmount() != null) campaign.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getApplicableProductIds() != null) campaign.setApplicableProductIds(request.getApplicableProductIds());
        if (request.getApplicableCategoryIds() != null) campaign.setApplicableCategoryIds(request.getApplicableCategoryIds());
        if (request.getStartDate() != null) campaign.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) campaign.setEndDate(request.getEndDate());
        if (request.getDistributionType() != null) campaign.setDistributionType(request.getDistributionType());
        if (request.getMaxTotalIssuance() != null) campaign.setMaxTotalIssuance(request.getMaxTotalIssuance());
        if (request.getPerUserLimit() != null) campaign.setPerUserLimit(request.getPerUserLimit());
        if (request.getScheduledAt() != null) campaign.setScheduledAt(request.getScheduledAt());
        if (request.getStatus() != null) campaign.setStatus(request.getStatus());
        if (request.getIsActive() != null) campaign.setIsActive(request.getIsActive());
        if (request.getNote() != null) campaign.setNote(request.getNote());

        campaign = campaignRepository.save(campaign);
        return mapToResponse(campaign);
    }

    public void deleteCampaign(String id) {
        CouponCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND, "Chiến dịch khuyến mãi không tìm thấy"));

        if (campaign.getStatus() == CampaignStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể xóa chiến dịch khuyến mãi đang chạy");
        }
        
        campaignRepository.delete(campaign);
    }

    public CouponCampaignResponse getCampaignById(String id) {
        CouponCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND, "Chiến dịch khuyến mãi không tìm thấy"));
        return mapToResponse(campaign);
    }

    public List<CouponCampaignResponse> getAllCampaigns() {
        return campaignRepository.findByIsActiveOrderByCreatedAtDesc(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CouponCampaignResponse> getCampaignsByStatus(CampaignStatus status) {
        return campaignRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampaignDistributionResponse distributeCampaign(String campaignId, CampaignRecipientFilter filter, String adminId) {
        long startTime = System.currentTimeMillis();

        CouponCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND, "Chiến dịch khuyến mãi không tìm thấy"));

        if (!campaign.getIsActive()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chiến dịch khuyến mãi chưa được kích hoạt");
        }

        // Filter users
        List<User> recipients = filterUsers(filter);
        log.info("Found {} recipients for campaign {}", recipients.size(), campaignId);

        int successCount = 0;
        int failedCount = 0;
        StringBuilder errorSummary = new StringBuilder();

        for (User user : recipients) {
            try {
                // Kiểm tra đã nhận chưa
                if (campaign.getPerUserLimit() != null) {
                    long receivedCount = userCouponRepository.countByUserIdAndCampaignId(user.getId(), campaignId);
                    if (receivedCount >= campaign.getPerUserLimit()) {
                        continue;
                    }
                }

                // Kiểm tra max total issuance
                if (campaign.getMaxTotalIssuance() != null && 
                    campaign.getCurrentIssuance() >= campaign.getMaxTotalIssuance()) {
                    log.warn("Đã đạt giới hạn phát phiếu giảm giá tối đa cho chiến dịch khuyến mãi {}", campaignId);
                    break;
                }

                // Tạo coupon
                String code = generateCouponCode(campaign, user.getId());
                
                UserCoupon userCoupon = UserCoupon.builder()
                        .userId(user.getId())
                        .campaignId(campaignId)
                        .code(code)
                        .title(campaign.getTitle())
                        .description(campaign.getDescription())
                        .couponType(campaign.getCouponType())
                        .discountValue(campaign.getDiscountValue())
                        .minOrderAmount(campaign.getMinOrderAmount())
                        .applicableProductIds(campaign.getApplicableProductIds())
                        .applicableCategoryIds(campaign.getApplicableCategoryIds())
                        .issuedDate(LocalDateTime.now())
                        .expiresAt(campaign.getEndDate().withHour(23).withMinute(59).withSecond(59))
                        .issuedVia(IssuedVia.CAMPAIGN)
                        .issuedBy(adminId)
                        .build();

                userCouponRepository.save(userCoupon);
                successCount++;

                // Update campaign
                campaign.setCurrentIssuance(campaign.getCurrentIssuance() + 1);

            } catch (Exception e) {
                failedCount++;
                errorSummary.append(String.format("User %s: %s; ", user.getId(), e.getMessage()));
                log.error("Không thể phát phiếu giảm giá cho người dùng {}: {}", user.getId(), e.getMessage());
            }
        }

        // Update campaign status
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setDistributedAt(LocalDateTime.now());
        campaignRepository.save(campaign);

        // Save log
        CampaignDistributionLog distributionLog = CampaignDistributionLog.builder()
                .campaignId(campaignId)
                .adminId(adminId)
                .filterCriteria(filterToMap(filter))
                .recipientsCount(recipients.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errorSummary(errorSummary.toString())
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();

        distributionLog = distributionLogRepository.save(distributionLog);

        log.info("Distributed campaign {}: {} success, {} failed", campaignId, successCount, failedCount);

        return CampaignDistributionResponse.builder()
                .id(distributionLog.getId())
                .campaignId(campaignId)
                .campaignTitle(campaign.getTitle())
                .adminId(adminId)
                .filterCriteria(distributionLog.getFilterCriteria())
                .recipientsCount(recipients.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errorSummary(errorSummary.toString())
                .executionTimeMs(distributionLog.getExecutionTimeMs())
                .createdAt(distributionLog.getCreatedAt())
                .build();
    }

    private List<User> filterUsers(CampaignRecipientFilter filter) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Base: user active và không bị ban
        criteriaList.add(Criteria.where("is_active").is(true));
        criteriaList.add(Criteria.where("is_banned").is(false));

        // All users
        if (Boolean.TRUE.equals(filter.getAll())) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
            return mongoTemplate.find(query, User.class);
        }

        // Explicit user IDs
        if (filter.getExplicitUserIds() != null && !filter.getExplicitUserIds().isEmpty()) {
            criteriaList.add(Criteria.where("_id").in(filter.getExplicitUserIds()));
        }

        // By segment
        if (filter.getCustomerSegmentId() != null) {
            criteriaList.add(Criteria.where("customer_segment_id").is(filter.getCustomerSegmentId()));
        }

        // By total spend
        if (filter.getMinTotalSpend() != null) {
            criteriaList.add(Criteria.where("total_spend").gte(filter.getMinTotalSpend()));
        }
        if (filter.getMaxTotalSpend() != null) {
            criteriaList.add(Criteria.where("total_spend").lte(filter.getMaxTotalSpend()));
        }

        // By registration date
        if (filter.getRegisteredBefore() != null) {
            criteriaList.add(Criteria.where("created_at").lte(filter.getRegisteredBefore()));
        }
        if (filter.getRegisteredAfter() != null) {
            criteriaList.add(Criteria.where("created_at").gte(filter.getRegisteredAfter()));
        }

        // By location
        if (filter.getCity() != null) {
            criteriaList.add(Criteria.where("address.province_name").regex(filter.getCity(), "i"));
        }
        if (filter.getDistrict() != null) {
            criteriaList.add(Criteria.where("address.commune_name").regex(filter.getDistrict(), "i"));
        }

        // Exclude users
        if (filter.getExcludeUserIds() != null && !filter.getExcludeUserIds().isEmpty()) {
            criteriaList.add(Criteria.where("_id").nin(filter.getExcludeUserIds()));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.find(query, User.class);
    }

    private String generateCouponCode(CouponCampaign campaign, String userId) {
        if (campaign.getDistributionType() == DistributionType.SHARED_CODE) {
            return campaign.getCodeTemplate();
        }

        // UNIQUE_PER_USER
        String code;
        int attempts = 0;
        do {
            String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            code = (campaign.getCodeTemplate() != null ? campaign.getCodeTemplate() + "-" : "") + uuid;
            attempts++;
        } while (userCouponRepository.findByCode(code).isPresent() && attempts < 10);

        if (attempts >= 10) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo mã phiếu giảm giá unique");
        }

        return code;
    }

    private Map<String, Object> filterToMap(CampaignRecipientFilter filter) {
        Map<String, Object> map = new HashMap<>();
        if (filter.getAll() != null) map.put("all", filter.getAll());
        if (filter.getCustomerSegmentId() != null) map.put("customerSegmentId", filter.getCustomerSegmentId());
        if (filter.getMinTotalSpend() != null) map.put("minTotalSpend", filter.getMinTotalSpend());
        if (filter.getMaxTotalSpend() != null) map.put("maxTotalSpend", filter.getMaxTotalSpend());
        if (filter.getRegisteredBefore() != null) map.put("registeredBefore", filter.getRegisteredBefore());
        if (filter.getRegisteredAfter() != null) map.put("registeredAfter", filter.getRegisteredAfter());
        if (filter.getExplicitUserIds() != null) map.put("explicitUserIds", filter.getExplicitUserIds());
        return map;
    }

    private CouponCampaignResponse mapToResponse(CouponCampaign campaign) {
        return CouponCampaignResponse.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .description(campaign.getDescription())
                .codeTemplate(campaign.getCodeTemplate())
                .couponType(campaign.getCouponType())
                .discountValue(campaign.getDiscountValue())
                .minOrderAmount(campaign.getMinOrderAmount())
                .applicableProductIds(campaign.getApplicableProductIds())
                .applicableCategoryIds(campaign.getApplicableCategoryIds())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .distributionType(campaign.getDistributionType())
                .maxTotalIssuance(campaign.getMaxTotalIssuance())
                .perUserLimit(campaign.getPerUserLimit())
                .currentIssuance(campaign.getCurrentIssuance())
                .status(campaign.getStatus())
                .scheduledAt(campaign.getScheduledAt())
                .distributedAt(campaign.getDistributedAt())
                .isActive(campaign.getIsActive())
                .createdBy(campaign.getCreatedBy())
                .note(campaign.getNote())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }

    public List<CampaignDistributionResponse> getCampaignDistributionLogs(String campaignId) {
        return distributionLogRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(log -> CampaignDistributionResponse.builder()
                        .id(log.getId())
                        .campaignId(log.getCampaignId())
                        .adminId(log.getAdminId())
                        .filterCriteria(log.getFilterCriteria())
                        .recipientsCount(log.getRecipientsCount())
                        .successCount(log.getSuccessCount())
                        .failedCount(log.getFailedCount())
                        .errorSummary(log.getErrorSummary())
                        .executionTimeMs(log.getExecutionTimeMs())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

