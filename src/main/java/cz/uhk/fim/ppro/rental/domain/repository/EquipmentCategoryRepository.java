package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, Long> {
    Optional<EquipmentCategory> findByCode(String code);
}
