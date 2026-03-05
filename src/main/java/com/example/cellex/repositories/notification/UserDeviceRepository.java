package com.example.cellex.repositories.notification;

import com.example.cellex.models.notification.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findByUserUuidAndIsActiveTrue(UUID userUuid);

    List<UserDevice> findByUserUuid(UUID userUuid);

    List<UserDevice> findByIsActiveTrue();

    boolean existsByFcmToken(String fcmToken);

    // --- Backward-compat: String ID methods ---

    default Optional<UserDevice> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default List<UserDevice> findByUserIdAndIsActiveTrue(String userId) {
        return findByUserUuidAndIsActiveTrue(UUID.fromString(userId));
    }

    default List<UserDevice> findByUserId(String userId) {
        return findByUserUuid(UUID.fromString(userId));
    }
}
