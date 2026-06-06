package com.example.cellex.services.warranty;

import com.example.cellex.dtos.request.warranty.ClaimStatusUpdateRequest;
import com.example.cellex.dtos.request.warranty.WarrantyClaimRequest;
import com.example.cellex.dtos.request.warranty.WarrantyPolicyRequest;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.warranty.WarrantyClaimResponse;
import com.example.cellex.enums.NotificationType;
import com.example.cellex.enums.WarrantyStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.models.warranty.WarrantyClaim;
import com.example.cellex.models.warranty.WarrantyPolicy;
import com.example.cellex.repositories.order.OrderItemRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.repositories.warranty.WarrantyClaimRepository;
import com.example.cellex.repositories.warranty.WarrantyPolicyRepository;
import com.example.cellex.services.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WarrantyService {

    private final WarrantyPolicyRepository warrantyPolicyRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    // ================= POLICY MANAGEMENT (VENDOR) =================

    public WarrantyPolicy createOrUpdatePolicy(String productId, WarrantyPolicyRequest request) {
        List<WarrantyPolicy> existingPolicies = warrantyPolicyRepository.findByProductId(productId);
        WarrantyPolicy policy;

        if (!existingPolicies.isEmpty()) {
            policy = existingPolicies.get(0);
            policy.setDurationMonths(request.getDurationMonths());
            policy.setType(request.getType());
            policy.setTerms(request.getTerms());
            log.info("Updated WarrantyPolicy for product: {}", productId);
        } else {
            policy = WarrantyPolicy.builder()
                    .productId(productId)
                    .durationMonths(request.getDurationMonths())
                    .type(request.getType())
                    .terms(request.getTerms())
                    .build();
            log.info("Created new WarrantyPolicy for product: {}", productId);
        }

        return warrantyPolicyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public WarrantyPolicy getPolicyByProductId(String productId) {
        return warrantyPolicyRepository.findByProductId(productId)
                .stream().findFirst()
                .orElseThrow(() -> {
                    log.warn("WarrantyPolicy not found for product: {}", productId);
                    return new AppException(ErrorCode.NOT_FOUND); // Thay bằng mã lỗi phù hợp của dự án
                });
    }

    // ================= CLAIM MANAGEMENT (CLIENT & VENDOR) =================

    public WarrantyClaim createClaim(UUID userId, WarrantyClaimRequest request) {
        // 1. Lấy thông tin OrderItem
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. Tìm Product để lấy shopId (Dựa vào kiến trúc Entity bạn cung cấp)
        Product product = productRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 3. Tạo phiếu bảo hành
        WarrantyClaim claim = WarrantyClaim.builder()
                .orderItemId(orderItem.getUuid())
                .userId(userId)
                .shopId(UUID.fromString(product.getShopId()))
                .status(WarrantyStatus.PENDING)
                .issueDescription(request.getIssueDescription())
                .images(request.getImages())
                .build();

        WarrantyClaim saved = warrantyClaimRepository.save(claim);
        log.info("User {} created WarrantyClaim for OrderItem {}", userId, request.getOrderItemId());

        // 4. Gửi notification cho chủ shop
        notifyShopAboutNewClaim(saved, orderItem, userId);

        return saved;
    }

    private void notifyShopAboutNewClaim(WarrantyClaim claim, OrderItem orderItem, UUID customerUserId) {
        try {
            // Tìm shop và chủ shop
            Shop shop = shopRepository.findById(claim.getShopId().toString())
                    .orElse(null);
            if (shop == null || shop.getOwnerUuid() == null) {
                log.warn("Cannot notify shop owner: shop not found for claim {}", claim.getId());
                return;
            }

            Optional<User> shopOwnerOpt = userRepository.findByUuid(shop.getOwnerUuid());
            if (shopOwnerOpt.isEmpty()) {
                log.warn("Cannot notify shop owner: user not found for uuid {}", shop.getOwnerUuid());
                return;
            }

            // Tìm khách hàng để lấy tên
            Optional<User> customerOpt = userRepository.findByUuid(customerUserId);
            String customerName = customerOpt.map(User::getFullName).orElse("Khách hàng");

            String title = "Yêu cầu bảo hành mới";
            String message = String.format("%s đã gửi yêu cầu bảo hành cho sản phẩm \"%s\". Lý do: %s",
                    customerName,
                    orderItem.getProductName(),
                    claim.getIssueDescription() != null && claim.getIssueDescription().length() > 80
                            ? claim.getIssueDescription().substring(0, 80) + "..."
                            : claim.getIssueDescription());
            String metadata = String.format("{\"claimId\":\"%s\",\"productId\":\"%s\"}",
                    claim.getId(), orderItem.getProductId());
            String actionUrl = "/vendor/warranty";

            notificationService.sendNotificationToUser(
                    shopOwnerOpt.get(), title, message,
                    NotificationType.WARRANTY_CREATED, metadata, actionUrl, null);
        } catch (Exception e) {
            log.error("Failed to send warranty claim notification for claim {}", claim.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> getUserClaims(UUID userId) {
        return warrantyClaimRepository.findByUserId(userId).stream()
                .map(this::mapClaimToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaim> getShopClaims(UUID shopId) {
        return warrantyClaimRepository.findByShopId(shopId);
    }

    public WarrantyClaim updateClaimStatus(UUID shopId, UUID claimId, ClaimStatusUpdateRequest request) {
        WarrantyClaim claim = warrantyClaimRepository.findById(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // BOLA Protection ở mức Service: Đảm bảo phiếu bảo hành này thuộc về shop đang request
        if (!claim.getShopId().equals(shopId)) {
            log.warn("Shop {} attempted to update Claim {} which does not belong to them", shopId, claimId);
            throw new AppException(ErrorCode.UNAUTHORIZED); // Hoặc UNAUTHORIZED tùy dự án
        }

        claim.setStatus(request.getStatus());
        if (request.getShopResponse() != null) {
            claim.setShopResponse(request.getShopResponse());
        }

        log.info("Shop {} updated Claim {} status to {}", shopId, claimId, request.getStatus());
        return warrantyClaimRepository.save(claim);
    }

    // ================= VENDOR ENDPOINTS (no shopId in URL — inferred from auth) =================

    @Transactional(readOnly = true)
    public PageResponse<WarrantyClaimResponse> getShopClaimsForVendor(String vendorId, Pageable pageable, WarrantyStatus status) {
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        Page<WarrantyClaim> page = status != null
                ? warrantyClaimRepository.findByShopIdAndStatus(shop.getId(), status, pageable)
                : warrantyClaimRepository.findByShopId(shop.getId(), pageable);

        return PageResponse.of(page, this::mapClaimToResponse);
    }

    @Transactional
    public WarrantyClaimResponse respondToClaim(String vendorId, UUID claimId, ClaimStatusUpdateRequest request) {
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        WarrantyClaim claim = warrantyClaimRepository.findById(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!claim.getShopId().equals(UUID.fromString(shop.getId()))) {
            log.warn("Shop {} attempted to respond to Claim {} which does not belong to them", shop.getId(), claimId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        claim.setStatus(request.getStatus());
        if (request.getShopResponse() != null) {
            claim.setShopResponse(request.getShopResponse());
        }

        WarrantyClaim saved = warrantyClaimRepository.save(claim);
        log.info("Shop {} responded to Claim {} with status {}", shop.getId(), claimId, request.getStatus());

        // Gửi notification cho khách hàng về cập nhật trạng thái
        notifyCustomerAboutClaimUpdate(saved, shop);

        return mapClaimToResponse(saved);
    }

    private void notifyCustomerAboutClaimUpdate(WarrantyClaim claim, Shop shop) {
        try {
            Optional<User> customerOpt = userRepository.findByUuid(claim.getUserId());
            if (customerOpt.isEmpty()) {
                log.warn("Cannot notify customer: user not found for uuid {}", claim.getUserId());
                return;
            }

            // Lấy thông tin sản phẩm từ OrderItem
            String productName = "sản phẩm";
            Optional<OrderItem> orderItemOpt = orderItemRepository.findById(claim.getOrderItemId());
            if (orderItemOpt.isPresent()) {
                productName = orderItemOpt.get().getProductName();
            }

            String statusLabel = switch (claim.getStatus()) {
                case PROCESSING -> "đang được xử lý";
                case COMPLETED -> "đã hoàn thành";
                case REJECTED -> "đã bị từ chối";
                default -> "đang chờ xử lý";
            };

            String title = "Cập nhật bảo hành";
            String message = String.format("Yêu cầu bảo hành cho \"%s\" của bạn %s bởi %s.",
                    productName, statusLabel, shop.getShopName() != null ? shop.getShopName() : "cửa hàng");
            String metadata = String.format("{\"claimId\":\"%s\",\"status\":\"%s\"}",
                    claim.getId(), claim.getStatus());
            String actionUrl = "/my-account?tab=notifications";

            notificationService.sendNotificationToUser(
                    customerOpt.get(), title, message,
                    NotificationType.WARRANTY_UPDATED, metadata, actionUrl, null);
        } catch (Exception e) {
            log.error("Failed to send warranty update notification for claim {}", claim.getId(), e);
        }
    }

    // ================= Mapper: WarrantyClaim entity → enriched WarrantyClaimResponse =================

    private WarrantyClaimResponse mapClaimToResponse(WarrantyClaim claim) {
        // Parse images JSON string → List<String>
        List<String> imageList = parseImages(claim.getImages());

        // Lookup order item + order for product info
        String productName = null;
        String productImage = null;
        String orderCode = null;
        Optional<OrderItem> orderItemOpt = orderItemRepository.findById(claim.getOrderItemId());
        if (orderItemOpt.isPresent()) {
            OrderItem item = orderItemOpt.get();
            productName = item.getProductName();
            productImage = item.getProductImage();
            if (item.getOrder() != null) {
                orderCode = item.getOrder().getOrderCode();
            }
        }

        // Lookup user for name + email
        String userName = null;
        String userEmail = null;
        Optional<User> userOpt = userRepository.findByUuid(claim.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userName = user.getFullName();
            userEmail = user.getEmail();
        }

        return WarrantyClaimResponse.builder()
                .id(claim.getId() != null ? claim.getId().toString() : null)
                .orderItemId(claim.getOrderItemId() != null ? claim.getOrderItemId().toString() : null)
                .userId(claim.getUserId() != null ? claim.getUserId().toString() : null)
                .shopId(claim.getShopId() != null ? claim.getShopId().toString() : null)
                .status(claim.getStatus())
                .issueDescription(claim.getIssueDescription())
                .images(imageList)
                .shopResponse(claim.getShopResponse())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                // Enriched fields
                .userName(userName)
                .userEmail(userEmail)
                .productName(productName)
                .productImage(productImage)
                .orderCode(orderCode)
                .build();
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            // Handle both formats: JSON array [...] and JSON object {...}
            String trimmed = imagesJson.trim();
            if (trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            }
            // If it's a JSON object or plain string, wrap it as a single-element list
            return Collections.singletonList(trimmed);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse images JSON for claim: {}", imagesJson, e);
            return Collections.emptyList();
        }
    }
}