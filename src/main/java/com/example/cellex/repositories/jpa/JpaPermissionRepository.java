package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for PermissionEntity (PostgreSQL/Supabase).
 */
@Repository
public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, Integer> {

    Optional<PermissionEntity> findByPermissionKey(String permissionKey);

    boolean existsByPermissionKey(String permissionKey);
}
