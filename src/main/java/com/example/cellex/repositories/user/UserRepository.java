package com.example.cellex.repositories.user;

import com.example.cellex.enums.Role;
import com.example.cellex.models.user.User;
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
 * JPA Repository for User entity (PostgreSQL/Supabase).
 * Migrated from MongoRepository. Maintains the same method signatures
 * where possible for backward compatibility with all dependent services.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Custom query method to find a user by their email address.
    Optional<User> findByEmail(String email);

    // Find all users in a specific customer segment
    List<User> findByCustomerSegmentId(String customerSegmentId);

    // ==================== Analytics Methods ====================

    /**
     * Count users by role enum
     */
    long countByRole(Role role);

    /**
     * Count users created between dates
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Count active users
     */
    long countByIsActiveTrue();

    /**
     * Count non-banned users
     */
    long countByIsBannedFalse();

    /**
     * Count users by role and created between dates
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByRoleAndCreatedAtBetween(@Param("role") Role role,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Count users by role created before a date
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.createdAt <= :beforeDate")
    long countByRoleAndCreatedAtBefore(@Param("role") Role role,
                                       @Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Find users by role and created between dates
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByRoleAndCreatedAtBetween(@Param("role") Role role,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find users by role with pagination
     */
    List<User> findByRole(Role role, Pageable pageable);

    /**
     * Search users by name (case insensitive) with pagination
     */
    Page<User> findByFullNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find by UUID (convenience for String-to-UUID conversion in services).
     */
    @Query("SELECT u FROM User u WHERE u.uuid = :uuid")
    Optional<User> findByUuid(@Param("uuid") UUID uuid);

    /**
     * Backward-compatible findById accepting String ID.
     * Converts String to UUID and delegates to the standard findById(UUID).
     * This ensures all existing services that pass String IDs still compile and work.
     */
    default Optional<User> findById(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        try {
            return findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Query("SELECT u FROM User u WHERE (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchByFullNameOrEmail(@Param("keyword") String keyword, Pageable pageable);
}
