package cz.uhk.fim.ppro.rental.presentation.controller;

import cz.uhk.fim.ppro.rental.application.service.EquipmentItemService;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentCategoryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class EquipmentCategoryController {

    private final EquipmentItemService itemService;

    public EquipmentCategoryController(EquipmentItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<List<EquipmentCategoryDto>> getAllCategories() {
        return ResponseEntity.ok(itemService.getAllCategories());
    }
}
