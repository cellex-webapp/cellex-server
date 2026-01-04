package com.example.cellex.repositories.notification;

import com.example.cellex.models.notification.UserNotificationRead;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationReadRepository extends MongoRepository<UserNotificationRead, String> {
    
    Optional<UserNotificationRead> findByUserIdAndNotificationId(String userId, String notificationId);
    
    List<UserNotificationRead> findByUserId(String userId);
    
    List<UserNotificationRead> findByNotificationId(String notificationId);
    
    boolean existsByUserIdAndNotificationId(String userId, String notificationId);
    
    void deleteByNotificationId(String notificationId);
}
