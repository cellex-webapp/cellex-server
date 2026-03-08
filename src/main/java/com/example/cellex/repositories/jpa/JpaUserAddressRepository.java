package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for UserAddressEntity (PostgreSQL/Supabase).
 */
@Repository
public interface JpaUserAddressRepository extends JpaRepository<UserAddressEntity, UUID> {

    List<UserAddressEntity> findByUserUuidOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    Optional<UserAddressEntity> findByIdAndUserUuid(UUID id, UUID userId);

    Optional<UserAddressEntity> findByUserUuidAndIsDefaultTrue(UUID userId);

    long countByUserUuid(UUID userId);

    @Modifying
    @Query("UPDATE UserAddressEntity a SET a.isDefault = false WHERE a.user.uuid = :userId AND a.isDefault = true")
    void resetDefaultAddresses(@Param("userId") UUID userId);

    // Keep backward compatibility
    default List<UserAddressEntity> findByUserId(UUID userId) {
        return findByUserUuidOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    default Optional<UserAddressEntity> findByUserIdAndIsDefaultTrue(UUID userId) {
        return findByUserUuidAndIsDefaultTrue(userId);
    }

    default long countByUserId(UUID userId) {
        return countByUserUuid(userId);
    }
}
