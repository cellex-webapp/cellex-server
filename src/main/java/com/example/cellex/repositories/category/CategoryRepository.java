package com.example.cellex.repositories.category;

import com.example.cellex.models.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByIsActiveTrue();

    Page<Category> findByIsActiveTrue(Pageable pageable);

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    // --- Backward-compat: String ID methods ---

    default Optional<Category> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default boolean existsById(String id) {
        return existsById(UUID.fromString(id));
    }

    default void deleteById(String id) {
        deleteById(UUID.fromString(id));
    }
}