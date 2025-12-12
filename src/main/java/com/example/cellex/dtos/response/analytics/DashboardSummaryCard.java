package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * DTO cho Summary Card hiển thị trên Dashboard
 * Đại diện cho một ô metric với giá trị chính, so sánh với kỳ trước và trend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryCard {
    
    /**
     * Tiêu đề của card (VD: "Tổng doanh thu", "Số khách hàng mới")
     */
    private String title;
    
    /**
     * Giá trị chính hiển thị (dạng String để linh hoạt format)
     */
    private String value;
    
    /**
     * Giá trị số (để frontend có thể format theo ý)
     */
    private Double numericValue;
    
    /**
     * Đơn vị của giá trị (VD: "VND", "đơn", "sản phẩm")
     */
    private String unit;
    
    /**
     * Phần trăm thay đổi so với kỳ trước
     * Dương = tăng, Âm = giảm
     */
    private Double changePercent;
    
    /**
     * Giá trị kỳ trước để so sánh
     */
    private Double previousValue;
    
    /**
     * Trend: UP, DOWN, STABLE
     */
    private TrendDirection trend;
    
    /**
     * Loại metric (để frontend biết cách format)
     */
    private MetricType metricType;
    
    /**
     * Mô tả ngắn gọn
     */
    private String description;
    
    /**
     * Icon gợi ý (frontend sẽ map)
     */
    private String icon;
    
    public enum TrendDirection {
        UP,      // Tăng
        DOWN,    // Giảm
        STABLE   // Ổn định (thay đổi < 1%)
    }
    
    public enum MetricType {
        CURRENCY,    // Tiền tệ
        NUMBER,      // Số lượng
        PERCENTAGE,  // Phần trăm
        RATING       // Đánh giá (sao)
    }
    
    /**
     * Factory method để tạo card với auto-calculated trend
     */
    public static DashboardSummaryCard create(String title, Double currentValue, Double previousValue, 
                                               String unit, MetricType type, String icon, String description) {
        Double changePercent = null;
        TrendDirection trend = TrendDirection.STABLE;
        
        if (previousValue != null && previousValue != 0) {
            changePercent = ((currentValue - previousValue) / previousValue) * 100;
            changePercent = Math.round(changePercent * 100.0) / 100.0;
            
            if (changePercent > 1) {
                trend = TrendDirection.UP;
            } else if (changePercent < -1) {
                trend = TrendDirection.DOWN;
            }
        } else if (currentValue != null && currentValue > 0) {
            changePercent = 100.0;
            trend = TrendDirection.UP;
        }
        
        String formattedValue = formatValue(currentValue, type);
        
        return DashboardSummaryCard.builder()
                .title(title)
                .value(formattedValue)
                .numericValue(currentValue)
                .unit(unit)
                .changePercent(changePercent)
                .previousValue(previousValue)
                .trend(trend)
                .metricType(type)
                .icon(icon)
                .description(description)
                .build();
    }
    
    private static String formatValue(Double value, MetricType type) {
        if (value == null) return "0";
        
        switch (type) {
            case CURRENCY:
                if (value >= 1_000_000_000) {
                    return String.format("%.2f tỷ", value / 1_000_000_000);
                } else if (value >= 1_000_000) {
                    return String.format("%.2f triệu", value / 1_000_000);
                } else if (value >= 1_000) {
                    return String.format("%.1fK", value / 1_000);
                }
                return String.format("%.0f", value);
            case PERCENTAGE:
                return String.format("%.1f%%", value);
            case RATING:
                return String.format("%.1f", value);
            default:
                if (value >= 1_000_000) {
                    return String.format("%.2fM", value / 1_000_000);
                } else if (value >= 1_000) {
                    return String.format("%.1fK", value / 1_000);
                }
                return String.format("%.0f", value);
        }
    }
}
