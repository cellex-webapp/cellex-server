package com.example.cellex.services.inventory;

import com.example.cellex.dtos.request.inventory.InventoryCheckBalanceRequest;
import com.example.cellex.dtos.request.inventory.InventoryImportRequest;
import com.example.cellex.dtos.response.inventory.InventoryCheckHistoryResponse;
import com.example.cellex.dtos.response.inventory.InventoryCheckResponse;
import com.example.cellex.dtos.response.inventory.InventoryImportHistoryResponse;
import com.example.cellex.dtos.response.inventory.InventoryImportResponse;
import com.example.cellex.dtos.response.inventory.ProductSkuSearchResponse;
import com.example.cellex.enums.InventoryCheckStatus;
import com.example.cellex.enums.InventoryTransactionType;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.inventory.*;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.product.ProductSku;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.shop.ShopStaffMember;
import com.example.cellex.repositories.inventory.*;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.product.ProductSkuRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.shop.ShopStaffMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryCheckRepository inventoryCheckRepository;
    private final InventoryCheckItemRepository inventoryCheckItemRepository;
    private final ShopRepository shopRepository;
    private final ShopStaffMemberRepository shopStaffMemberRepository;
    private final SupplierService supplierService;

    @Transactional
    public InventoryImportResponse importInventory(String userId, Role role, InventoryImportRequest request) {
        String shopId = resolveShopId(userId, role, request.getShopId(), true);

        Supplier supplier = supplierService.findAccessibleSupplier(userId, role, request.getSupplierId());
        if (!shopId.equals(supplier.getShopId())) {
            throw new AppException(ErrorCode.RESOURCE_NOT_OWNED, "Nha cung cap khong thuoc cua hang duoc chon");
        }

        String referenceId = generateReferenceId("IMP");
        LocalDateTime now = LocalDateTime.now();

        List<InventoryBatch> batches = new ArrayList<>();
        List<InventoryTransaction> transactions = new ArrayList<>();
        List<InventoryImportResponse.ItemResult> itemResults = new ArrayList<>();

        int totalQuantity = 0;
        double totalImportAmount = 0.0;

        for (InventoryImportRequest.Item item : request.getItems()) {
            ProductSku sku = findSkuInShop(item.getSkuId(), shopId);

            int quantity = item.getQuantity();
            double importPrice = item.getImportPrice();

            InventoryBatch batch = InventoryBatch.builder()
                    .skuUuid(UUID.fromString(sku.getId()))
                    .supplierUuid(UUID.fromString(supplier.getId()))
                    .importPriceDecimal(BigDecimal.valueOf(importPrice))
                    .quantity(quantity)
                    .remainQuantity(quantity)
                    .importDate(now)
                    .referenceId(referenceId)
                    .build();
            batches.add(batch);

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .skuUuid(UUID.fromString(sku.getId()))
                    .type(InventoryTransactionType.IMPORT)
                    .quantityChange(quantity)
                    .referenceId(referenceId)
                    .note(request.getNote())
                    .build();
            transactions.add(transaction);

            int nextOnHand = (sku.getOnHandStock() != null ? sku.getOnHandStock() : 0) + quantity;
            sku.setOnHandStock(nextOnHand);
            ProductSku savedSku = productSkuRepository.save(sku);

            totalQuantity += quantity;
            totalImportAmount += quantity * importPrice;

            itemResults.add(InventoryImportResponse.ItemResult.builder()
                    .skuId(savedSku.getId())
                    .skuCode(savedSku.getSkuCode())
                    .quantity(quantity)
                    .importPrice(importPrice)
                    .onHandStock(savedSku.getOnHandStock())
                    .reservedStock(savedSku.getReservedStock())
                    .availableStock(savedSku.getAvailableStock())
                    .build());
        }

        inventoryBatchRepository.saveAll(batches);
        inventoryTransactionRepository.saveAll(transactions);

        Supplier savedSupplier = supplierRepository.save(supplier);

        return InventoryImportResponse.builder()
                .referenceId(referenceId)
                .supplierId(savedSupplier.getId())
                .supplierName(savedSupplier.getSupplierName())
                .totalQuantity(totalQuantity)
                .totalImportAmount(totalImportAmount)
                .importedAt(now)
                .items(itemResults)
                .build();
    }

    @Transactional
    public InventoryCheckResponse balanceInventory(String userId, Role role, InventoryCheckBalanceRequest request) {
        String shopId = resolveShopId(userId, role, request.getShopId(), true);
        String referenceId = generateReferenceId("CHK");

        UUID createdBy;
        try {
            createdBy = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        InventoryCheck check = InventoryCheck.builder()
                .shopUuid(UUID.fromString(shopId))
                .status(InventoryCheckStatus.BALANCED)
                .createdByUuid(createdBy)
                .build();
        InventoryCheck savedCheck = inventoryCheckRepository.save(check);

        List<InventoryCheckItem> checkItems = new ArrayList<>();
        List<InventoryTransaction> transactions = new ArrayList<>();
        List<InventoryCheckResponse.ItemResult> itemResults = new ArrayList<>();

        int totalAdjustedQuantity = 0;

        for (InventoryCheckBalanceRequest.Item item : request.getItems()) {
            ProductSku sku = findSkuInShop(item.getSkuId(), shopId);

            int systemStock = sku.getOnHandStock() != null ? sku.getOnHandStock() : 0;
            int actualStock = item.getActualStock();
            int difference = actualStock - systemStock;

            sku.setOnHandStock(actualStock);
            ProductSku savedSku = productSkuRepository.save(sku);

            InventoryCheckItem checkItem = InventoryCheckItem.builder()
                    .checkUuid(UUID.fromString(savedCheck.getId()))
                    .skuUuid(UUID.fromString(savedSku.getId()))
                    .systemStock(systemStock)
                    .actualStock(actualStock)
                    .difference(difference)
                    .reason(item.getReason())
                    .build();
            checkItems.add(checkItem);

            if (difference != 0) {
                InventoryTransactionType txType = difference > 0
                        ? InventoryTransactionType.ADJUSTMENT_UP
                        : InventoryTransactionType.ADJUSTMENT_DOWN;

                InventoryTransaction tx = InventoryTransaction.builder()
                        .skuUuid(UUID.fromString(savedSku.getId()))
                        .type(txType)
                        .quantityChange(difference)
                        .referenceId(referenceId)
                        .note(item.getReason())
                        .build();
                transactions.add(tx);
                totalAdjustedQuantity += Math.abs(difference);
            }

            itemResults.add(InventoryCheckResponse.ItemResult.builder()
                    .skuId(savedSku.getId())
                    .skuCode(savedSku.getSkuCode())
                    .systemStock(systemStock)
                    .actualStock(actualStock)
                    .difference(difference)
                    .reason(item.getReason())
                    .onHandStock(savedSku.getOnHandStock())
                    .reservedStock(savedSku.getReservedStock())
                    .availableStock(savedSku.getAvailableStock())
                    .build());
        }

        inventoryCheckItemRepository.saveAll(checkItems);
        if (!transactions.isEmpty()) {
            inventoryTransactionRepository.saveAll(transactions);
        }

        return InventoryCheckResponse.builder()
                .checkId(savedCheck.getId())
                .shopId(shopId)
                .status(savedCheck.getStatus().name())
                .createdAt(savedCheck.getCreatedAt())
                .totalAdjustedQuantity(totalAdjustedQuantity)
                .items(itemResults)
                .build();
    }

    public List<ProductSkuSearchResponse> searchSkus(String userId, Role role, String requestedShopId, String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        int maxLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 50);
        String shopId = resolveShopId(userId, role, requestedShopId, role != Role.ADMIN);

        Map<String, ProductSku> resultMap = new LinkedHashMap<>();

        List<ProductSku> skuByCode = productSkuRepository.searchBySkuCode(keyword.trim(), shopId, org.springframework.data.domain.PageRequest.of(0, maxLimit));
        for (ProductSku sku : skuByCode) {
            resultMap.putIfAbsent(sku.getId(), sku);
        }

        List<Product> matchedProducts = productRepository.findByNameContainingIgnoreCase(keyword.trim());
        if (!matchedProducts.isEmpty() && resultMap.size() < maxLimit) {
            List<String> productIds = matchedProducts.stream().map(Product::getId).toList();
            List<ProductSku> skuByProductName = productSkuRepository.findByProductIdInAndIsActiveTrue(productIds);
            for (ProductSku sku : skuByProductName) {
                if (shopId == null || shopId.equals(sku.getShopId())) {
                    resultMap.putIfAbsent(sku.getId(), sku);
                }
                if (resultMap.size() >= maxLimit) {
                    break;
                }
            }
        }

        List<ProductSku> skus = resultMap.values().stream().limit(maxLimit).toList();
        Map<String, Product> productMap = productRepository.findAllById(
                        skus.stream().map(ProductSku::getProductId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return skus.stream().map(sku -> {
            Product product = productMap.get(sku.getProductId());
            String productImage = (product != null && product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0)
                    : null;

            return ProductSkuSearchResponse.builder()
                    .skuId(sku.getId())
                    .skuCode(sku.getSkuCode())
                    .productId(sku.getProductId())
                    .productName(product != null ? product.getName() : null)
                    .productImage(productImage)
                    .price(sku.getPrice())
                    .onHandStock(sku.getOnHandStock())
                    .reservedStock(sku.getReservedStock())
                    .availableStock(sku.getAvailableStock())
                    .safetyStock(sku.getSafetyStock())
                    .variationData(sku.getVariationData())
                    .build();
        }).toList();
    }

        public List<InventoryImportHistoryResponse> getImportHistory(String userId, Role role, String requestedShopId, Integer limit) {
        int maxLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        String shopId = resolveShopId(userId, role, requestedShopId, false);

        List<InventoryBatch> batches = inventoryBatchRepository.findAllByOrderByImportDateDesc();
        if (batches.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<InventoryBatch>> grouped = batches.stream()
            .filter(batch -> batch.getReferenceId() != null)
            .filter(batch -> shopId == null || shopId.equals(resolveSkuShopId(batch.getSkuUuid())))
            .collect(Collectors.groupingBy(InventoryBatch::getReferenceId, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
            .limit(maxLimit)
            .map(entry -> {
                List<InventoryBatch> group = entry.getValue();
                InventoryBatch first = group.get(0);
                Supplier supplier = supplierRepository.findById(first.getSupplierUuid())
                    .orElse(null);

                int totalQuantity = group.stream().mapToInt(InventoryBatch::getQuantity).sum();
                double totalImportAmount = group.stream()
                    .mapToDouble(batch -> batch.getQuantity() * batch.getImportPrice())
                    .sum();

                return InventoryImportHistoryResponse.builder()
                    .referenceId(first.getReferenceId())
                    .supplierId(first.getSupplierId())
                    .supplierName(supplier != null ? supplier.getSupplierName() : null)
                    .totalQuantity(totalQuantity)
                    .totalImportAmount(totalImportAmount)
                    .importedAt(first.getImportDate())
                    .build();
            })
            .toList();
        }

        public List<InventoryCheckHistoryResponse> getCheckHistory(String userId, Role role, String requestedShopId, Integer limit) {
        int maxLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        String shopId = resolveShopId(userId, role, requestedShopId, false);

        List<InventoryCheck> checks = shopId != null
            ? inventoryCheckRepository.findByShopUuidOrderByCreatedAtDesc(UUID.fromString(shopId), org.springframework.data.domain.PageRequest.of(0, maxLimit)).getContent()
            : inventoryCheckRepository.findAllByOrderByCreatedAtDesc().stream().limit(maxLimit).toList();

        return checks.stream()
            .limit(maxLimit)
            .map(check -> {
                UUID checkUuid = UUID.fromString(check.getId());
                List<InventoryCheckItem> items = inventoryCheckItemRepository.findByCheckUuid(checkUuid);
                return InventoryCheckHistoryResponse.builder()
                    .checkCode(buildInventoryCheckCode(check))
                    .shopId(check.getShopId())
                    .status(check.getStatus() != null ? check.getStatus().name() : null)
                    .createdAt(check.getCreatedAt())
                    .totalAdjustedQuantity(items.stream()
                        .mapToInt(item -> Math.abs(item.getDifference() != null ? item.getDifference() : 0))
                        .sum())
                    .reason(items.stream()
                        .map(InventoryCheckItem::getReason)
                        .filter(reason -> reason != null && !reason.isBlank())
                        .distinct()
                        .collect(Collectors.joining(" | ")))
                    .build();
            })
            .toList();
        }

    public List<ProductSku> findLowStockSkus() {
        return productSkuRepository.findLowStockSkus();
    }

    private ProductSku findSkuInShop(String skuId, String shopId) {
        ProductSku sku = productSkuRepository.findActiveById(skuId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "SKU khong ton tai"));

        if (!shopId.equals(sku.getShopId())) {
            throw new AppException(ErrorCode.RESOURCE_NOT_OWNED, "SKU khong thuoc cua hang duoc chon");
        }

        return sku;
    }

    private String resolveShopId(String userId, Role role, String requestedShopId, boolean requireShop) {
        if (role == Role.ADMIN) {
            if (requestedShopId != null && !requestedShopId.isBlank()) {
                shopRepository.findById(requestedShopId)
                        .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
                return requestedShopId;
            }
            if (requireShop) {
                throw new AppException(ErrorCode.FIELD_REQUIRED, "shopId la bat buoc voi ADMIN");
            }
            return null;
        }

        Shop shop = shopRepository.findByVendorId(userId)
                .orElse(null);
        if (shop != null) return shop.getId();
        return shopStaffMemberRepository.findByUserUuidAndIsActiveTrue(UUID.fromString(userId))
                .map(ShopStaffMember::getShopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
    }

    private String generateReferenceId(String prefix) {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return prefix + "-" + timePart + "-" + randomPart;
    }

    private String resolveSkuShopId(UUID skuUuid) {
        return productSkuRepository.findById(skuUuid)
                .map(ProductSku::getShopId)
                .orElse(null);
    }

    private String buildInventoryCheckCode(InventoryCheck check) {
        LocalDateTime createdAt = check.getCreatedAt() != null ? check.getCreatedAt() : LocalDateTime.now();
        String timePart = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = check.getId() != null && check.getId().length() >= 8
                ? check.getId().substring(0, 8).toUpperCase(Locale.ROOT)
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "CHK-" + timePart + "-" + suffix;
    }
}
