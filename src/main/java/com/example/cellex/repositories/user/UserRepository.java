package com.example.cellex.repositories.user;

import com.example.cellex.enums.Role;
import com.example.cellex.models.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
// This interface handles database operations for the User entity.
public interface UserRepository extends MongoRepository<User, String> {
    // Custom query method to find a user by their email address.
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    
    // Find all users in a specific customer segment
    List<User> findByCustomerSegmentId(String customerSegmentId);

    // ==================== Analytics Methods ====================

    /**
     * Đếm số user theo role
     */
    long countByRole(Role role);

    /**
     * Đếm số user được tạo trong khoảng thời gian
     */
    @Query(value = "{'created_at': {$gte: ?0, $lte: ?1}}", count = true)
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Đếm số user đang active
     */
    long countByIsActiveTrue();

    /**
     * Đếm số user không bị banned
     */
    long countByIsBannedFalse();

    /**
     * Đếm số user theo role và khoảng thời gian tạo
     */
    @Query(value = "{'role': ?0, 'created_at': {$gte: ?1, $lte: ?2}}", count = true)
    long countByRoleAndCreatedAtBetween(Role role, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Đếm số user theo role và tạo trước thời điểm
     */
    @Query(value = "{'role': ?0, 'created_at': {$lte: ?1}}", count = true)
    long countByRoleAndCreatedAtBefore(Role role, LocalDateTime beforeDate);

    /**
     * Tìm users theo role và khoảng thời gian tạo
     */
    @Query("{'role': ?0, 'created_at': {$gte: ?1, $lte: ?2}}")
    List<User> findByRoleAndCreatedAtBetween(Role role, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tìm users theo role với phân trang
     */
    List<User> findByRole(Role role, org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm users theo tên (case insensitive) với phân trang
     */
    org.springframework.data.domain.Page<User> findByFullNameContainingIgnoreCase(String name, org.springframework.data.domain.Pageable pageable);
}