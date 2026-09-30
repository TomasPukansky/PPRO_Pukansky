package cz.uhk.fim.ppro.rental.presentation.controller;

import cz.uhk.fim.ppro.rental.application.service.EquipmentItemService;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import cz.uhk.fim.ppro.rental.presentation.dto.CreateEquipmentItemRequest;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentItemDto;
import cz.uhk.fim.ppro.rental.presentation.dto.UpdateEquipmentStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class EquipmentItemController {

    private final EquipmentItemService itemService;

    public EquipmentItemController(EquipmentItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Získání seznamu inventárních kusů s možností filtrace (např. pouze volné pro sobotní ráno).
     */
    @GetMapping
    public ResponseEntity<List<EquipmentItemDto>> getItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) EquipmentStatus status) {
        List<EquipmentItemDto> items = itemService.getItems(categoryId, status);
        return ResponseEntity.ok(items);
    }

    /**
     * Detail konkrétního kusu vybavení.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentItemDto> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    /**
     * Zaevidování nového kusu vybavení do půjčovny.
     */
    @PostMapping
    public ResponseEntity<EquipmentItemDto> createItem(@Valid @RequestBody CreateEquipmentItemRequest request) {
        EquipmentItemDto created = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Změna stavu kusu (např. odeslání do servisu, ukončení servisu).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<EquipmentItemDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEquipmentStatusRequest request) {
        EquipmentItemDto updated = itemService.updateStatus(id, request);
        return ResponseEntity.ok(updated);
    }
}
