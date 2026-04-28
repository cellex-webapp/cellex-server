package com.example.cellex.services.product;

import com.example.cellex.dtos.request.product.ProductSkuRequest;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.product.ProductSku;
import com.example.cellex.repositories.product.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSkuService {

    private final ProductSkuRepository productSkuRepository;

    public Optional<ProductSku> findActiveById(String skuId) {
        return productSkuRepository.findActiveById(skuId);
    }

    public List<ProductSku> getActiveSkusByProductId(String productId) {
        return productSkuRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtAsc(productId);
    }

    public List<ProductSku> getActiveSkusByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productSkuRepository.findByProductIdInAndIsActiveTrue(productIds);
    }

    public Integer sumAvailableStockByProduct(String productId) {
        return getActiveSkusByProductId(productId).stream()
                .mapToInt(ProductSku::getAvailableStock)
                .sum();
    }

    public List<ProductSku> searchBySkuCode(String keyword, String shopId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return productSkuRepository.searchBySkuCode(keyword, shopId, pageable);
    }

    @Transactional
    public List<ProductSku> replaceSkusForProduct(String productId, String shopId, List<ProductSkuRequest> skuRequests) {
        if (productId == null || productId.isBlank() || shopId == null || shopId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "productId/shopId khong hop le");
        }

        List<ProductSku> existingSkus = productSkuRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtAsc(productId);

        if (skuRequests == null) {
            return existingSkus;
        }

        if (skuRequests.isEmpty()) {
            existingSkus.forEach(sku -> sku.setIsActive(false));
            productSkuRepository.saveAll(existingSkus);
            return Collections.emptyList();
        }

        UUID shopUuid;
        try {
            shopUuid = UUID.fromString(shopId);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_INPUT, "shopId khong hop le");
        }

        Set<String> skuCodeSet = new HashSet<>();
        Set<String> variationHashSet = new HashSet<>();

        List<ProductSku> toSave = new ArrayList<>();
        Map<String, ProductSku> existingSkuMap = existingSkus.stream()
                .collect(Collectors.toMap(s -> s.getSkuCode().toLowerCase(), s -> s));

        for (ProductSkuRequest raw : skuRequests) {
            Map<String, String> variationData = normalizeVariationData(raw.getVariationData());
            String variationHash = computeVariationHash(variationData);
            String skuCode = normalizeSkuCode(raw.getSkuCode(), variationHash);

            if (!skuCodeSet.add(skuCode.toLowerCase())) {
                throw new AppException(ErrorCode.DUPLICATE_VALUE, "SKU code bi trung trong danh sach");
            }
            if (!variationHashSet.add(variationHash)) {
                throw new AppException(ErrorCode.DUPLICATE_VALUE, "To hop bien the bi trung trong danh sach SKU");
            }

            Integer onHand = raw.getOnHandStock() != null ? raw.getOnHandStock() : 0;
            Integer reserved = raw.getReservedStock() != null ? raw.getReservedStock() : 0;
            Integer safety = raw.getSafetyStock() != null ? raw.getSafetyStock() : 0;

            if (onHand < 0 || reserved < 0 || safety < 0) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Gia tri ton kho khong hop le");
            }
            if (reserved > onHand) {
                throw new AppException(ErrorCode.INVALID_INPUT, "reservedStock khong the lon hon onHandStock");
            }
            if (raw.getPrice() == null || raw.getPrice() <= 0) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Gia SKU phai lon hon 0");
            }

            ProductSku existing = existingSkuMap.get(skuCode.toLowerCase());
            if (existing != null) {
                existing.setVariationData(variationData);
                existing.setVariationHash(variationHash);
                existing.setImageUrl(raw.getImageUrl());
                existing.setPriceDecimal(BigDecimal.valueOf(raw.getPrice()));
                existing.setOnHandStock(onHand);
                existing.setReservedStock(reserved);
                existing.setSafetyStock(safety);
                existing.setIsActive(raw.getIsActive() == null || raw.getIsActive());
                toSave.add(existing);
                existingSkuMap.remove(skuCode.toLowerCase());
            } else {
                productSkuRepository.findBySkuCodeIgnoreCaseAndIsActiveTrue(skuCode)
                        .ifPresent(e -> {
                            if (!productId.equals(e.getProductId())) {
                                throw new AppException(ErrorCode.DUPLICATE_VALUE, "SKU code da ton tai: " + skuCode);
                            }
                        });

                toSave.add(ProductSku.builder()
                        .productId(productId)
                        .shopUuid(shopUuid)
                        .skuCode(skuCode)
                        .variationData(variationData)
                        .variationHash(variationHash)
                        .imageUrl(raw.getImageUrl())
                        .priceDecimal(BigDecimal.valueOf(raw.getPrice()))
                        .onHandStock(onHand)
                        .reservedStock(reserved)
                        .safetyStock(safety)
                        .isActive(raw.getIsActive() == null || raw.getIsActive())
                        .build());
            }
        }

        for (ProductSku notInRequest : existingSkuMap.values()) {
            notInRequest.setIsActive(false);
            toSave.add(notInRequest);
        }

        List<ProductSku> saved = productSkuRepository.saveAll(toSave);
        log.info("Replaced/Updated {} SKUs for product {}", saved.size(), productId);
        return saved.stream().filter(ProductSku::getIsActive).collect(Collectors.toList());
    }

    @Transactional
    public ProductSku reserveStock(String skuId, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        ProductSku sku = productSkuRepository.findActiveByIdForUpdate(skuId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "SKU khong ton tai"));

        int available = sku.getAvailableStock();
        if (available < quantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK, "So luong SKU khong du");
        }

        sku.setReservedStock((sku.getReservedStock() != null ? sku.getReservedStock() : 0) + quantity);
        return productSkuRepository.save(sku);
    }

    @Transactional
    public ProductSku releaseReservedStock(String skuId, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        ProductSku sku = productSkuRepository.findActiveByIdForUpdate(skuId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "SKU khong ton tai"));

        int currentReserved = sku.getReservedStock() != null ? sku.getReservedStock() : 0;
        sku.setReservedStock(Math.max(0, currentReserved - quantity));
        return productSkuRepository.save(sku);
    }

    @Transactional
    public ProductSku consumeReservedStock(String skuId, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        ProductSku sku = productSkuRepository.findActiveByIdForUpdate(skuId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "SKU khong ton tai"));

        int currentReserved = sku.getReservedStock() != null ? sku.getReservedStock() : 0;
        int onHand = sku.getOnHandStock() != null ? sku.getOnHandStock() : 0;

        if (currentReserved < quantity) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "So luong reserved khong du de tru");
        }
        if (onHand < quantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK, "So luong on-hand khong du");
        }

        sku.setReservedStock(currentReserved - quantity);
        sku.setOnHandStock(onHand - quantity);
        return productSkuRepository.save(sku);
    }

    private String normalizeSkuCode(String skuCode, String variationHash) {
        if (skuCode == null || skuCode.isBlank()) {
            return "SKU-" + variationHash.substring(0, 8).toUpperCase(Locale.ROOT);
        }
        return skuCode.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, String> normalizeVariationData(Map<String, String> variationData) {
        if (variationData == null || variationData.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "variationData khong duoc de trong");
        }

        Map<String, String> normalized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        variationData.forEach((k, v) -> {
            if (k == null || k.isBlank() || v == null || v.isBlank()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "variationData khong hop le");
            }
            normalized.put(k.trim(), v.trim());
        });

        Map<String, String> ordered = new LinkedHashMap<>();
        normalized.forEach(ordered::put);
        return ordered;
    }

    private String computeVariationHash(Map<String, String> variationData) {
        String canonical = variationData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> entry.getKey().trim().toLowerCase(Locale.ROOT) + "=" + entry.getValue().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("|"));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Khong the tinh variation hash");
        }
    }
}
