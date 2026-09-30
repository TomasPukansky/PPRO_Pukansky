package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import cz.uhk.fim.ppro.rental.presentation.dto.CreateEquipmentItemRequest;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentCategoryDto;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentItemDto;
import cz.uhk.fim.ppro.rental.presentation.dto.UpdateEquipmentStatusRequest;

import java.util.List;

public interface EquipmentItemService {

    List<EquipmentItemDto> getItems(Long categoryId, EquipmentStatus status);

    EquipmentItemDto getItemById(Long id);

    EquipmentItemDto createItem(CreateEquipmentItemRequest request);

    EquipmentItemDto updateStatus(Long id, UpdateEquipmentStatusRequest request);

    List<EquipmentCategoryDto> getAllCategories();
}
