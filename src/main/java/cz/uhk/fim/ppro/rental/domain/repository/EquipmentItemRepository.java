package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentItem;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {

    boolean existsByInventoryNumber(String inventoryNumber);

    Optional<EquipmentItem> findByInventoryNumber(String inventoryNumber);

    List<EquipmentItem> findByStatus(EquipmentStatus status);

    List<EquipmentItem> findByCategoryId(Long categoryId);

    List<EquipmentItem> findByCategoryIdAndStatus(Long categoryId, EquipmentStatus status);
}
