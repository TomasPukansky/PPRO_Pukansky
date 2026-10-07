package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.DamageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DamageRecordRepository extends JpaRepository<DamageRecord, Long> {

    List<DamageRecord> findByItemIdOrderByReportedAtDesc(Long itemId);

    List<DamageRecord> findByCustomerIdOrderByReportedAtDesc(Long customerId);
}
