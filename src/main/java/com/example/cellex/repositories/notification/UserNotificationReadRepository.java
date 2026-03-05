package com.example.cellex.repositories.notification;

import com.example.cellex.models.notification.UserNotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationReadRepository extends JpaRepository<UserNotificationRead, UUID> {

    Optional<UserNotificationRead> findByUserUuidAndNotificationUuid(UUID userUuid, UUID notificationUuid);

    List<UserNotificationRead> findByUserUuid(UUID userUuid);

    List<UserNotificationRead> findByNotificationUuid(UUID notificationUuid);

    boolean existsByUserUuidAndNotificationUuid(UUID userUuid, UUID notificationUuid);

    void deleteByNotificationUuid(UUID notificationUuid);

    // --- Backward-compat: String ID methods ---

    default Optional<UserNotificationRead> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default Optional<UserNotificationRead> findByUserIdAndNotificationId(String userId, String notificationId) {
        return findByUserUuidAndNotificationUuid(UUID.fromString(userId), UUID.fromString(notificationId));
    }

    default List<UserNotificationRead> findByUserId(String userId) {
        return findByUserUuid(UUID.fromString(userId));
    }

    default List<UserNotificationRead> findByNotificationId(String notificationId) {
        return findByNotificationUuid(UUID.fromString(notificationId));
    }

    default boolean existsByUserIdAndNotificationId(String userId, String notificationId) {
        return existsByUserUuidAndNotificationUuid(UUID.fromString(userId), UUID.fromString(notificationId));
    }

    default void deleteByNotificationId(String notificationId) {
        deleteByNotificationUuid(UUID.fromString(notificationId));
    }
}
