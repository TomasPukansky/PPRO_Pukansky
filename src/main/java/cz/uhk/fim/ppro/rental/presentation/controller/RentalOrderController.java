package cz.uhk.fim.ppro.rental.presentation.controller;

import cz.uhk.fim.ppro.rental.application.service.RentalService;
import cz.uhk.fim.ppro.rental.presentation.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class RentalOrderController {

    private final RentalService rentalService;

    public RentalOrderController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public ResponseEntity<List<RentalOrderDto>> getOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(rentalService.getOrders(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalOrderDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalService.getOrderById(id));
    }

    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<RentalOrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(rentalService.getOrderByNumber(orderNumber));
    }

    @PostMapping("/reservations")
    public ResponseEntity<RentalOrderDto> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        RentalOrderDto created = rentalService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/handout")
    public ResponseEntity<RentalOrderDto> handoutOrder(
            @PathVariable Long id,
            @Valid @RequestBody HandoutOrderRequest request) {
        return ResponseEntity.ok(rentalService.handoutOrder(id, request));
    }

    @PostMapping("/{orderId}/items/{rentalItemId}/return")
    public ResponseEntity<RentalOrderDto> returnItem(
            @PathVariable Long orderId,
            @PathVariable Long rentalItemId,
            @RequestBody ReturnItemRequest request) {
        return ResponseEntity.ok(rentalService.returnSingleItem(orderId, rentalItemId, request));
    }

    @GetMapping("/items/{rentalItemId}/available-swaps")
    public ResponseEntity<List<EquipmentItemDto>> getAvailableSwaps(@PathVariable Long rentalItemId) {
        return ResponseEntity.ok(rentalService.getAvailableItemsForSwap(rentalItemId));
    }

    @PostMapping("/{orderId}/items/{rentalItemId}/swap")
    public ResponseEntity<RentalOrderDto> swapItem(
            @PathVariable Long orderId,
            @PathVariable Long rentalItemId,
            @Valid @RequestBody SwapItemRequest request) {
        return ResponseEntity.ok(rentalService.swapItem(orderId, rentalItemId, request.getNewItemId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<RentalOrderDto> cancelOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Zrušeno zákazníkem") String reason) {
        return ResponseEntity.ok(rentalService.cancelOrder(id, reason));
    }

    @PostMapping("/expire-unclaimed")
    public ResponseEntity<Map<String, Object>> expireUnclaimed() {
        int count = rentalService.expireUnclaimedReservations();
        return ResponseEntity.ok(Map.of("expiredCount", count, "message", "Prošlé rezervace byly úspěšně stornovány a kusy uvolněny."));
    }
}
