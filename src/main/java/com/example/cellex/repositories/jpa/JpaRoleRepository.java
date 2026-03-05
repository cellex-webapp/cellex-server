package com.example.cellex.repositories.jpa;

import com.example.cellex.models.jpa.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for RoleEntity (PostgreSQL/Supabase).
 */
@Repository
public interface JpaRoleRepository extends JpaRepository<RoleEntity, Integer> {

    Optional<RoleEntity> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);
}
