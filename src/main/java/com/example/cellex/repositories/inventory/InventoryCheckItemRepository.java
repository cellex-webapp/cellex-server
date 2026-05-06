package com.example.cellex.repositories.inventory;

import com.example.cellex.models.inventory.InventoryCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryCheckItemRepository extends JpaRepository<InventoryCheckItem, UUID> {

    List<InventoryCheckItem> findByCheckUuid(UUID checkUuid);

    List<InventoryCheckItem> findBySkuUuid(UUID skuUuid);

    long countByCheckUuid(UUID checkUuid);
}
