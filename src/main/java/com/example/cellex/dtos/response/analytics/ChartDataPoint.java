package com.example.cellex.dtos.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO cho dữ liệu biểu đồ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartDataPoint {
    
    /**
     * Label hiển thị (trục X)
     */
    private String label;
    
    /**
     * Giá trị (trục Y)
     */
    private Double value;
    
    /**
     * Ngày tương ứng (nếu là time series)
     */
    private LocalDate date;
    
    /**
     * Giá trị phụ (cho multi-series chart)
     */
    private Map<String, Double> additionalValues;

    /**
     * DTO cho Time Series Chart (biểu đồ theo thời gian)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeSeriesChart {
        /**
         * Tiêu đề biểu đồ
         */
        private String title;
        
        /**
         * Loại biểu đồ: LINE, BAR, AREA
         */
        private ChartType chartType;
        
        /**
         * Dữ liệu theo thời gian
         */
        private List<ChartDataPoint> data;
        
        /**
         * Label trục X
         */
        private String xAxisLabel;
        
        /**
         * Label trục Y
         */
        private String yAxisLabel;
        
        /**
         * Đơn vị
         */
        private String unit;
        
        /**
         * Tổng giá trị
         */
        private Double total;
        
        /**
         * Giá trị trung bình
         */
        private Double average;
    }

    /**
     * DTO cho Pie/Donut Chart
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PieChartData {
        /**
         * Tiêu đề biểu đồ
         */
        private String title;
        
        /**
         * Các phần của biểu đồ
         */
        private List<PieSlice> slices;
        
        /**
         * Tổng giá trị
         */
        private Double total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PieSlice {
        private String label;
        private Double value;
        private Double percentage;
        private String color;  // Gợi ý màu cho frontend
    }

    /**
     * DTO cho Bar Chart với nhiều series
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BarChartData {
        private String title;
        private List<String> categories;  // Labels trục X
        private List<BarSeries> series;   // Nhiều series dữ liệu
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BarSeries {
        private String name;
        private List<Double> data;
        private String color;
    }

    public enum ChartType {
        LINE,
        BAR,
        AREA,
        PIE,
        DONUT
    }
}
