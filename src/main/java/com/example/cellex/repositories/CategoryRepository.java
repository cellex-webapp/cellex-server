package com.example.cellex.repositories;

import com.example.cellex.models.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByIsActiveTrue();
    boolean existsBySlug(String slug);
    Optional<Category> findBySlug(String slug);
    Optional<Category> findBySlugAndIsActiveTrue(String slug);
}