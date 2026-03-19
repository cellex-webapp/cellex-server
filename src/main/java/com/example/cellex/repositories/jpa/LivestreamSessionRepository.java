package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.LivestreamSessionEntity;
import com.example.cellex.models.livestream.LivestreamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LivestreamSessionRepository extends JpaRepository<LivestreamSessionEntity, String> {
    List<LivestreamSessionEntity> findByStatus(LivestreamStatus status);
}