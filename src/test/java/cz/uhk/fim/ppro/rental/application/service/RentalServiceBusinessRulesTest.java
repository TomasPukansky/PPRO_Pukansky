package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.application.exception.BusinessValidationException;
import cz.uhk.fim.ppro.rental.domain.model.*;
import cz.uhk.fim.ppro.rental.domain.repository.*;
import cz.uhk.fim.ppro.rental.presentation.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceBusinessRulesTest {

    @Mock
    private RentalOrderRepository orderRepository;
    @Mock
    private RentalItemRepository rentalItemRepository;
    @Mock
    private EquipmentItemRepository itemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PriceRuleRepository priceRuleRepository;
    @Mock
    private ServiceLogRepository serviceLogRepository;
    @Mock
    private DamageRecordRepository damageRecordRepository;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private Customer customer;
    private EquipmentCategory category;
    private EquipmentItem item1;
    private PriceRule priceRule;

    @BeforeEach
    void setUp() {
        customer = new Customer("Jan", "Novák", "+420777111222", "123456", "Poznámka");
        customer.setId(1L);

        category = new EquipmentCategory(1L, "SKI_CARV", "Sjezdové lyže", "Test");
        item1 = new EquipmentItem(category, "SKI-001", "170 cm", 2023, "Výborný stav", EquipmentStatus.AVAILABLE);
        item1.setId(10L);

        priceRule = new PriceRule(category, BigDecimal.valueOf(350.00), BigDecimal.valueOf(600.00), BigDecimal.valueOf(1000.00));
    }

    @Test
    @DisplayName("Pravidlo: Žádný překryv – kus nesmí jít rezervovat, pokud se v daném intervalu překrývá s jinou výpůjčkou")
    void shouldRejectReservationWhenDatesOverlap() {
        OffsetDateTime start = OffsetDateTime.of(2026, 10, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 10, 12, 18, 0, 0, 0, ZoneOffset.UTC);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item1));

        // Simulace existujícího překryvu
        RentalOrder existingOrder = new RentalOrder("REZ-2026-0001", customer, OrderStatus.RESERVATION, start, end);
        RentalItem existingItem = new RentalItem(existingOrder, item1, BigDecimal.valueOf(350), BigDecimal.valueOf(700), RentalItemStatus.RESERVED);
        when(rentalItemRepository.findOverlappingActiveItems(eq(10L), eq(start), eq(end), isNull()))
                .thenReturn(List.of(existingItem));

        CreateReservationRequest request = new CreateReservationRequest();
        request.setCustomerId(1L);
        request.setStartsAt(start);
        request.setExpectedEndAt(end);
        RequestedItemDto reqItem = new RequestedItemDto(1L, "170 cm");
        reqItem.setSpecificItemId(10L);
        request.setRequestedItems(List.of(reqItem));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> rentalService.createReservation(request));
        assertTrue(ex.getMessage().contains("již rezervován/půjčen"));
    }

    @Test
    @DisplayName("Pravidlo: Kus v servisu se nesmí rezervovat ani půjčit")
    void shouldRejectReservationWhenItemIsInService() {
        item1.setStatus(EquipmentStatus.IN_SERVICE);

        OffsetDateTime start = OffsetDateTime.of(2026, 10, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 10, 12, 18, 0, 0, 0, ZoneOffset.UTC);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item1));

        CreateReservationRequest request = new CreateReservationRequest();
        request.setCustomerId(1L);
        request.setStartsAt(start);
        request.setExpectedEndAt(end);
        RequestedItemDto reqItem = new RequestedItemDto(1L, "170 cm");
        reqItem.setSpecificItemId(10L);
        request.setRequestedItems(List.of(reqItem));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> rentalService.createReservation(request));
        assertTrue(ex.getMessage().contains("je v servisu"));
    }

    @Test
    @DisplayName("Pravidlo: Úspěšné vytvoření rezervace po telefonu s auto-alokací volného kusu")
    void shouldCreateReservationWithAutoAllocation() {
        OffsetDateTime start = OffsetDateTime.of(2026, 10, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 10, 11, 18, 0, 0, 0, ZoneOffset.UTC);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(itemRepository.findByCategoryId(1L)).thenReturn(List.of(item1));
        when(rentalItemRepository.findOverlappingActiveItems(eq(10L), eq(start), eq(end), isNull()))
                .thenReturn(Collections.emptyList());
        when(priceRuleRepository.findFirstByCategoryIdOrderByValidFromDesc(1L)).thenReturn(Optional.of(priceRule));
        when(orderRepository.save(any(RentalOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReservationRequest request = new CreateReservationRequest();
        request.setCustomerId(1L);
        request.setStartsAt(start);
        request.setExpectedEndAt(end);
        request.setRequestedItems(List.of(new RequestedItemDto(1L, "170 cm")));

        RentalOrderDto result = rentalService.createReservation(request);

        assertNotNull(result);
        assertEquals(OrderStatus.RESERVATION, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("SKI-001", result.getItems().get(0).getInventoryNumber());
        assertEquals(BigDecimal.valueOf(350.00), result.getItems().get(0).getDailyRateSnapshot());
    }

    @Test
    @DisplayName("Pravidlo: Částečné vrácení a odeslání poškozeného kusu do servisu")
    void shouldHandlePartialReturnWithDamage() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(2);
        OffsetDateTime end = OffsetDateTime.now().plusDays(1);

        RentalOrder order = new RentalOrder("VYP-2026-0001", customer, OrderStatus.ACTIVE, start, end);
        order.setId(100L);

        RentalItem ri1 = new RentalItem(order, item1, BigDecimal.valueOf(350), BigDecimal.valueOf(700), RentalItemStatus.RENTED);
        ri1.setId(501L);
        order.addItem(ri1);

        EquipmentItem item2 = new EquipmentItem(category, "BOT-042", "42 EU", 2023, "Stav ok", EquipmentStatus.RENTED);
        item2.setId(20L);
        RentalItem ri2 = new RentalItem(order, item2, BigDecimal.valueOf(150), BigDecimal.valueOf(300), RentalItemStatus.RENTED);
        ri2.setId(502L);
        order.addItem(ri2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(RentalOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        // Vrátíme pouze lyže (ri1) s poškozením
        ReturnItemRequest returnRequest = new ReturnItemRequest();
        returnRequest.setDamaged(true);
        returnRequest.setDamageDescription("Zlomená špička");
        returnRequest.setDamagePenaltyFee(BigDecimal.valueOf(500.00));

        RentalOrderDto result = rentalService.returnSingleItem(100L, 501L, returnRequest);

        // Objednávka zůstává ACTIVE, protože boty (ri2) ještě nebyly vráceny!
        assertEquals(OrderStatus.ACTIVE, result.getStatus());
        assertEquals(EquipmentStatus.IN_SERVICE, item1.getStatus());
        verify(damageRecordRepository, times(1)).save(any(DamageRecord.class));
        verify(serviceLogRepository, times(1)).save(any(ServiceLog.class));
    }
}
