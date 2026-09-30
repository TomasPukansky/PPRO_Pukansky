package cz.uhk.fim.ppro.rental.domain;

import cz.uhk.fim.ppro.rental.application.exception.BusinessValidationException;
import cz.uhk.fim.ppro.rental.application.exception.DuplicateInventoryNumberException;
import cz.uhk.fim.ppro.rental.application.service.EquipmentItemServiceImpl;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentCategory;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentItem;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import cz.uhk.fim.ppro.rental.domain.repository.EquipmentCategoryRepository;
import cz.uhk.fim.ppro.rental.domain.repository.EquipmentItemRepository;
import cz.uhk.fim.ppro.rental.presentation.dto.CreateEquipmentItemRequest;
import cz.uhk.fim.ppro.rental.presentation.dto.EquipmentItemDto;
import cz.uhk.fim.ppro.rental.presentation.dto.UpdateEquipmentStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentBusinessRulesTest {

    @Mock
    private EquipmentItemRepository itemRepository;

    @Mock
    private EquipmentCategoryRepository categoryRepository;

    @InjectMocks
    private EquipmentItemServiceImpl itemService;

    private EquipmentCategory sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new EquipmentCategory(1L, "SKI_CARV", "Sjezdové lyže", "Popis");
    }

    @Test
    @DisplayName("Pravidlo: Unikátnost inventárního čísla – nelze vytvořit dva kusy se stejným číslem")
    void shouldRejectDuplicateInventoryNumber() {
        when(itemRepository.existsByInventoryNumber("SKI-001")).thenReturn(true);

        CreateEquipmentItemRequest request = new CreateEquipmentItemRequest(
                1L, "SKI-001", "170 cm", 2024, "Nové", EquipmentStatus.AVAILABLE
        );

        assertThrows(DuplicateInventoryNumberException.class, () -> itemService.createItem(request));
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pravidlo: Kus v servisu nelze přímo zapůjčit (status RENTED)")
    void shouldRejectRentingItemInService() {
        EquipmentItem inServiceItem = new EquipmentItem(sampleCategory, "SKI-004", "160 cm", 2022, "Poškozeno", EquipmentStatus.IN_SERVICE);
        inServiceItem.setId(4L);

        when(itemRepository.findById(4L)).thenReturn(Optional.of(inServiceItem));

        UpdateEquipmentStatusRequest request = new UpdateEquipmentStatusRequest(EquipmentStatus.RENTED, "Pokus o půjčení");

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> itemService.updateStatus(4L, request));
        assertTrue(ex.getMessage().contains("v servisu a nelze jej zapůjčit"));
    }

    @Test
    @DisplayName("Pravidlo: Kus v servisu nelze zarezervovat (status RESERVED)")
    void shouldRejectReservingItemInService() {
        EquipmentItem inServiceItem = new EquipmentItem(sampleCategory, "SKI-004", "160 cm", 2022, "Poškozeno", EquipmentStatus.IN_SERVICE);
        inServiceItem.setId(4L);

        when(itemRepository.findById(4L)).thenReturn(Optional.of(inServiceItem));

        UpdateEquipmentStatusRequest request = new UpdateEquipmentStatusRequest(EquipmentStatus.RESERVED, "Pokus o rezervaci");

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> itemService.updateStatus(4L, request));
        assertTrue(ex.getMessage().contains("v servisu"));
    }

    @Test
    @DisplayName("Pravidlo: Kus, který je půjčený venku (RENTED), nelze rovnou poslat do servisu")
    void shouldRejectSendingRentedItemToServiceWithoutReturn() {
        EquipmentItem rentedItem = new EquipmentItem(sampleCategory, "SKI-002", "170 cm", 2023, "V pořádku", EquipmentStatus.RENTED);
        rentedItem.setId(2L);

        when(itemRepository.findById(2L)).thenReturn(Optional.of(rentedItem));

        UpdateEquipmentStatusRequest request = new UpdateEquipmentStatusRequest(EquipmentStatus.IN_SERVICE, "Chci servis");

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> itemService.updateStatus(2L, request));
        assertTrue(ex.getMessage().contains("zapůjčen zákazníkovi"));
    }

    @Test
    @DisplayName("Pravidlo: Volný kus lze řádně odeslat do servisu a zaznamenat důvod")
    void shouldSuccessfullySendAvailableItemToService() {
        EquipmentItem availableItem = new EquipmentItem(sampleCategory, "SKI-001", "170 cm", 2023, "V pořádku", EquipmentStatus.AVAILABLE);
        availableItem.setId(1L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(availableItem));
        when(itemRepository.save(any(EquipmentItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEquipmentStatusRequest request = new UpdateEquipmentStatusRequest(EquipmentStatus.IN_SERVICE, "Tupé hrany");
        EquipmentItemDto result = itemService.updateStatus(1L, request);

        assertEquals(EquipmentStatus.IN_SERVICE, result.getStatus());
        assertTrue(result.getConditionState().contains("Tupé hrany"));
    }

    @Test
    @DisplayName("Pravidlo: Kus po servisu lze vrátit zpět do fondu volných kusů (AVAILABLE)")
    void shouldSuccessfullyReturnItemFromServiceToAvailable() {
        EquipmentItem inServiceItem = new EquipmentItem(sampleCategory, "SKI-001", "170 cm", 2023, "Poškozeno", EquipmentStatus.IN_SERVICE);
        inServiceItem.setId(1L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(inServiceItem));
        when(itemRepository.save(any(EquipmentItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEquipmentStatusRequest request = new UpdateEquipmentStatusRequest(EquipmentStatus.AVAILABLE, "Servis hotov, nabroušeno");
        EquipmentItemDto result = itemService.updateStatus(1L, request);

        assertEquals(EquipmentStatus.AVAILABLE, result.getStatus());
    }
}
