package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for UserEntity (PostgreSQL/Supabase).
 * Replaces the old MongoDB UserRepository for all user operations.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByCustomerSegmentId(UUID customerSegmentId);

    // ==================== Analytics Methods ====================

    @Query("SELECT COUNT(u) FROM UserEntity u JOIN u.roles r WHERE r.roleName = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    long countByIsActiveTrue();

    long countByIsBannedFalse();

    @Query("SELECT COUNT(u) FROM UserEntity u JOIN u.roles r WHERE r.roleName = :roleName AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByRoleNameAndCreatedAtBetween(@Param("roleName") String roleName,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(u) FROM UserEntity u JOIN u.roles r WHERE r.roleName = :roleName AND u.createdAt <= :beforeDate")
    long countByRoleNameAndCreatedAtBefore(@Param("roleName") String roleName,
                                           @Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.roleName = :roleName AND u.createdAt BETWEEN :startDate AND :endDate")
    List<UserEntity> findByRoleNameAndCreatedAtBetween(@Param("roleName") String roleName,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.roleName = :roleName")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    Page<UserEntity> findByFullNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByEmail(String email);
}
