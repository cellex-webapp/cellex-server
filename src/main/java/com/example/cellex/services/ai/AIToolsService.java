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
        
        if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndIsPublishedTrue(
                keyword, PageRequest.of(0, resultLimit)).getContent();
        } else if (categoryId != null && !categoryId.isEmpty()) {
            products = productRepository.findByCategoryIdAndIsPublishedTrue(
                categoryId, PageRequest.of(0, resultLimit)).getContent();
        } else {
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
        return info;
    }

    private Map<String, Object> mapProductToDetailedInfo(Product product) {
        Map<String, Object> info = mapProductToSimpleInfo(product);
        info.put("description", product.getDescription());
        info.put("purchaseCount", product.getPurchaseCount());
        
        if (product.getAttributeValues() != null) {
            List<Map<String, String>> attributes = product.getAttributeValues().stream()
                .map(attr -> {
                    Map<String, String> attrMap = new HashMap<>();
                    attrMap.put("name", attr.getAttributeName());
                    attrMap.put("value", attr.getValue());
                    attrMap.put("unit", attr.getUnit() != null ? attr.getUnit() : "");
                    return attrMap;
                })
                .collect(Collectors.toList());
            info.put("attributes", attributes);
        }
        
        return info;
    }
}
