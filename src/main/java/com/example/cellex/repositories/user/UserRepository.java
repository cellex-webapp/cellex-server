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
}