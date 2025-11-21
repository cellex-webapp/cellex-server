package com.example.cellex.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponse {
    
    private List<NotificationResponse> notifications;
    private Long unreadCount;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
}
