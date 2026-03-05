package com.example.cellex.repositories.category;

import com.example.cellex.models.category.CategoryAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {

    // JPA query methods using actual field name (categoryUuid)
    List<CategoryAttribute> findByCategoryUuidAndIsActiveTrueOrderBySortOrderAsc(UUID categoryUuid);

    List<CategoryAttribute> findByCategoryUuidAndIsHighlightTrueAndIsActiveTrueOrderBySortOrderAsc(UUID categoryUuid);

    Page<CategoryAttribute> findByCategoryUuidAndIsActiveTrue(UUID categoryUuid, Pageable pageable);

    Page<CategoryAttribute> findByCategoryUuidAndIsHighlightTrueAndIsActiveTrue(UUID categoryUuid, Pageable pageable);

    boolean existsByCategoryUuidAndAttributeKey(UUID categoryUuid, String attributeKey);

    // --- Backward-compat: String ID methods ---

    default Optional<CategoryAttribute> findById(String id) {
        return findById(UUID.fromString(id));
    }

    default List<CategoryAttribute> findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(String categoryId) {
        return findByCategoryUuidAndIsActiveTrueOrderBySortOrderAsc(UUID.fromString(categoryId));
    }

    default List<CategoryAttribute> findByCategoryIdAndIsHighlightTrueAndIsActiveTrueOrderBySortOrderAsc(String categoryId) {
        return findByCategoryUuidAndIsHighlightTrueAndIsActiveTrueOrderBySortOrderAsc(UUID.fromString(categoryId));
    }

    default Page<CategoryAttribute> findByCategoryIdAndIsActiveTrue(String categoryId, Pageable pageable) {
        return findByCategoryUuidAndIsActiveTrue(UUID.fromString(categoryId), pageable);
    }

    default Page<CategoryAttribute> findByCategoryIdAndIsHighlightTrueAndIsActiveTrue(String categoryId, Pageable pageable) {
        return findByCategoryUuidAndIsHighlightTrueAndIsActiveTrue(UUID.fromString(categoryId), pageable);
    }

    default boolean existsByCategoryIdAndAttributeKey(String categoryId, String attributeKey) {
        return existsByCategoryUuidAndAttributeKey(UUID.fromString(categoryId), attributeKey);
    }
}
