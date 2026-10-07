package cz.uhk.fim.ppro.rental.domain.repository;

import cz.uhk.fim.ppro.rental.domain.model.OrderStatus;
import cz.uhk.fim.ppro.rental.domain.model.RentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    Optional<RentalOrder> findByOrderNumber(String orderNumber);

    List<RentalOrder> findByStatus(OrderStatus status);

    List<RentalOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT o FROM RentalOrder o WHERE o.status IN (:statuses) ORDER BY o.startsAt ASC")
    List<RentalOrder> findByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM RentalOrder o WHERE o.status = 'ACTIVE' AND o.expectedEndAt < :now")
    List<RentalOrder> findOverdueOrders(@Param("now") OffsetDateTime now);

    @Query("SELECT o FROM RentalOrder o WHERE o.status = 'RESERVATION' AND o.startsAt <= :cutoffTime")
    List<RentalOrder> findUnclaimedReservations(@Param("cutoffTime") OffsetDateTime cutoffTime);
}
