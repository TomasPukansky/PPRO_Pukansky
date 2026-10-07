package cz.uhk.fim.ppro.rental.presentation.controller;

import cz.uhk.fim.ppro.rental.application.service.RentalService;
import cz.uhk.fim.ppro.rental.presentation.dto.RentalOrderDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final RentalService rentalService;

    public ReportController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping("/active-rentals")
    public ResponseEntity<List<RentalOrderDto>> getActiveRentals() {
        return ResponseEntity.ok(rentalService.getActiveRentals());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<RentalOrderDto>> getOverdueRentals() {
        return ResponseEntity.ok(rentalService.getOverdueRentals());
    }

    @GetMapping("/item-usage")
    public ResponseEntity<List<Map<String, Object>>> getItemUsageReport() {
        return ResponseEntity.ok(rentalService.getItemUsageReport());
    }

    @GetMapping("/availability-matrix")
    public ResponseEntity<Map<String, Object>> getAvailabilityMatrix(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(rentalService.getAvailabilityMatrix(date));
    }

    @GetMapping("/active-services")
    public ResponseEntity<List<Map<String, Object>>> getActiveServices() {
        return ResponseEntity.ok(rentalService.getActiveServices());
    }
}
