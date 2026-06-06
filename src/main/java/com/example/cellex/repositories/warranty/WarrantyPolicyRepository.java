package com.example.cellex.repositories.warranty;

import com.example.cellex.models.warranty.WarrantyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarrantyPolicyRepository extends JpaRepository<WarrantyPolicy, UUID> {
    List<WarrantyPolicy> findByProductId(String productId);
}