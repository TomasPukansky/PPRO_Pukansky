package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {

    Optional<PriceRule> findFirstByCategoryIdOrderByValidFromDesc(Long categoryId);
}
