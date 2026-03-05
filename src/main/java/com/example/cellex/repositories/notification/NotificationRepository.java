package com.example.cellex.repositories.notification;

import com.example.cellex.enums.NotificationType;
import com.example.cellex.models.notification.Notification;
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

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserUuidOrderByCreatedAtDesc(UUID userUuid, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE (n.userUuid = :userId OR n.isBroadcast = true) " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) ORDER BY n.createdAt DESC")
    Page<Notification> findUserNotificationsByUuid(@Param("userId") UUID userId,
                                                   @Param("now") LocalDateTime now,
                                                   Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userUuid = :userId " +
           "AND n.isBroadcast = false AND n.isRead = false " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    Long countUnreadPersonalNotificationsByUuid(@Param("userId") UUID userId,
                                                @Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notification n WHERE n.isBroadcast = true " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    List<Notification> findActiveBroadcastNotifications(@Param("now") LocalDateTime now);

    List<Notification> findByUserUuidAndIsReadFalse(UUID userUuid);

    List<Notification> findByTypeAndUserUuid(NotificationType type, UUID userUuid);

    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);

    Page<Notification> findByIsBroadcastTrueOrderByCreatedAtDesc(Pageable pageable);

    // --- Backward-compat: String ID methods ---

    default Optional<Notification> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable) {
        return findByUserUuidOrderByCreatedAtDesc(UUID.fromString(userId), pageable);
    }

    default Page<Notification> findUserNotifications(String userId, LocalDateTime now, Pageable pageable) {
        return findUserNotificationsByUuid(UUID.fromString(userId), now, pageable);
    }

    default Long countUnreadPersonalNotifications(String userId, LocalDateTime now) {
        return countUnreadPersonalNotificationsByUuid(UUID.fromString(userId), now);
    }

    default List<Notification> findByUserIdAndIsReadFalse(String userId) {
        return findByUserUuidAndIsReadFalse(UUID.fromString(userId));
    }

    default List<Notification> findByTypeAndUserId(NotificationType type, String userId) {
        return findByTypeAndUserUuid(type, UUID.fromString(userId));
    }
}
