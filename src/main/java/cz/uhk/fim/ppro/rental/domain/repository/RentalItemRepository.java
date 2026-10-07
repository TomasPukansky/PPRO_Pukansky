package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.RentalItem;
import cz.uhk.fim.ppro.rental.domain.model.RentalItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {

    List<RentalItem> findByOrderId(Long orderId);

    List<RentalItem> findByItemId(Long itemId);

    @Query("SELECT ri FROM RentalItem ri JOIN ri.order o " +
           "WHERE ri.item.id = :itemId " +
           "AND o.status IN ('RESERVATION', 'ACTIVE') " +
           "AND ri.status IN ('RESERVED', 'RENTED') " +
           "AND (:excludeOrderId IS NULL OR o.id != :excludeOrderId) " +
           "AND (o.startsAt < :newEnd AND o.expectedEndAt > :newStart)")
    List<RentalItem> findOverlappingActiveItems(
            @Param("itemId") Long itemId,
            @Param("newStart") OffsetDateTime newStart,
            @Param("newEnd") OffsetDateTime newEnd,
            @Param("excludeOrderId") Long excludeOrderId
    );

    @Query("SELECT ri FROM RentalItem ri JOIN ri.order o " +
           "WHERE o.status = 'ACTIVE' AND ri.status = 'RENTED' AND o.expectedEndAt < :now")
    List<RentalItem> findOverdueRentalItems(@Param("now") OffsetDateTime now);

    @Query("SELECT ri.item.id, COUNT(ri) FROM RentalItem ri GROUP BY ri.item.id")
    List<Object[]> countUsageByItemId();
}
