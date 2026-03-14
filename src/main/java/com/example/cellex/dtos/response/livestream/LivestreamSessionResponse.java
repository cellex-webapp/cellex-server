package com.example.cellex.dtos.response.livestream;

import com.example.cellex.models.livestream.LivestreamStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LivestreamSessionResponse {
    private String id;
    private String vendorId;
    private String vendorName;
    private String title;
    private String thumbnail;
    private LivestreamStatus status;
    private String roomId;
    private String zegoToken; // Token để Client kết nối video
    private LocalDateTime startedAt;
}