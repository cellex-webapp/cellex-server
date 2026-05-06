package com.example.cellex.repositories.inventory;

import com.example.cellex.models.inventory.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {

    List<InventoryBatch> findBySkuUuidOrderByImportDateAsc(UUID skuUuid);

    List<InventoryBatch> findBySupplierUuidOrderByImportDateDesc(UUID supplierUuid);

    List<InventoryBatch> findAllByOrderByImportDateDesc();
}
