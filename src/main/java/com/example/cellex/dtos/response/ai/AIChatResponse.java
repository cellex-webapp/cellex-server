package com.example.cellex.dtos.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO cho AI Chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResponse {
    
    /**
     * Nội dung phản hồi từ AI
     */
    private String message;
    
    /**
     * ID của conversation
     */
    private String conversationId;
    
    /**
     * Metadata bổ sung (productIds, chartData, etc.)
     */
    private AIMetadata metadata;
    
    /**
     * Loại response
     */
    private AIResponseType responseType;
    
    /**
     * Timestamp
     */
    private String timestamp;
    
    /**
     * Metadata class chứa dữ liệu bổ sung
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIMetadata {
        /**
         * Danh sách product IDs khi AI gợi ý sản phẩm
         */
        private List<String> productIds;
        
        /**
         * Dữ liệu biểu đồ cho vendor/admin
         */
        private ChartData chartData;
        
        /**
         * Dữ liệu bảng thống kê
         */
        private List<Map<String, Object>> tableData;
        
        /**
         * Gợi ý coupon
         */
        private List<CouponSuggestion> couponSuggestions;
        
        /**
         * Tên function đã được gọi
         */
        private String functionCalled;
    }
    
    /**
     * Loại response từ AI
     */
    public enum AIResponseType {
        TEXT,           // Chỉ là text thông thường
        PRODUCT_LIST,   // Có danh sách sản phẩm
        CHART,          // Có dữ liệu biểu đồ
        TABLE,          // Có dữ liệu bảng
        COUPON,         // Gợi ý coupon
        MIXED           // Kết hợp nhiều loại
    }
    
    /**
     * Dữ liệu biểu đồ
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private String chartType; // LINE, BAR, PIE
        private List<String> labels;
        private List<ChartDataset> datasets;
        private String title;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataset {
        private String label;
        private List<Double> data;
        private String backgroundColor;
        private String borderColor;
    }
    
    /**
     * Gợi ý coupon
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponSuggestion {
        private String productId;
        private String productName;
        private Integer viewCount;
        private Integer purchaseCount;
        private Double suggestedDiscount;
        private String reason;
    }
}
