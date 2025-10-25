package com.example.cellex.repositories;

import com.example.cellex.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
// This interface handles database operations for the User entity.
public interface UserRepository extends MongoRepository<User, String> {
    // Custom query method to find a user by their email address.
    Optional<User> findByEmail(String email);
    
    // Find all users in a specific customer segment
    List<User> findByCustomerSegmentId(String customerSegmentId);
}