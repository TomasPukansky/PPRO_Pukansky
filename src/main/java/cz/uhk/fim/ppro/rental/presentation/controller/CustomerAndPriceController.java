package cz.uhk.fim.ppro.rental.presentation.controller;

import cz.uhk.fim.ppro.rental.application.service.CustomerAndPriceService;
import cz.uhk.fim.ppro.rental.presentation.dto.CustomerDto;
import cz.uhk.fim.ppro.rental.presentation.dto.PriceRuleDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CustomerAndPriceController {

    private final CustomerAndPriceService customerAndPriceService;

    public CustomerAndPriceController(CustomerAndPriceService customerAndPriceService) {
        this.customerAndPriceService = customerAndPriceService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDto>> getCustomers(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(customerAndPriceService.getAllCustomers(query));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerAndPriceService.getCustomerById(id));
    }

    @GetMapping("/customers/by-phone/{phone}")
    public ResponseEntity<CustomerDto> getCustomerByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(customerAndPriceService.getCustomerByPhone(phone));
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAndPriceService.createCustomer(dto));
    }

    @GetMapping("/price-rules")
    public ResponseEntity<List<PriceRuleDto>> getPriceRules() {
        return ResponseEntity.ok(customerAndPriceService.getAllPriceRules());
    }
}
