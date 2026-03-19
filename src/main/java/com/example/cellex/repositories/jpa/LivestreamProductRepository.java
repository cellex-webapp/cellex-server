package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.LivestreamProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LivestreamProductRepository extends JpaRepository<LivestreamProductEntity, String> {
    List<LivestreamProductEntity> findBySessionId(String sessionId);
}