package com.example.cellex.repositories.category;

import com.example.cellex.models.category.CategoryAttribute;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface CategoryAttributeRepository extends MongoRepository<CategoryAttribute, String> {

    List<CategoryAttribute> findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(String categoryId);

    List<CategoryAttribute> findByCategoryIdAndIsHighlightTrueAndIsActiveTrueOrderBySortOrderAsc(String categoryId);

    // Pageable variants
    Page<CategoryAttribute> findByCategoryIdAndIsActiveTrue(String categoryId, Pageable pageable);

    Page<CategoryAttribute> findByCategoryIdAndIsHighlightTrueAndIsActiveTrue(String categoryId, Pageable pageable);

    boolean existsByCategoryIdAndAttributeKey(String categoryId, String attributeKey);
}
