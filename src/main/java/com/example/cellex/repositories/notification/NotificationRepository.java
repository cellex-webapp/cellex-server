package com.example.cellex.repositories.notification;

import com.example.cellex.enums.NotificationType;
import com.example.cellex.models.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("{ $or: [ { 'user_id': ?0 }, { 'is_broadcast': true } ], $or: [ { 'expires_at': null }, { 'expires_at': { $gt: ?1 } } ] }")
    Page<Notification> findUserNotifications(String userId, LocalDateTime now, Pageable pageable);
    
    @Query(value = "{ $or: [ { 'user_id': ?0 }, { 'is_broadcast': true } ], 'is_read': false, $or: [ { 'expires_at': null }, { 'expires_at': { $gt: ?1 } } ] }", count = true)
    Long countUnreadNotifications(String userId, LocalDateTime now);
    
    List<Notification> findByUserIdAndIsReadFalse(String userId);
    
    List<Notification> findByTypeAndUserId(NotificationType type, String userId);
    
    List<Notification> findByUserIdAndIsReadFalse(String userId, LocalDateTime readAt);
    
    @Query(value = "{ 'expires_at': { $ne: null, $lt: ?0 } }")
    List<Notification> findExpiredNotifications(LocalDateTime now);
    
    Page<Notification> findByIsBroadcastTrueOrderByCreatedAtDesc(Pageable pageable);
}
