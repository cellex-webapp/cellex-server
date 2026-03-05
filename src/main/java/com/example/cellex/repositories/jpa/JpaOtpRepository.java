package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for OtpEntity (PostgreSQL/Supabase).
 * Replaces the old MongoDB OtpRepository.
 */
@Repository
public interface JpaOtpRepository extends JpaRepository<OtpEntity, UUID> {

    Optional<OtpEntity> findByCodeAndEmail(String code, String email);
}
