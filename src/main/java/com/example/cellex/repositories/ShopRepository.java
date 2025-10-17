package com.example.cellex.repositories;

import com.example.cellex.models.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends MongoRepository<Shop, String> {
    Optional<Shop> findByVendorId(String vendorId);
    Optional<Shop> findByVendorIdAndIsVerifiedTrue(String vendorId);
    List<Shop> findByIsVerified(Boolean isVerified);
    boolean existsByVendorId(String vendorId);
}
