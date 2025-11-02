package com.example.cellex.repositories.category;

import com.example.cellex.models.category.CategoryAttribute;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryAttributeRepository extends MongoRepository<CategoryAttribute, String> {

    List<CategoryAttribute> findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(String categoryId);

    List<CategoryAttribute> findByCategoryIdAndIsHighlightTrueAndIsActiveTrueOrderBySortOrderAsc(String categoryId);

    boolean existsByCategoryIdAndAttributeKey(String categoryId, String attributeKey);
}
