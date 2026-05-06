package com.example.cellex.repositories.inventory;

import com.example.cellex.enums.InventoryTransactionType;
import com.example.cellex.models.inventory.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findBySkuUuidOrderByCreatedAtDesc(UUID skuUuid);

    List<InventoryTransaction> findByReferenceIdOrderByCreatedAtDesc(String referenceId);

    List<InventoryTransaction> findByTypeOrderByCreatedAtDesc(InventoryTransactionType type);
}
