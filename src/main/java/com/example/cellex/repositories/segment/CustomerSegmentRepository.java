package com.example.cellex.repositories.segment;

import com.example.cellex.models.segment.CustomerSegment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerSegmentRepository extends MongoRepository<CustomerSegment, String> {
    
    // Tìm segment phù hợp với tổng chi tiêu (minSpend <= totalSpend < maxSpend hoặc maxSpend = null)
    List<CustomerSegment> findAllByOrderByLevelDesc();
    
    Optional<CustomerSegment> findByLevel(Integer level);
}

