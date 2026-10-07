package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.presentation.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RentalService {

    RentalOrderDto createReservation(CreateReservationRequest request);

    RentalOrderDto handoutOrder(Long orderId, HandoutOrderRequest request);

    RentalOrderDto returnSingleItem(Long orderId, Long rentalItemId, ReturnItemRequest request);

    RentalOrderDto swapItem(Long orderId, Long rentalItemId, Long newItemId);

    RentalOrderDto cancelOrder(Long orderId, String reason);

    int expireUnclaimedReservations();

    List<RentalOrderDto> getOrders(String status);

    RentalOrderDto getOrderById(Long id);

    RentalOrderDto getOrderByNumber(String orderNumber);

    List<EquipmentItemDto> getAvailableItemsForSwap(Long rentalItemId);

    // Reports / Přehledy
    List<RentalOrderDto> getActiveRentals();

    List<RentalOrderDto> getOverdueRentals();

    List<Map<String, Object>> getItemUsageReport();

    Map<String, Object> getAvailabilityMatrix(LocalDate date);

    List<Map<String, Object>> getActiveServices();
}
