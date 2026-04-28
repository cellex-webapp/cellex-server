package com.example.cellex.repositories.inventory;

import com.example.cellex.models.inventory.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    default Optional<Supplier> findById(String id) {
        try {
            return findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Query("SELECT s FROM Supplier s WHERE s.shopUuid = :shopUuid AND s.isActive = true ORDER BY s.createdAt DESC")
    Page<Supplier> findByShopUuidAndIsActiveTrue(@Param("shopUuid") UUID shopUuid, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.shopUuid = :shopUuid AND s.isActive = true " +
            "AND LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY s.createdAt DESC")
    Page<Supplier> findByShopUuidAndNameContaining(
            @Param("shopUuid") UUID shopUuid,
            @Param("search") String search,
            Pageable pageable
    );

    Page<Supplier> findByIsActiveTrue(Pageable pageable);

    Page<Supplier> findByIsActiveTrueAndSupplierNameContainingIgnoreCase(String supplierName, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Supplier s " +
            "WHERE s.shopUuid = :shopUuid AND s.phoneNumber = :phoneNumber AND s.isActive = true")
    boolean existsByShopUuidAndPhoneNumber(
            @Param("shopUuid") UUID shopUuid,
            @Param("phoneNumber") String phoneNumber
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Supplier s " +
            "WHERE s.shopUuid = :shopUuid AND s.taxCode = :taxCode AND s.isActive = true")
    boolean existsByShopUuidAndTaxCode(
            @Param("shopUuid") UUID shopUuid,
            @Param("taxCode") String taxCode
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Supplier s " +
            "WHERE s.shopUuid = :shopUuid AND s.phoneNumber = :phoneNumber AND s.uuid <> :supplierUuid AND s.isActive = true")
    boolean existsDuplicatePhoneForUpdate(
            @Param("shopUuid") UUID shopUuid,
            @Param("phoneNumber") String phoneNumber,
            @Param("supplierUuid") UUID supplierUuid
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Supplier s " +
            "WHERE s.shopUuid = :shopUuid AND s.taxCode = :taxCode AND s.uuid <> :supplierUuid AND s.isActive = true")
    boolean existsDuplicateTaxCodeForUpdate(
            @Param("shopUuid") UUID shopUuid,
            @Param("taxCode") String taxCode,
            @Param("supplierUuid") UUID supplierUuid
    );

    @Query("SELECT s FROM Supplier s WHERE s.uuid = :supplierUuid AND s.shopUuid = :shopUuid AND s.isActive = true")
    Optional<Supplier> findByUuidAndShopUuid(
            @Param("supplierUuid") UUID supplierUuid,
            @Param("shopUuid") UUID shopUuid
    );

    default Optional<Supplier> findByIdAndShopId(String supplierId, String shopId) {
        try {
            return findByUuidAndShopUuid(UUID.fromString(supplierId), UUID.fromString(shopId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
