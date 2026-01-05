package com.example.cellex.services.ai;

import com.example.cellex.dtos.response.ai.AIChatResponse;
import com.example.cellex.dtos.response.ai.AIChatResponse.*;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.models.category.Category;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.recommendation.ProductSimilarity;
import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.models.segment.CustomerSegment;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.coupon.CouponCampaignRepository;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.recommendation.ProductSimilarityRepository;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import com.example.cellex.repositories.segment.CustomerSegmentRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service chứa các AI Tools (Functions) để truy vấn database
 * Được sử dụng bởi Gemini Function Calling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIToolsService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final ProductSimilarityRepository productSimilarityRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final CouponCampaignRepository couponCampaignRepository;
    private final UserRepository userRepository;

    // ==================== USER TOOLS ====================

    /**
     * Tìm kiếm sản phẩm theo keyword, category, và price range
     */
    public Map<String, Object> searchProducts(String keyword, String categoryId, 
                                               Double minPrice, Double maxPrice, 
                                               Integer limit) {
        log.info("AI Tool: searchProducts - keyword={}, categoryId={}, minPrice={}, maxPrice={}, limit={}", 
                keyword, categoryId, minPrice, maxPrice, limit);
        
        List<Product> products;
        int resultLimit = limit != null ? limit : 10;
        
        // Nếu có keyword, thử tìm category trước
        String effectiveCategoryId = categoryId;
        if (keyword != null && !keyword.isEmpty() && effectiveCategoryId == null) {
            // Tìm category theo tên (case-insensitive)
            Optional<Category> matchedCategory = categoryRepository.findAll().stream()
                .filter(cat -> cat.getName().toLowerCase().contains(keyword.toLowerCase()) 
                            || keyword.toLowerCase().contains(cat.getName().toLowerCase()))
                .findFirst();
            
            if (matchedCategory.isPresent()) {
                effectiveCategoryId = matchedCategory.get().getId();
                log.info("AI Tool: searchProducts - Found category '{}' matching keyword '{}'", 
                        matchedCategory.get().getName(), keyword);
            }
        }
        
        // Tìm kiếm sản phẩm
        if (effectiveCategoryId != null && !effectiveCategoryId.isEmpty()) {
            // Ưu tiên tìm theo categoryId
            products = productRepository.findByCategoryIdAndIsPublishedTrue(
                effectiveCategoryId, PageRequest.of(0, resultLimit)).getContent();
        } else if (keyword != null && !keyword.isEmpty()) {
            // Tìm theo tên sản phẩm
            products = productRepository.findByNameContainingIgnoreCaseAndIsPublishedTrue(
                keyword, PageRequest.of(0, resultLimit)).getContent();
        } else {
            // Không có filter
            products = productRepository.findAllByIsPublishedTrue(PageRequest.of(0, resultLimit));
        }
        
        // Filter theo price range
        if (minPrice != null || maxPrice != null) {
            products = products.stream()
                .filter(p -> {
                    double price = p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice();
                    boolean matchMin = minPrice == null || price >= minPrice;
                    boolean matchMax = maxPrice == null || price <= maxPrice;
                    return matchMin && matchMax;
                })
                .collect(Collectors.toList());
        }
        
        List<String> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        List<Map<String, Object>> productDetails = products.stream().map(this::mapProductToSimpleInfo).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("productIds", productIds);
        result.put("products", productDetails);
        result.put("totalFound", products.size());
        
        return result;
    }

    /**
     * Lấy chi tiết sản phẩm bao gồm thuộc tính kỹ thuật
     */
    public Map<String, Object> getProductDetails(String productId) {
        log.info("AI Tool: getProductDetails - productId={}", productId);
        
        return productRepository.findById(productId)
            .map(product -> {
                Map<String, Object> details = new HashMap<>();
                // Không trả id - chỉ dùng để tìm kiếm, không hiển trong text
                details.put("name", product.getName());
                details.put("description", product.getDescription());
                details.put("price", product.getPrice());
                details.put("finalPrice", product.getFinalPrice());
                details.put("saleOff", product.getSaleOff());
                details.put("stockQuantity", product.getStockQuantity());
                details.put("averageRating", product.getAverageRating());
                details.put("reviewCount", product.getReviewCount());
                details.put("purchaseCount", product.getPurchaseCount());
                
                // Map attributes
                if (product.getAttributeValues() != null) {
                    List<Map<String, String>> attributes = product.getAttributeValues().stream()
                        .map(attr -> {
                            Map<String, String> attrMap = new HashMap<>();
                            attrMap.put("name", attr.getAttributeName());
                            attrMap.put("value", attr.getValue());
                            attrMap.put("unit", attr.getUnit());
                            return attrMap;
                        })
                        .collect(Collectors.toList());
                    details.put("attributes", attributes);
                }
                
                // Get category name
                if (product.getCategoryId() != null) {
                    categoryRepository.findById(product.getCategoryId())
                        .ifPresent(cat -> details.put("categoryName", cat.getName()));
                }
                
                return details;
            })
            .orElse(Map.of("error", "Product not found"));
    }

    /**
     * So sánh nhiều sản phẩm
     */
    public Map<String, Object> compareProducts(List<String> productIds) {
        log.info("AI Tool: compareProducts - productIds={}", productIds);
        
        List<Product> products = productRepository.findAllById(productIds);
        
        List<Map<String, Object>> comparison = products.stream()
            .map(this::mapProductToDetailedInfo)
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("products", comparison);
        result.put("comparedCount", products.size());
        
        return result;
    }

    /**
     * Gợi ý sản phẩm liên quan dựa trên ProductSimilarity
     */
    public Map<String, Object> getSimilarProducts(String productId, Integer limit) {
        log.info("AI Tool: getSimilarProducts - productId={}, limit={}", productId, limit);
        
        int resultLimit = limit != null ? limit : 5;
        
        List<ProductSimilarity> similarities = productSimilarityRepository
            .findByProductIdOrderBySimilarityScoreDesc(productId);
        
        List<String> similarIds = similarities.stream()
            .limit(resultLimit)
            .map(ProductSimilarity::getSimilarProductId)
            .collect(Collectors.toList());
        
        List<Product> similarProducts = productRepository.findAllById(similarIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("productIds", similarIds);
        result.put("products", similarProducts.stream().map(this::mapProductToSimpleInfo).collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Tìm sản phẩm theo attribute (ví dụ: pin lớn nhất, RAM cao nhất, hoặc RAM = 12GB)
     */
    public Map<String, Object> searchByAttribute(String keyword, String attributeKey, 
                                                  String attributeValue, String sortOrder, Integer limit) {
        log.info("AI Tool: searchByAttribute - keyword={}, attributeKey={}, attributeValue={}, sortOrder={}, limit={}", 
                keyword, attributeKey, attributeValue, sortOrder, limit);
        
        int resultLimit = limit != null ? limit : 10;
        String order = sortOrder != null ? sortOrder.toLowerCase() : "desc";
        
        // Tìm category từ keyword nếu có
        String categoryId = null;
        if (keyword != null && !keyword.isEmpty()) {
            Optional<Category> matchedCategory = categoryRepository.findAll().stream()
                .filter(cat -> cat.getName().toLowerCase().contains(keyword.toLowerCase()) 
                            || keyword.toLowerCase().contains(cat.getName().toLowerCase()))
                .findFirst();
            if (matchedCategory.isPresent()) {
                categoryId = matchedCategory.get().getId();
            }
        }
        
        // Lấy sản phẩm
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndIsPublishedTrue(
                categoryId, PageRequest.of(0, 100)).getContent();
        } else if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndIsPublishedTrue(
                keyword, PageRequest.of(0, 100)).getContent();
        } else {
            products = productRepository.findAllByIsPublishedTrue(PageRequest.of(0, 100));
        }
        
        // Filter và sort theo attribute
        if (attributeKey != null && !attributeKey.isEmpty()) {
            products = products.stream()
                .filter(p -> p.getAttributeValues() != null && p.getAttributeValues().stream()
                    .anyMatch(attr -> attr.getAttributeKey() != null && 
                                    attr.getAttributeKey().equalsIgnoreCase(attributeKey)))
                .collect(Collectors.toList());
            
            // Nếu có attributeValue, filter theo giá trị cụ thể
            if (attributeValue != null && !attributeValue.isEmpty()) {
                final String targetValue = attributeValue.toLowerCase().trim();
                products = products.stream()
                    .filter(p -> {
                        Double attrVal = getAttributeNumericValue(p, attributeKey);
                        if (attrVal != null) {
                            try {
                                // Nếu attributeValue là số, so sánh bằng
                                Double targetVal = Double.parseDouble(targetValue);
                                return Math.abs(attrVal - targetVal) < 0.01; // Tolerance for floating point
                            } catch (NumberFormatException e) {
                                // Nếu không phải số, so sánh chuỗi
                                return p.getAttributeValues().stream()
                                    .anyMatch(attr -> attr.getAttributeKey().equalsIgnoreCase(attributeKey) &&
                                                    attr.getValue().toLowerCase().contains(targetValue));
                            }
                        }
                        return false;
                    })
                    .limit(resultLimit)
                    .collect(Collectors.toList());
            } else {
                // Không có attributeValue, sort theo giá trị
                products = products.stream()
                    .sorted((p1, p2) -> {
                        Double val1 = getAttributeNumericValue(p1, attributeKey);
                        Double val2 = getAttributeNumericValue(p2, attributeKey);
                        if (val1 == null) return 1;
                        if (val2 == null) return -1;
                        return order.equals("asc") ? val1.compareTo(val2) : val2.compareTo(val1);
                    })
                    .limit(resultLimit)
                    .collect(Collectors.toList());
            }
        } else {
            products = products.stream().limit(resultLimit).collect(Collectors.toList());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("productIds", products.stream().map(Product::getId).collect(Collectors.toList()));
        result.put("products", products.stream().map(this::mapProductToSimpleInfo).collect(Collectors.toList()));
        result.put("totalFound", products.size());
        result.put("sortedBy", attributeKey);
        result.put("attributeValue", attributeValue);
        result.put("sortOrder", order);
        
        return result;
    }
    
    private Double getAttributeNumericValue(Product product, String attributeKey) {
        return product.getAttributeValues().stream()
            .filter(attr -> attr.getAttributeKey() != null && 
                          attr.getAttributeKey().equalsIgnoreCase(attributeKey))
            .findFirst()
            .map(attr -> {
                try {
                    return Double.parseDouble(attr.getValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            })
            .orElse(null);
    }

    /**
     * Lấy sản phẩm hot dựa trên UserInteraction
     */
    public Map<String, Object> getHotProducts(String categoryId, Integer limit) {
        log.info("AI Tool: getHotProducts - categoryId={}, limit={}", categoryId, limit);
        
        int resultLimit = limit != null ? limit : 10;
        
        // Lấy top products theo purchaseCount
        List<Product> products;
        if (categoryId != null && !categoryId.isEmpty()) {
            products = productRepository.findByCategoryIdAndIsPublishedTrue(
                categoryId, PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "purchaseCount")))
                .getContent();
        } else {
            products = productRepository.findAllByIsPublishedTrue(
                PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "purchaseCount")));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("productIds", products.stream().map(Product::getId).collect(Collectors.toList()));
        result.put("products", products.stream().map(this::mapProductToSimpleInfo).collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Lấy gợi ý cá nhân hóa cho user dựa trên UserInteraction
     */
    public Map<String, Object> getPersonalizedRecommendations(String userId, Integer limit) {
        log.info("AI Tool: getPersonalizedRecommendations - userId={}, limit={}", userId, limit);
        
        int resultLimit = limit != null ? limit : 10;
        
        List<UserInteraction> interactions = userInteractionRepository
            .findByUserIdOrderByTotalScoreDesc(userId);
        
        // Lấy categories user quan tâm nhất
        Map<String, Long> categoryScores = interactions.stream()
            .filter(i -> i.getCategoryId() != null)
            .collect(Collectors.groupingBy(
                UserInteraction::getCategoryId,
                Collectors.summingLong(i -> i.getTotalScore().longValue())
            ));
        
        // Lấy sản phẩm từ các category yêu thích
        Set<String> viewedProductIds = interactions.stream()
            .map(UserInteraction::getProductId)
            .collect(Collectors.toSet());
        
        List<Product> recommendations = new ArrayList<>();
        for (String catId : categoryScores.keySet()) {
            List<Product> catProducts = productRepository
                .findByCategoryIdAndIsPublishedTrue(catId, PageRequest.of(0, 5))
                .getContent();
            
            // Loại bỏ sản phẩm đã xem
            catProducts.stream()
                .filter(p -> !viewedProductIds.contains(p.getId()))
                .limit(3)
                .forEach(recommendations::add);
        }
        
        recommendations = recommendations.stream().limit(resultLimit).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("productIds", recommendations.stream().map(Product::getId).collect(Collectors.toList()));
        result.put("products", recommendations.stream().map(this::mapProductToSimpleInfo).collect(Collectors.toList()));
        result.put("basedOnCategories", categoryScores.keySet());
        
        return result;
    }

    /**
     * Lấy danh sách categories
     */
    public Map<String, Object> getCategories() {
        log.info("AI Tool: getCategories");
        
        List<Category> categories = categoryRepository.findByIsActiveTrue();
        
        List<Map<String, Object>> categoryList = categories.stream()
            .map(cat -> {
                Map<String, Object> catMap = new HashMap<>();
                // Không trả id - chỉ cần tên danh mục trong text
                catMap.put("name", cat.getName());
                catMap.put("description", cat.getDescription());
                return catMap;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categoryList);
        result.put("totalCount", categories.size());
        
        return result;
    }

    // ==================== VENDOR TOOLS ====================

    /**
     * Lấy thống kê doanh thu của shop
     */
    public Map<String, Object> getRevenueStats(String shopId, String startDate, String endDate) {
        log.info("AI Tool: getRevenueStats - shopId={}, startDate={}, endDate={}", shopId, startDate, endDate);
        
        LocalDateTime start = startDate != null ? 
            LocalDateTime.parse(startDate + "T00:00:00") : 
            LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? 
            LocalDateTime.parse(endDate + "T23:59:59") : 
            LocalDateTime.now();
        
        List<Order> completedOrders = orderRepository
            .findCompletedPaidOrdersByShopIdAndDateRange(shopId, start, end);
        
        double totalRevenue = completedOrders.stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        
        long totalOrders = completedOrders.size();
        double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
        
        // Doanh thu theo ngày
        Map<String, Double> revenueByDay = completedOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getCreatedAt().toLocalDate().toString(),
                Collectors.summingDouble(Order::getTotalAmount)
            ));
        
        // Sắp xếp theo ngày
        TreeMap<String, Double> sortedRevenue = new TreeMap<>(revenueByDay);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("avgOrderValue", avgOrderValue);
        result.put("revenueByDay", sortedRevenue);
        result.put("period", Map.of("start", start.toString(), "end", end.toString()));
        
        // Tạo chart data
        ChartData chartData = ChartData.builder()
            .chartType("LINE")
            .title("Doanh thu theo ngày")
            .labels(new ArrayList<>(sortedRevenue.keySet()))
            .datasets(List.of(
                ChartDataset.builder()
                    .label("Doanh thu (VNĐ)")
                    .data(new ArrayList<>(sortedRevenue.values()))
                    .borderColor("#3B82F6")
                    .backgroundColor("rgba(59, 130, 246, 0.1)")
                    .build()
            ))
            .build();
        result.put("chartData", chartData);
        
        return result;
    }

    /**
     * Lấy sản phẩm bán chạy của shop
     */
    public Map<String, Object> getTopSellingProducts(String shopId, Integer limit) {
        log.info("AI Tool: getTopSellingProducts - shopId={}, limit={}", shopId, limit);
        
        int resultLimit = limit != null ? limit : 10;
        
        List<Product> products = productRepository.findByShopId(shopId, 
            PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "purchaseCount")))
            .getContent();
        
        List<Map<String, Object>> productList = products.stream()
            .map(p -> {
                Map<String, Object> pMap = new HashMap<>();
                // Không trả id - chỉ hiển thị tên và số liệu
                pMap.put("name", p.getName());
                pMap.put("price", p.getFinalPrice());
                pMap.put("purchaseCount", p.getPurchaseCount());
                pMap.put("stockQuantity", p.getStockQuantity());
                pMap.put("revenue", p.getPurchaseCount() * p.getFinalPrice());
                return pMap;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("products", productList);
        result.put("productIds", products.stream().map(Product::getId).collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Cảnh báo hàng tồn kho thấp
     */
    public Map<String, Object> getLowStockProducts(String shopId, Integer threshold) {
        log.info("AI Tool: getLowStockProducts - shopId={}, threshold={}", shopId, threshold);
        
        int stockThreshold = threshold != null ? threshold : 10;
        
        List<Product> allProducts = productRepository.findByShopId(shopId, PageRequest.of(0, 1000)).getContent();
        
        List<Product> lowStockProducts = allProducts.stream()
            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= stockThreshold)
            .sorted(Comparator.comparing(Product::getStockQuantity))
            .collect(Collectors.toList());
        
        List<Map<String, Object>> alerts = lowStockProducts.stream()
            .map(p -> {
                Map<String, Object> alert = new HashMap<>();
                // Không trả id - chỉ hiển thị tên và cảnh báo
                alert.put("name", p.getName());
                alert.put("stockQuantity", p.getStockQuantity());
                alert.put("purchaseCount", p.getPurchaseCount());
                alert.put("urgency", p.getStockQuantity() == 0 ? "CRITICAL" : 
                         p.getStockQuantity() <= 5 ? "HIGH" : "MEDIUM");
                return alert;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("lowStockProducts", alerts);
        result.put("totalCount", lowStockProducts.size());
        result.put("criticalCount", alerts.stream().filter(a -> "CRITICAL".equals(a.get("urgency"))).count());
        
        return result;
    }

    /**
     * Gợi ý tạo coupon cho sản phẩm có view cao nhưng mua thấp
     */
    public Map<String, Object> suggestCoupons(String shopId) {
        log.info("AI Tool: suggestCoupons - shopId={}", shopId);
        
        List<Product> products = productRepository.findByShopId(shopId, PageRequest.of(0, 100)).getContent();
        
        // Tìm sản phẩm có potential (view/cart cao, purchase thấp)
        List<CouponSuggestion> suggestions = new ArrayList<>();
        
        for (Product product : products) {
            // Lấy interaction data
            List<UserInteraction> interactions = userInteractionRepository.findByProductId(product.getId());
            
            int totalViews = interactions.stream().mapToInt(UserInteraction::getViewCount).sum();
            int totalCarts = interactions.stream().mapToInt(UserInteraction::getCartCount).sum();
            int totalPurchases = product.getPurchaseCount() != null ? product.getPurchaseCount() : 0;
            
            // Nếu có nhiều view/cart nhưng ít purchase -> gợi ý coupon
            if (totalViews > 50 && totalCarts > 10 && totalPurchases < totalCarts / 2) {
                double conversionRate = totalPurchases > 0 ? (double) totalPurchases / totalCarts * 100 : 0;
                double suggestedDiscount = conversionRate < 10 ? 15 : 
                                          conversionRate < 20 ? 10 : 5;
                
                suggestions.add(CouponSuggestion.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .viewCount(totalViews)
                    .purchaseCount(totalPurchases)
                    .suggestedDiscount(suggestedDiscount)
                    .reason(String.format("Tỷ lệ chuyển đổi thấp (%.1f%%). Đề xuất giảm %.0f%% để tăng doanh số.", 
                            conversionRate, suggestedDiscount))
                    .build());
            }
        }
        
        // Sắp xếp theo tiềm năng cao nhất
        suggestions.sort((a, b) -> b.getViewCount() - a.getViewCount());
        
        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", suggestions.stream().limit(10).collect(Collectors.toList()));
        result.put("totalSuggestions", suggestions.size());
        
        return result;
    }

    /**
     * Phân tích đơn hàng của shop
     */
    public Map<String, Object> getOrderAnalytics(String shopId, String startDate, String endDate) {
        log.info("AI Tool: getOrderAnalytics - shopId={}, startDate={}, endDate={}", shopId, startDate, endDate);
        
        LocalDateTime start = startDate != null ? 
            LocalDateTime.parse(startDate + "T00:00:00") : 
            LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? 
            LocalDateTime.parse(endDate + "T23:59:59") : 
            LocalDateTime.now();
        
        // Lấy tất cả đơn hàng của shop
        List<Order> allOrders = orderRepository.findByShopId(shopId, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Filter theo thời gian
        List<Order> filteredOrders = allOrders.stream()
            .filter(o -> o.getCreatedAt() != null && 
                        o.getCreatedAt().isAfter(start) && 
                        o.getCreatedAt().isBefore(end))
            .collect(Collectors.toList());
        
        // Thống kê theo status
        Map<String, Long> ordersByStatus = filteredOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getStatus().name(),
                Collectors.counting()
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", filteredOrders.size());
        result.put("ordersByStatus", ordersByStatus);
        result.put("pendingOrders", ordersByStatus.getOrDefault("PENDING", 0L));
        result.put("confirmedOrders", ordersByStatus.getOrDefault("CONFIRMED", 0L));
        result.put("shippingOrders", ordersByStatus.getOrDefault("SHIPPING", 0L));
        result.put("deliveredOrders", ordersByStatus.getOrDefault("DELIVERED", 0L));
        result.put("cancelledOrders", ordersByStatus.getOrDefault("CANCELLED", 0L));
        
        // Tạo chart data
        ChartData chartData = ChartData.builder()
            .chartType("PIE")
            .title("Phân bố trạng thái đơn hàng")
            .labels(new ArrayList<>(ordersByStatus.keySet()))
            .datasets(List.of(
                ChartDataset.builder()
                    .label("Số đơn hàng")
                    .data(ordersByStatus.values().stream().map(Long::doubleValue).collect(Collectors.toList()))
                    .build()
            ))
            .build();
        result.put("chartData", chartData);
        
        return result;
    }

    // ==================== ADMIN TOOLS ====================

    /**
     * Tổng hợp doanh thu toàn hệ thống
     */
    public Map<String, Object> getSystemRevenue(String startDate, String endDate) {
        log.info("AI Tool: getSystemRevenue - startDate={}, endDate={}", startDate, endDate);
        
        LocalDateTime start = startDate != null ? 
            LocalDateTime.parse(startDate + "T00:00:00") : 
            LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? 
            LocalDateTime.parse(endDate + "T23:59:59") : 
            LocalDateTime.now();
        
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        
        double totalRevenue = completedOrders.stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        
        // Doanh thu theo shop
        Map<String, Double> revenueByShop = completedOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getShopName() != null ? o.getShopName() : o.getShopId(),
                Collectors.summingDouble(Order::getTotalAmount)
            ));
        
        // Doanh thu theo ngày
        Map<String, Double> revenueByDay = completedOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getCreatedAt().toLocalDate().toString(),
                Collectors.summingDouble(Order::getTotalAmount)
            ));
        
        TreeMap<String, Double> sortedRevenue = new TreeMap<>(revenueByDay);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", completedOrders.size());
        result.put("revenueByShop", revenueByShop);
        result.put("revenueByDay", sortedRevenue);
        
        // Tạo chart data
        ChartData chartData = ChartData.builder()
            .chartType("LINE")
            .title("Doanh thu hệ thống theo ngày")
            .labels(new ArrayList<>(sortedRevenue.keySet()))
            .datasets(List.of(
                ChartDataset.builder()
                    .label("Doanh thu (VNĐ)")
                    .data(new ArrayList<>(sortedRevenue.values()))
                    .borderColor("#10B981")
                    .backgroundColor("rgba(16, 185, 129, 0.1)")
                    .build()
            ))
            .build();
        result.put("chartData", chartData);
        
        return result;
    }

    /**
     * Phân tích hiệu quả CustomerSegment
     */
    public Map<String, Object> getSegmentAnalytics() {
        log.info("AI Tool: getSegmentAnalytics");
        
        List<CustomerSegment> segments = customerSegmentRepository.findAll();
        List<User> users = userRepository.findAll();
        
        List<Map<String, Object>> segmentStats = segments.stream()
            .map(segment -> {
                List<User> segmentUsers = users.stream()
                    .filter(u -> segment.getId().equals(u.getCustomerSegmentId()))
                    .collect(Collectors.toList());
                
                double totalSpend = segmentUsers.stream()
                    .mapToDouble(u -> u.getTotalSpend() != null ? u.getTotalSpend() : 0)
                    .sum();
                
                double avgSpend = segmentUsers.size() > 0 ? totalSpend / segmentUsers.size() : 0;
                
                Map<String, Object> stat = new HashMap<>();
                stat.put("segmentId", segment.getId());
                stat.put("segmentName", segment.getName());
                stat.put("level", segment.getLevel());
                stat.put("userCount", segmentUsers.size());
                stat.put("totalSpend", totalSpend);
                stat.put("avgSpend", avgSpend);
                stat.put("minSpend", segment.getMinSpend());
                stat.put("maxSpend", segment.getMaxSpend());
                return stat;
            })
            .sorted((a, b) -> ((Integer)b.get("level")).compareTo((Integer)a.get("level")))
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("segments", segmentStats);
        result.put("totalSegments", segments.size());
        result.put("totalUsers", users.size());
        
        // Tạo chart data
        ChartData chartData = ChartData.builder()
            .chartType("BAR")
            .title("Số lượng khách hàng theo phân khúc")
            .labels(segmentStats.stream().map(s -> (String)s.get("segmentName")).collect(Collectors.toList()))
            .datasets(List.of(
                ChartDataset.builder()
                    .label("Số khách hàng")
                    .data(segmentStats.stream().map(s -> ((Number)s.get("userCount")).doubleValue()).collect(Collectors.toList()))
                    .backgroundColor("#8B5CF6")
                    .build()
            ))
            .build();
        result.put("chartData", chartData);
        
        return result;
    }

    /**
     * Gợi ý điều chỉnh CustomerSegment
     */
    public Map<String, Object> suggestSegmentAdjustments() {
        log.info("AI Tool: suggestSegmentAdjustments");
        
        List<CustomerSegment> segments = customerSegmentRepository.findAll();
        List<User> users = userRepository.findAll();
        
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        // Phân tích phân bố spending của users
        List<Double> allSpends = users.stream()
            .map(u -> u.getTotalSpend() != null ? u.getTotalSpend() : 0.0)
            .sorted()
            .collect(Collectors.toList());
        
        if (!allSpends.isEmpty()) {
            double median = allSpends.get(allSpends.size() / 2);
            double q1 = allSpends.get(allSpends.size() / 4);
            double q3 = allSpends.get(3 * allSpends.size() / 4);
            
            // Kiểm tra từng segment
            for (CustomerSegment segment : segments) {
                long usersInSegment = users.stream()
                    .filter(u -> segment.getId().equals(u.getCustomerSegmentId()))
                    .count();
                
                double percentOfUsers = users.size() > 0 ? (double) usersInSegment / users.size() * 100 : 0;
                
                // Segment có quá ít user
                if (usersInSegment < 10 && segment.getLevel() != null && segment.getLevel() > 1) {
                    suggestions.add(Map.of(
                        "segmentId", segment.getId(),
                        "segmentName", segment.getName(),
                        "issue", "Số lượng khách hàng quá ít (" + usersInSegment + " users)",
                        "suggestion", "Xem xét giảm ngưỡng minSpend để mở rộng phân khúc"
                    ));
                }
                
                // Segment có quá nhiều user (không phải basic)
                if (percentOfUsers > 40 && segment.getLevel() != null && segment.getLevel() > 1) {
                    suggestions.add(Map.of(
                        "segmentId", segment.getId(),
                        "segmentName", segment.getName(),
                        "issue", String.format("Quá nhiều khách hàng (%.1f%% tổng số)", percentOfUsers),
                        "suggestion", "Xem xét tăng ngưỡng minSpend để segment có tính độc quyền hơn"
                    ));
                }
            }
            
            // Thêm thông tin phân bố
            Map<String, Object> distribution = new HashMap<>();
            distribution.put("median", median);
            distribution.put("q1", q1);
            distribution.put("q3", q3);
            distribution.put("min", allSpends.get(0));
            distribution.put("max", allSpends.get(allSpends.size() - 1));
            
            Map<String, Object> result = new HashMap<>();
            result.put("suggestions", suggestions);
            result.put("spendDistribution", distribution);
            result.put("totalUsers", users.size());
            
            return result;
        }
        
        return Map.of("suggestions", suggestions, "message", "Không đủ dữ liệu để phân tích");
    }

    /**
     * Lấy tổng quan hệ thống
     */
    public Map<String, Object> getSystemOverview() {
        log.info("AI Tool: getSystemOverview");
        
        long totalProducts = productRepository.countByIsPublishedTrue();
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        
        // Doanh thu tháng này
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        List<Order> monthOrders = orderRepository.findCompletedPaidOrdersByDateRange(startOfMonth, LocalDateTime.now());
        double monthRevenue = monthOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalProducts", totalProducts);
        result.put("totalUsers", totalUsers);
        result.put("totalOrders", totalOrders);
        result.put("pendingOrders", pendingOrders);
        result.put("deliveredOrders", deliveredOrders);
        result.put("monthRevenue", monthRevenue);
        result.put("monthOrderCount", monthOrders.size());
        
        return result;
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> mapProductToSimpleInfo(Product product) {
        Map<String, Object> info = new HashMap<>();
        // Không trả id - chỉ dùng để render card, không hiển trong text
        info.put("name", product.getName());
        info.put("price", product.getPrice());
        info.put("finalPrice", product.getFinalPrice());
        info.put("saleOff", product.getSaleOff());
        info.put("averageRating", product.getAverageRating());
        info.put("reviewCount", product.getReviewCount());
        info.put("stockQuantity", product.getStockQuantity());
        
        // Format keyAttributes as a string for table display
        if (product.getAttributeValues() != null && !product.getAttributeValues().isEmpty()) {
            String keyAttributesStr = product.getAttributeValues().stream()
                .filter(attr -> attr.getAttributeKey() != null && 
                       (attr.getAttributeKey().equals("ram") || 
                        attr.getAttributeKey().equals("battery") || 
                        attr.getAttributeKey().equals("screen") ||
                        attr.getAttributeKey().equals("storage") ||
                        attr.getAttributeKey().equals("cpu") ||
                        attr.getAttributeKey().equals("vga")))
                .map(attr -> {
                    String unit = (attr.getUnit() != null && !attr.getUnit().isEmpty()) ? attr.getUnit() : "";
                    return attr.getAttributeName() + ": " + attr.getValue() + unit;
                })
                .collect(Collectors.joining(", "));
            
            if (!keyAttributesStr.isEmpty()) {
                info.put("keyAttributes", keyAttributesStr);
            } else {
                info.put("keyAttributes", "N/A");
            }
        } else {
            info.put("keyAttributes", "N/A");
        }
        
        return info;
    }

    private Map<String, Object> mapProductToDetailedInfo(Product product) {
        Map<String, Object> info = new HashMap<>();
        // Basic info without keyAttributes to avoid table display issues
        info.put("name", product.getName());
        info.put("price", product.getPrice());
        info.put("finalPrice", product.getFinalPrice());
        info.put("saleOff", product.getSaleOff());
        info.put("averageRating", product.getAverageRating());
        info.put("reviewCount", product.getReviewCount());
        info.put("stockQuantity", product.getStockQuantity());
        info.put("description", product.getDescription());
        info.put("purchaseCount", product.getPurchaseCount());
        
        // Format keyAttributes as a readable string for table display
        if (product.getAttributeValues() != null && !product.getAttributeValues().isEmpty()) {
            // Create a formatted string for key attributes (for table display)
            String keyAttributesStr = product.getAttributeValues().stream()
                .filter(attr -> attr.getAttributeKey() != null && 
                       (attr.getAttributeKey().equals("ram") || 
                        attr.getAttributeKey().equals("battery") || 
                        attr.getAttributeKey().equals("screen") ||
                        attr.getAttributeKey().equals("storage") ||
                        attr.getAttributeKey().equals("cpu") ||
                        attr.getAttributeKey().equals("vga")))
                .map(attr -> {
                    String unit = (attr.getUnit() != null && !attr.getUnit().isEmpty()) ? attr.getUnit() : "";
                    return attr.getAttributeName() + ": " + attr.getValue() + unit;
                })
                .collect(Collectors.joining(", "));
            
            if (!keyAttributesStr.isEmpty()) {
                info.put("keyAttributes", keyAttributesStr);
            } else {
                // Nếu không có key attributes, để string rỗng
                info.put("keyAttributes", "N/A");
            }
        } else {
            info.put("keyAttributes", "N/A");
        }
        
        return info;
    }

    /**
     * Đề xuất chiến lược kinh doanh toàn diện cho VENDOR
     */
    public Map<String, Object> suggestVendorBusinessStrategy(String shopId, Integer daysToAnalyze) {
        log.info("AI Tool: suggestVendorBusinessStrategy - shopId={}, daysToAnalyze={}", shopId, daysToAnalyze);
        
        int days = daysToAnalyze != null ? daysToAnalyze : 30;
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // 1. Phân tích doanh thu và đơn hàng
        List<Order> allOrders = orderRepository.findByShopIdAndCreatedAtAfter(shopId, startDate);
        List<Order> completedOrders = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .collect(Collectors.toList());
        
        double totalRevenue = completedOrders.stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        
        double avgOrderValue = completedOrders.size() > 0 ? totalRevenue / completedOrders.size() : 0;
        
        // 2. Phân tích sản phẩm
        List<Product> shopProducts = productRepository.findByShopIdAndIsPublishedTrue(shopId);
        List<Product> lowStockProducts = shopProducts.stream()
            .filter(p -> p.getStockQuantity() <= 10)
            .collect(Collectors.toList());
        
        // Top products
        List<Product> topProducts = shopProducts.stream()
            .sorted((a, b) -> Integer.compare(
                b.getPurchaseCount() != null ? b.getPurchaseCount() : 0,
                a.getPurchaseCount() != null ? a.getPurchaseCount() : 0
            ))
            .limit(5)
            .collect(Collectors.toList());
        
        // Poor performing products
        List<Product> poorProducts = shopProducts.stream()
            .filter(p -> (p.getPurchaseCount() == null || p.getPurchaseCount() < 5))
            .filter(p -> p.getCreatedAt().isBefore(LocalDateTime.now().minusDays(30)))
            .sorted((a, b) -> Integer.compare(
                a.getPurchaseCount() != null ? a.getPurchaseCount() : 0,
                b.getPurchaseCount() != null ? b.getPurchaseCount() : 0
            ))
            .limit(5)
            .collect(Collectors.toList());
        
        // 3. Phân tích xu hướng theo thời gian
        Map<String, Long> ordersByDay = completedOrders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                Collectors.counting()
            ));
        
        // 4. Tạo các đề xuất cụ thể
        List<Map<String, Object>> strategies = new ArrayList<>();
        
        // Strategy 1: Revenue optimization
        if (totalRevenue > 0) {
            Map<String, Object> revenueStrategy = new HashMap<>();
            revenueStrategy.put("category", "Tối ưu doanh thu");
            revenueStrategy.put("priority", "HIGH");
            
            List<String> actions = new ArrayList<>();
            if (avgOrderValue < 500000) {
                actions.add("Tạo combo sản phẩm để tăng giá trị đơn hàng trung bình");
                actions.add("Áp dụng 'Mua 2 giảm 10%' cho sản phẩm có margin cao");
            }
            if (topProducts.size() > 0) {
                actions.add("Tăng stock cho top " + topProducts.size() + " sản phẩm bán chạy để tránh hết hàng");
                actions.add("Cross-sell: Gợi ý sản phẩm bổ sung khi khách mua " + topProducts.get(0).getName());
            }
            actions.add("Chạy flash sale vào khung giờ 20h-22h (giờ vàng mua sắm online)");
            
            revenueStrategy.put("actions", actions);
            revenueStrategy.put("expectedImpact", "Tăng 15-25% doanh thu trong 2 tuần");
            strategies.add(revenueStrategy);
        }
        
        // Strategy 2: Inventory management
        if (lowStockProducts.size() > 0) {
            Map<String, Object> inventoryStrategy = new HashMap<>();
            inventoryStrategy.put("category", "Quản lý tồn kho");
            inventoryStrategy.put("priority", "CRITICAL");
            
            List<String> actions = new ArrayList<>();
            actions.add("CẦN NHẬP NGAY: " + lowStockProducts.size() + " sản phẩm sắp hết hàng");
            lowStockProducts.stream().limit(3).forEach(p -> 
                actions.add("  - " + p.getName() + " (còn " + p.getStockQuantity() + " cái)")
            );
            actions.add("Thiết lập cảnh báo tự động khi stock < 15 cho sản phẩm bán chạy");
            
            inventoryStrategy.put("actions", actions);
            inventoryStrategy.put("expectedImpact", "Tránh mất đơn hàng do hết stock");
            strategies.add(inventoryStrategy);
        }
        
        // Strategy 3: Product portfolio optimization
        if (poorProducts.size() > 0) {
            Map<String, Object> productStrategy = new HashMap<>();
            productStrategy.put("category", "Tối ưu danh mục sản phẩm");
            productStrategy.put("priority", "MEDIUM");
            
            List<String> actions = new ArrayList<>();
            actions.add(poorProducts.size() + " sản phẩm kém hiệu quả cần xử lý:");
            poorProducts.stream().limit(3).forEach(p -> 
                actions.add("  - " + p.getName() + " (chỉ " + (p.getPurchaseCount() != null ? p.getPurchaseCount() : 0) + " lượt bán)")
            );
            actions.add("Giảm giá sốc 30-40% để thanh lý và thu hồi vốn");
            actions.add("Bundle với sản phẩm bán chạy để tăng visibility");
            actions.add("Cân nhắc ngừng nhập hàng cho các SKU này");
            
            productStrategy.put("actions", actions);
            productStrategy.put("expectedImpact", "Giảm 20% hàng tồn kho chậm luân chuyển");
            strategies.add(productStrategy);
        }
        
        // Strategy 4: Marketing & Promotion
        Map<String, Object> marketingStrategy = new HashMap<>();
        marketingStrategy.put("category", "Marketing & Khuyến mãi");
        marketingStrategy.put("priority", "HIGH");
        
        List<String> marketingActions = new ArrayList<>();
        if (topProducts.size() > 0) {
            marketingActions.add("Tạo Landing Page cho sản phẩm best-seller: " + topProducts.get(0).getName());
            marketingActions.add("Chạy Sponsored Ads cho top 3 sản phẩm (ROI cao nhất)");
        }
        marketingActions.add("Tạo coupon FREE SHIP cho đơn > " + (int)(avgOrderValue * 0.8) + "đ");
        marketingActions.add("Email Marketing: Gửi 'Deal trong tuần' cho khách hàng cũ");
        marketingActions.add("Chạy contest 'Review nhận quà' để tăng social proof");
        
        marketingStrategy.put("actions", marketingActions);
        marketingStrategy.put("expectedImpact", "Tăng 30% traffic và 20% conversion rate");
        strategies.add(marketingStrategy);
        
        // Strategy 5: Customer retention
        Map<String, Object> retentionStrategy = new HashMap<>();
        retentionStrategy.put("category", "Giữ chân khách hàng");
        retentionStrategy.put("priority", "MEDIUM");
        
        List<String> retentionActions = new ArrayList<>();
        retentionActions.add("Tạo chương trình 'Tích điểm đổi quà' cho khách quen");
        retentionActions.add("Gửi coupon sinh nhật (giảm 15%) cho khách hàng");
        retentionActions.add("After-sales service: Nhắc bảo hành sau 3 tháng mua");
        retentionActions.add("VIP program: Giảm 10% cố định cho khách mua > 3 lần");
        
        retentionStrategy.put("actions", retentionActions);
        retentionStrategy.put("expectedImpact", "Tăng 40% tỷ lệ khách quay lại mua");
        strategies.add(retentionStrategy);
        
        // Tạo result
        Map<String, Object> result = new HashMap<>();
        result.put("analysisпериод", days + " ngày");
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", completedOrders.size());
        result.put("avgOrderValue", avgOrderValue);
        result.put("totalProducts", shopProducts.size());
        result.put("lowStockCount", lowStockProducts.size());
        result.put("topPerformers", topProducts.stream().limit(3)
            .map(p -> Map.of("name", p.getName(), "sales", p.getPurchaseCount() != null ? p.getPurchaseCount() : 0))
            .collect(Collectors.toList()));
        result.put("strategies", strategies);
        
        // Summary insights
        List<String> keyInsights = new ArrayList<>();
        keyInsights.add("📊 Doanh thu " + days + " ngày: " + String.format("%,.0fđ", totalRevenue));
        keyInsights.add("📦 Số đơn hoàn thành: " + completedOrders.size() + " đơn");
        keyInsights.add("💰 Giá trị đơn hàng TB: " + String.format("%,.0fđ", avgOrderValue));
        keyInsights.add("⚠️ " + lowStockProducts.size() + " sản phẩm cần nhập hàng gấp");
        keyInsights.add("🏆 Top seller: " + (topProducts.size() > 0 ? topProducts.get(0).getName() : "N/A"));
        keyInsights.add("🎯 " + strategies.size() + " chiến lược đề xuất với priority cao");
        
        result.put("keyInsights", keyInsights);
        
        return result;
    }

    /**
     * Đề xuất chiến lược kinh doanh toàn diện cho ADMIN (toàn hệ thống)
     */
    public Map<String, Object> suggestAdminBusinessStrategy(Integer daysToAnalyze) {
        log.info("AI Tool: suggestAdminBusinessStrategy - daysToAnalyze={}", daysToAnalyze);
        
        int days = daysToAnalyze != null ? daysToAnalyze : 30;
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // 1. Phân tích doanh thu toàn hệ thống
        List<Order> allOrders = orderRepository.findByCreatedAtAfter(startDate);
        List<Order> completedOrders = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .collect(Collectors.toList());
        
        double totalRevenue = completedOrders.stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        
        // Revenue by shop
        Map<String, Double> revenueByShop = completedOrders.stream()
            .collect(Collectors.groupingBy(
                Order::getShopId,
                Collectors.summingDouble(Order::getTotalAmount)
            ));
        
        // 2. Phân tích phân khúc khách hàng
        List<CustomerSegment> segments = customerSegmentRepository.findAll();
        List<User> users = userRepository.findAll();
        
        Map<String, Long> usersBySegment = users.stream()
            .filter(u -> u.getCustomerSegmentId() != null)
            .collect(Collectors.groupingBy(
                User::getCustomerSegmentId,
                Collectors.counting()
            ));
        
        Map<String, Double> spendingBySegment = users.stream()
            .filter(u -> u.getCustomerSegmentId() != null)
            .collect(Collectors.groupingBy(
                User::getCustomerSegmentId,
                Collectors.summingDouble(u -> u.getTotalSpend() != null ? u.getTotalSpend() : 0.0)
            ));
        
        // 3. Phân tích sản phẩm toàn hệ thống
        List<Product> allProducts = productRepository.findAll();
        long activeProducts = allProducts.stream().filter(Product::getIsPublished).count();
        
        List<Product> topProducts = allProducts.stream()
            .filter(Product::getIsPublished)
            .sorted((a, b) -> Integer.compare(
                b.getPurchaseCount() != null ? b.getPurchaseCount() : 0,
                a.getPurchaseCount() != null ? a.getPurchaseCount() : 0
            ))
            .limit(10)
            .collect(Collectors.toList());
        
        // 4. Tạo các đề xuất chiến lược cấp hệ thống
        List<Map<String, Object>> strategies = new ArrayList<>();
        
        // Strategy 1: Market expansion
        Map<String, Object> expansionStrategy = new HashMap<>();
        expansionStrategy.put("category", "Mở rộng thị trường");
        expansionStrategy.put("priority", "HIGH");
        
        List<String> expansionActions = new ArrayList<>();
        expansionActions.add("Mở thêm 2-3 danh mục sản phẩm mới trending (Smart Home, Gaming Gear)");
        expansionActions.add("Tuyển dụng thêm " + Math.max(5, revenueByShop.size() / 10) + " vendor chất lượng cao");
        expansionActions.add("Partnership với các thương hiệu chính hãng để tăng trust");
        expansionActions.add("Mở rộng logistics: Thêm 2 kho vùng để giao hàng nhanh hơn");
        
        expansionStrategy.put("actions", expansionActions);
        expansionStrategy.put("expectedImpact", "Tăng 35% GMV trong Q tiếp theo");
        strategies.add(expansionStrategy);
        
        // Strategy 2: Customer segment optimization
        Map<String, Object> segmentStrategy = new HashMap<>();
        segmentStrategy.put("category", "Tối ưu phân khúc khách hàng");
        segmentStrategy.put("priority", "CRITICAL");
        
        List<String> segmentActions = new ArrayList<>();
        // Find most valuable segment
        String topSegmentId = spendingBySegment.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (topSegmentId != null) {
            Optional<CustomerSegment> topSeg = customerSegmentRepository.findById(topSegmentId);
            if (topSeg.isPresent()) {
                segmentActions.add("Focus marketing budget 60% vào segment '" + topSeg.get().getName() + "' (revenue cao nhất)");
            }
        }
        
        segmentActions.add("Tạo campaign 'Nâng hạng VIP' để push khách từ Bronze lên Silver");
        segmentActions.add("Personalized homepage cho từng segment (AI-powered)");
        segmentActions.add("Điều chỉnh ngưỡng phân khúc dựa trên phân tích quartiles");
        segmentActions.add("VIP exclusive: Early access sale cho segment cao nhất");
        
        segmentStrategy.put("actions", segmentActions);
        segmentStrategy.put("expectedImpact", "Tăng 25% lifetime value khách hàng");
        strategies.add(segmentStrategy);
        
        // Strategy 3: Platform optimization
        Map<String, Object> platformStrategy = new HashMap<>();
        platformStrategy.put("category", "Tối ưu nền tảng");
        platformStrategy.put("priority", "HIGH");
        
        List<String> platformActions = new ArrayList<>();
        platformActions.add("Nâng cấp AI recommendation engine để tăng CTR 30%");
        platformActions.add("Implement 'Buy Now Pay Later' (BNPL) để tăng AOV 40%");
        platformActions.add("Mobile app optimization: Giảm loading time xuống <2s");
        platformActions.add("Live stream shopping: Hỗ trợ vendors bán hàng qua live");
        platformActions.add("Social commerce integration: Bán trực tiếp trên Facebook/TikTok");
        
        platformStrategy.put("actions", platformActions);
        platformStrategy.put("expectedImpact", "Tăng 45% conversion rate và UX score");
        strategies.add(platformStrategy);
        
        // Strategy 4: Vendor ecosystem
        Map<String, Object> vendorStrategy = new HashMap<>();
        vendorStrategy.put("category", "Phát triển hệ sinh thái vendors");
        vendorStrategy.put("priority", "MEDIUM");
        
        List<String> vendorActions = new ArrayList<>();
        vendorActions.add("Vendor training program: Hướng dẫn tối ưu listing và marketing");
        vendorActions.add("Performance-based incentives: Giảm 20% commission cho top 10% vendors");
        vendorActions.add("Vendor dashboard nâng cao: Real-time analytics + AI insights");
        vendorActions.add("Quality control tự động: AI scan product images và descriptions");
        vendorActions.add("Cross-vendor collaboration: Cho phép bundle products từ nhiều shops");
        
        vendorStrategy.put("actions", vendorActions);
        vendorStrategy.put("expectedImpact", "Tăng 30% vendor satisfaction và retention");
        strategies.add(vendorStrategy);
        
        // Strategy 5: Marketing & Growth
        Map<String, Object> growthStrategy = new HashMap<>();
        growthStrategy.put("category", "Marketing & Tăng trưởng");
        growthStrategy.put("priority", "HIGH");
        
        List<String> growthActions = new ArrayList<>();
        if (topProducts.size() > 0) {
            growthActions.add("Mega Campaign: '" + topProducts.get(0).getName() + " Festival' với 50% flash deals");
        }
        growthActions.add("Referral program: 'Mời bạn nhận 100K' cho cả người giới thiệu và người mới");
        growthActions.add("Content marketing: SEO blog về tech reviews (organic traffic +200%)");
        growthActions.add("Influencer partnerships: 10-15 tech YouTubers/TikTokers");
        growthActions.add("TV/OOH advertising: Thương hiệu hóa Cellex trong Q4 (mùa mua sắm)");
        
        growthStrategy.put("actions", growthActions);
        growthStrategy.put("expectedImpact", "Tăng 60% new users và brand awareness 2x");
        strategies.add(growthStrategy);
        
        // Tạo result
        Map<String, Object> result = new HashMap<>();
        result.put("analysisPeriod", days + " ngày");
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", completedOrders.size());
        result.put("totalShops", revenueByShop.size());
        result.put("activeProducts", activeProducts);
        result.put("totalUsers", users.size());
        result.put("strategies", strategies);
        
        // Top insights
        List<String> keyInsights = new ArrayList<>();
        keyInsights.add("💰 GMV " + days + " ngày: " + String.format("%,.0fđ", totalRevenue));
        keyInsights.add("🏪 Số shops hoạt động: " + revenueByShop.size());
        keyInsights.add("👥 Tổng users: " + users.size());
        keyInsights.add("📦 Sản phẩm active: " + activeProducts);
        keyInsights.add("🎯 " + strategies.size() + " chiến lược tăng trưởng cấp hệ thống");
        keyInsights.add("📈 Tiềm năng tăng GMV 50-70% trong 3 tháng tới");
        
        result.put("keyInsights", keyInsights);
        
        // Top performing categories/products
        if (topProducts.size() > 0) {
            result.put("topProducts", topProducts.stream().limit(5)
                .map(p -> Map.of(
                    "name", p.getName(),
                    "sales", p.getPurchaseCount() != null ? p.getPurchaseCount() : 0,
                    "revenue", p.getPurchaseCount() != null ? p.getPurchaseCount() * p.getFinalPrice() : 0
                ))
                .collect(Collectors.toList()));
        }
        
        return result;
    }
}
