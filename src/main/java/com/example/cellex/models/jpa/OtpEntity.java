package com.example.cellex.models.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for the 'otps' table in PostgreSQL (Supabase).
 * Stores OTP codes for signup verification and password reset.
 * Temporarily holds registration data until verification succeeds.
 */
@Entity
@Table(name = "otps")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "is_used")
    @Builder.Default
    private boolean isUsed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    // Temporary registration data
    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "hashed_password", length = 255)
    private String hashedPassword;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
