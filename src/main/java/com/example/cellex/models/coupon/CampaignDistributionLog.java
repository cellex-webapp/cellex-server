package com.example.cellex.models.coupon;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "campaign_distribution_logs")
public class CampaignDistributionLog {

    @Id
    private String id;

    @Field("campaign_id")
    private String campaignId;

    @Field("admin_id")
    private String adminId;

    @Field("filter_criteria")
    private Map<String, Object> filterCriteria; // JSON của filter đã áp dụng

    @Field("recipients_count")
    private Integer recipientsCount; // Số lượng người nhận dự kiến

    @Field("success_count")
    private Integer successCount; // Số lượng phát thành công

    @Field("failed_count")
    private Integer failedCount; // Số lượng thất bại

    @Field("error_summary")
    private String errorSummary; // Tóm tắt lỗi nếu có

    @Field("execution_time_ms")
    private Long executionTimeMs; // Thời gian thực thi (ms)

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
}

