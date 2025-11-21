package com.example.cellex.repositories.notification;

import com.example.cellex.models.notification.UserDevice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends MongoRepository<UserDevice, String> {
    
    Optional<UserDevice> findByFcmToken(String fcmToken);
    
    List<UserDevice> findByUserIdAndIsActiveTrue(String userId);
    
    List<UserDevice> findByUserId(String userId);
    
    List<UserDevice> findByIsActiveTrue();
    
    boolean existsByFcmToken(String fcmToken);
}
