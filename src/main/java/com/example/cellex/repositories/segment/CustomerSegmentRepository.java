package com.example.cellex.repositories.segment;

import com.example.cellex.models.segment.CustomerSegment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, UUID> {

    List<CustomerSegment> findAllByOrderByLevelDesc();

    Page<CustomerSegment> findAllByOrderByLevelDesc(Pageable pageable);

    Optional<CustomerSegment> findByLevel(Integer level);

    // --- Backward-compat: String ID methods ---

    default Optional<CustomerSegment> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default boolean existsById(String id) {
        return existsById(UUID.fromString(id));
    }

    default void deleteById(String id) {
        deleteById(UUID.fromString(id));
    }
}

