package com.example.cellex.models.jpa;

import com.example.cellex.models.livestream.LivestreamStatus;
import com.example.cellex.models.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "livestream_sessions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivestreamSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String thumbnail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LivestreamStatus status;

    @Column(name = "room_id", unique = true)
    private String roomId; // Mã phòng sinh ra bởi ZegoCloud

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}