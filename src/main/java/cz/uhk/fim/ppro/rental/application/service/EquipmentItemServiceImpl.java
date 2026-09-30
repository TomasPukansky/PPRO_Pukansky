package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.application.exception.BusinessValidationException;
import cz.uhk.fim.ppro.rental.application.exception.DuplicateInventoryNumberException;
import cz.uhk.fim.ppro.rental.application.exception.ResourceNotFoundException;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentCategory;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentItem;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import cz.uhk.fim.ppro.rental.domain.repository.EquipmentCategoryRepository;
import cz.uhk.fim.ppro.rental.domain.repository.EquipmentItemRepository;
import cz.uhk.fim.ppro.rental.presentation.dto.CreateEquipmentItemRequest;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentCategoryDto;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentItemDto;
import cz.uhk.fim.ppro.rental.presentation.dto.UpdateEquipmentStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EquipmentItemServiceImpl implements EquipmentItemService {

    private final EquipmentItemRepository itemRepository;
    private final EquipmentCategoryRepository categoryRepository;

    public EquipmentItemServiceImpl(EquipmentItemRepository itemRepository, EquipmentCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentItemDto> getItems(Long categoryId, EquipmentStatus status) {
        List<EquipmentItem> items;
        if (categoryId != null && status != null) {
            items = itemRepository.findByCategoryIdAndStatus(categoryId, status);
        } else if (categoryId != null) {
            items = itemRepository.findByCategoryId(categoryId);
        } else if (status != null) {
            items = itemRepository.findByStatus(status);
        } else {
            items = itemRepository.findAll();
        }

        return items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentItemDto getItemById(Long id) {
        EquipmentItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kus vybavení s ID " + id + " nebyl nalezen."));
        return mapToDto(item);
    }

    @Override
    public EquipmentItemDto createItem(CreateEquipmentItemRequest request) {
        String cleanInvNumber = request.getInventoryNumber().trim().toUpperCase();

        if (itemRepository.existsByInventoryNumber(cleanInvNumber)) {
            throw new DuplicateInventoryNumberException(cleanInvNumber);
        }

        EquipmentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategorie s ID " + request.getCategoryId() + " neexistuje."));

        EquipmentStatus status = request.getStatus() != null ? request.getStatus() : EquipmentStatus.AVAILABLE;

        EquipmentItem item = new EquipmentItem(
                category,
                cleanInvNumber,
                request.getSize().trim(),
                request.getPurchaseYear(),
                request.getConditionState().trim(),
                status
        );

        EquipmentItem saved = itemRepository.save(item);
        return mapToDto(saved);
    }

    @Override
    public EquipmentItemDto updateStatus(Long id, UpdateEquipmentStatusRequest request) {
        EquipmentItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kus vybavení s ID " + id + " nebyl nalezen."));

        EquipmentStatus currentStatus = item.getStatus();
        EquipmentStatus targetStatus = request.getStatus();

        if (currentStatus == targetStatus) {
            return mapToDto(item);
        }

        // Doménové obchodní pravidlo: Kus v servisu se nesmí přímo půjčit ani rezervovat
        if (currentStatus == EquipmentStatus.IN_SERVICE && (targetStatus == EquipmentStatus.RENTED || targetStatus == EquipmentStatus.RESERVED)) {
            throw new BusinessValidationException("Kus je aktuálně v servisu a nelze jej zapůjčit ani rezervovat. Nejprve ukončete servis (převod do VOLNÉ).");
        }

        // Doménové pravidlo: Kus, který je půjčený venku, nelze přepnout do servisu bez předchozího vrácení
        if (currentStatus == EquipmentStatus.RENTED && targetStatus == EquipmentStatus.IN_SERVICE) {
            throw new BusinessValidationException("Kus je aktuálně zapůjčen zákazníkovi a nelze jej přímo odeslat do servisu. Nejprve musí být vrácen.");
        }

        item.setStatus(targetStatus);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            item.setConditionState(item.getConditionState() + " [Pozn.: " + request.getNote().trim() + "]");
        }

        EquipmentItem updated = itemRepository.save(item);
        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentCategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> new EquipmentCategoryDto(cat.getId(), cat.getCode(), cat.getName(), cat.getDescription()))
                .collect(Collectors.toList());
    }

    private EquipmentItemDto mapToDto(EquipmentItem item) {
        return new EquipmentItemDto(
                item.getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getCategory().getCode(),
                item.getInventoryNumber(),
                item.getSize(),
                item.getPurchaseYear(),
                item.getConditionState(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
