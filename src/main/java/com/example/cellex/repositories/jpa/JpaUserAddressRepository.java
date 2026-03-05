package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for UserAddressEntity (PostgreSQL/Supabase).
 */
@Repository
public interface JpaUserAddressRepository extends JpaRepository<UserAddressEntity, UUID> {

    List<UserAddressEntity> findByUserId(UUID userId);

    Optional<UserAddressEntity> findByUserIdAndIsDefaultTrue(UUID userId);

    long countByUserId(UUID userId);
}
