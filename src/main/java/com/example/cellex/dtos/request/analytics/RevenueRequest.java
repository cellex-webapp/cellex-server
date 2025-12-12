package com.example.cellex.dtos.request.analytics;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO Request cho việc lấy thống kê doanh thu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueRequest {

    /**
     * Ngày bắt đầu (YYYY-MM-DD)
     */
    private LocalDate startDate;

    /**
     * Ngày kết thúc (YYYY-MM-DD)
     */
    private LocalDate endDate;
}
