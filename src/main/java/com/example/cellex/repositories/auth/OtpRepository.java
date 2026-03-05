package com.example.cellex.repositories.auth;

import com.example.cellex.models.auth.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Otp entity (PostgreSQL/Supabase).
 * Migrated from MongoRepository.
 */
@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findByCodeAndEmail(String code, String email);
}