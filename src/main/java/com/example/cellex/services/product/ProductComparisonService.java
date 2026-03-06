package com.example.cellex.services.product;

import com.example.cellex.dtos.response.product.ProductComparisonResponse;
import com.example.cellex.dtos.response.product.ProductComparisonResponse.ComparisonRow;
import com.example.cellex.dtos.response.product.ProductComparisonResponse.PriceSummary;
import com.example.cellex.dtos.response.product.ProductComparisonResponse.ProductSummary;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.category.CategoryAttribute;
import com.example.cellex.models.product.Product;
import com.example.cellex.repositories.category.CategoryAttributeRepository;
import com.example.cellex.repositories.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductComparisonService {

    private final ProductRepository productRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;

    private static final int MAX_COMPARE_PRODUCTS = 4;
    private static final int MIN_COMPARE_PRODUCTS = 2;
    private static final String VALUE_NOT_AVAILABLE = "—";

    public ProductComparisonResponse getComparison(List<String> productIds) {
        // 1. Validate số lượng sản phẩm
        if (productIds == null || productIds.size() < MIN_COMPARE_PRODUCTS) {
            throw new AppException(ErrorCode.COMPARISON_MINIMUM_REQUIRED);
        }
        if (productIds.size() > MAX_COMPARE_PRODUCTS) {
            throw new AppException(ErrorCode.COMPARISON_LIMIT_EXCEEDED);
        }

        // 2. Lấy danh sách sản phẩm
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 3. Kiểm tra tất cả sản phẩm cùng danh mục
        String categoryId = products.get(0).getCategoryId();
        boolean allSameCategory = products.stream()
                .allMatch(p -> categoryId.equals(p.getCategoryId()));
        if (!allSameCategory) {
            throw new AppException(ErrorCode.CATEGORY_MISMATCH);
        }

        // 4. Lấy danh sách thuộc tính chính thức từ Category (đã sắp xếp theo sortOrder)
        List<CategoryAttribute> categoryAttributes =
                categoryAttributeRepository.findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(categoryId);

        // 5. Xây dựng danh sách ProductSummary
        List<ProductSummary> productSummaries = products.stream()
                .map(this::mapToProductSummary)
                .collect(Collectors.toList());

        // 6. Xây dựng bảng so sánh thông số kỹ thuật
        List<ComparisonRow> technicalSpecs = buildComparisonRows(categoryAttributes, products);

        // 7. Tính toán Price Summary
        PriceSummary priceSummary = buildPriceSummary(products);

        return ProductComparisonResponse.builder()
                .products(productSummaries)
                .technicalSpecs(technicalSpecs)
                .priceSummary(priceSummary)
                .build();
    }

    private ProductSummary mapToProductSummary(Product product) {
        String firstImage = (product.getImages() != null && !product.getImages().isEmpty())
                ? product.getImages().get(0) : null;

        double savedAmount = 0.0;
        if (product.getPrice() != null && product.getFinalPrice() != null) {
            savedAmount = product.getPrice() - product.getFinalPrice();
        }

        return ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .image(firstImage)
                .price(product.getPrice())
                .finalPrice(product.getFinalPrice())
                .saleOff(product.getSaleOff())
                .averageRating(product.getAverageRating())
                .savedAmount(savedAmount)
                .build();
    }

    private List<ComparisonRow> buildComparisonRows(
            List<CategoryAttribute> categoryAttributes, List<Product> products) {

        List<ComparisonRow> rows = new ArrayList<>();

        for (CategoryAttribute catAttr : categoryAttributes) {
            Map<String, String> valuesMap = new LinkedHashMap<>();

            for (Product product : products) {
                String displayValue = findAttributeDisplayValue(product, catAttr);
                valuesMap.put(product.getId(), displayValue);
            }

            // Xác định isDifferent: kiểm tra nếu các giá trị đều giống nhau
            Collection<String> allValues = valuesMap.values();
            Set<String> uniqueValues = new HashSet<>(allValues);
            // Loại bỏ giá trị N/A khi so sánh sự khác biệt
            uniqueValues.remove(VALUE_NOT_AVAILABLE);
            boolean isDifferent = uniqueValues.size() > 1;

            // Tìm bestProductId cho kiểu NUMBER
            String bestProductId = null;
            if ("NUMBER".equalsIgnoreCase(catAttr.getDataType()) && isDifferent) {
                bestProductId = findBestNumericProduct(products, catAttr);
            }

            rows.add(ComparisonRow.builder()
                    .attributeName(catAttr.getAttributeName())
                    .dataType(catAttr.getDataType())
                    .sortOrder(catAttr.getSortOrder())
                    .isHighlight(catAttr.getIsHighlight())
                    .values(valuesMap)
                    .isDifferent(isDifferent)
                    .bestProductId(bestProductId)
                    .build());
        }

        return rows;
    }

    /**
     * Tìm giá trị hiển thị của thuộc tính cho một sản phẩm.
     * Kết hợp value + unit thành chuỗi hiển thị (ví dụ: "8" + "GB" = "8 GB").
     */
    private String findAttributeDisplayValue(Product product, CategoryAttribute catAttr) {
        if (product.getAttributeValues() == null) {
            return VALUE_NOT_AVAILABLE;
        }

        return product.getAttributeValues().stream()
                .filter(av -> catAttr.getId().equals(av.getAttributeId())
                        || catAttr.getAttributeKey().equals(av.getAttributeKey()))
                .findFirst()
                .map(av -> {
                    String value = av.getValue();
                    if (value == null || value.isBlank()) {
                        return VALUE_NOT_AVAILABLE;
                    }
                    // Ưu tiên unit từ CategoryAttribute, fallback về unit trong ProductAttributeValue
                    String unit = catAttr.getUnit() != null ? catAttr.getUnit() : av.getUnit();
                    if (unit != null && !unit.isBlank()) {
                        return value + " " + unit;
                    }
                    return value;
                })
                .orElse(VALUE_NOT_AVAILABLE);
    }

    /**
     * Tìm sản phẩm có giá trị số lớn nhất cho thuộc tính NUMBER (Best in class).
     */
    private String findBestNumericProduct(List<Product> products, CategoryAttribute catAttr) {
        String bestId = null;
        double bestValue = Double.MIN_VALUE;

        for (Product product : products) {
            if (product.getAttributeValues() == null) continue;

            Optional<Product.ProductAttributeValue> attrOpt = product.getAttributeValues().stream()
                    .filter(av -> catAttr.getId().equals(av.getAttributeId())
                            || catAttr.getAttributeKey().equals(av.getAttributeKey()))
                    .findFirst();

            if (attrOpt.isPresent()) {
                try {
                    double numericValue = Double.parseDouble(attrOpt.get().getValue());
                    if (numericValue > bestValue) {
                        bestValue = numericValue;
                        bestId = product.getId();
                    }
                } catch (NumberFormatException ignored) {
                    // Giá trị không parse được → bỏ qua
                }
            }
        }

        return bestId;
    }

    /**
     * Tính toán thông tin giá: sản phẩm rẻ nhất và tiết kiệm nhiều nhất.
     */
    private PriceSummary buildPriceSummary(List<Product> products) {
        String lowestPriceId = null;
        double lowestPrice = Double.MAX_VALUE;
        String highestSavingsId = null;
        double highestSavings = 0.0;

        for (Product product : products) {
            double finalPrice = product.getFinalPrice() != null ? product.getFinalPrice() : Double.MAX_VALUE;
            if (finalPrice < lowestPrice) {
                lowestPrice = finalPrice;
                lowestPriceId = product.getId();
            }

            double savings = 0.0;
            if (product.getPrice() != null && product.getFinalPrice() != null) {
                savings = product.getPrice() - product.getFinalPrice();
            }
            if (savings > highestSavings) {
                highestSavings = savings;
                highestSavingsId = product.getId();
            }
        }

        return PriceSummary.builder()
                .lowestPriceProductId(lowestPriceId)
                .lowestFinalPrice(lowestPrice == Double.MAX_VALUE ? null : lowestPrice)
                .highestSavingsProductId(highestSavingsId)
                .highestSavingsAmount(highestSavings)
                .build();
    }
}
