package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.ServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceLogRepository extends JpaRepository<ServiceLog, Long> {

    List<ServiceLog> findByItemIdOrderByStartedAtDesc(Long itemId);

    List<ServiceLog> findByFinishedAtIsNullOrderByStartedAtDesc();
}
