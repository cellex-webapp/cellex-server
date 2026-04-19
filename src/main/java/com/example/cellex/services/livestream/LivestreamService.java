package com.example.cellex.services.livestream;

import com.example.cellex.dtos.request.livestream.AddProductToLiveBagRequest;
import com.example.cellex.dtos.request.livestream.CreateLivestreamRequest;
import com.example.cellex.dtos.response.livestream.LivestreamProductResponse;
import com.example.cellex.dtos.response.livestream.LivestreamSessionResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.jpa.LivestreamProductEntity;
import com.example.cellex.models.jpa.LivestreamSessionEntity;
import com.example.cellex.models.livestream.LivestreamStatus;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.jpa.LivestreamProductRepository;
import com.example.cellex.repositories.jpa.LivestreamSessionRepository;
import com.example.cellex.repositories.product.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LivestreamService {

    private final LivestreamSessionRepository sessionRepository;
    private final LivestreamProductRepository livestreamProductRepository;
    private final ProductRepository productRepository;
    private final ZegoTokenService zegoTokenService;

    @Transactional
    public LivestreamSessionResponse createSession(User vendor, CreateLivestreamRequest request) {
        String roomId = "ROOM_" + UUID.randomUUID().toString().substring(0, 8);

        LivestreamSessionEntity session = LivestreamSessionEntity.builder()
                .vendor(vendor)
                .title(request.getTitle())
                .thumbnail(request.getThumbnail())
                .status(LivestreamStatus.LIVE)
                .roomId(roomId)
                .startedAt(LocalDateTime.now())
                .build();

        sessionRepository.save(session);

        // Sinh token cho Host
        String token = zegoTokenService.generateToken(roomId, vendor.getId(), true);

        return mapToResponse(session, token);
    }

    @Transactional
    public void endSession(String sessionId, User vendor) {
        LivestreamSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.LIVE_SESSION_NOT_FOUND));

        if (!session.getVendor().getId().equals(vendor.getId())) {
            throw new AppException(ErrorCode.NOT_HOST_OF_SESSION);
        }

        session.setStatus(LivestreamStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<LivestreamSessionResponse> getActiveSessions() {
        return sessionRepository.findByStatus(LivestreamStatus.LIVE).stream()
                .map(session -> mapToResponse(session, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LivestreamProductResponse> getSessionProducts(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new AppException(ErrorCode.LIVE_SESSION_NOT_FOUND);
        }

        List<LivestreamProductEntity> livestreamProducts = livestreamProductRepository.findBySessionId(sessionId);

        Map<String, Product> productsById = livestreamProducts.isEmpty()
            ? Collections.emptyMap()
            : productRepository.findAllById(livestreamProducts.stream()
                    .map(LivestreamProductEntity::getProductId)
                    .distinct()
                    .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return livestreamProducts.stream()
            .map(entity -> mapToProductResponse(entity, productsById.get(entity.getProductId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addProductToBag(String sessionId, AddProductToLiveBagRequest request, User vendor) {
        LivestreamSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.LIVE_SESSION_NOT_FOUND));

        if (!session.getVendor().getId().equals(vendor.getId())) {
            throw new AppException(ErrorCode.NOT_HOST_OF_SESSION);
        }

        LivestreamProductEntity liveProduct = LivestreamProductEntity.builder()
                .session(session)
                .productId(request.getProductId())
                .flashSalePrice(request.getFlashSalePrice())
                .isPinned(false)
                .build();

        livestreamProductRepository.save(liveProduct);
    }

    // Lấy token cho Viewer khi họ bấm vào phòng
    public String getViewerToken(String sessionId, User viewer) {
        LivestreamSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.LIVE_SESSION_NOT_FOUND));
        return zegoTokenService.generateToken(session.getRoomId(), viewer.getId(), false);
    }

    private LivestreamSessionResponse mapToResponse(LivestreamSessionEntity entity, String token) {
        return LivestreamSessionResponse.builder()
                .id(entity.getId())
                .vendorId(entity.getVendor().getId())
                .vendorName(entity.getVendor().getFullName())
                .title(entity.getTitle())
                .thumbnail(entity.getThumbnail())
                .status(entity.getStatus())
                .roomId(entity.getRoomId())
                .zegoToken(token)
                .startedAt(entity.getStartedAt())
                .build();
    }

    private LivestreamProductResponse mapToProductResponse(LivestreamProductEntity entity, Product product) {
        LivestreamProductResponse.LivestreamProductResponseBuilder builder = LivestreamProductResponse.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .flashSalePrice(entity.getFlashSalePrice())
                .isPinned(entity.getIsPinned());

        if (product != null) {
            builder.shopId(product.getShopId())
                    .categoryId(product.getCategoryId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .images(product.getImages())
                    .price(product.getPrice())
                    .saleOff(product.getSaleOff())
                    .finalPrice(product.getFinalPrice())
                    .stockQuantity(product.getStockQuantity())
                    .averageRating(product.getAverageRating())
                    .reviewCount(product.getReviewCount())
                    .purchaseCount(product.getPurchaseCount())
                    .isPublished(product.getIsPublished())
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt());

            if (product.getAttributeValues() != null && !product.getAttributeValues().isEmpty()) {
                builder.attributeValues(product.getAttributeValues().stream()
                        .map(attribute -> LivestreamProductResponse.ProductAttributeValueResponse.builder()
                                .attributeId(attribute.getAttributeId())
                                .attributeKey(attribute.getAttributeKey())
                                .attributeName(attribute.getAttributeName())
                                .value(attribute.getValue())
                                .unit(attribute.getUnit())
                                .dataType(attribute.getDataType())
                                .build())
                        .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }
}