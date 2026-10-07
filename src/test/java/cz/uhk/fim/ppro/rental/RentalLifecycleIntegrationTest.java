package cz.uhk.fim.ppro.rental;

import cz.uhk.fim.ppro.rental.application.exception.BusinessValidationException;
import cz.uhk.fim.ppro.rental.application.service.RentalService;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentCategory;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentItem;
import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import cz.uhk.fim.ppro.rental.domain.model.OrderStatus;
import cz.uhk.fim.ppro.rental.domain.model.PriceRule;
import cz.uhk.fim.ppro.rental.domain.repository.EquipmentCategoryRepository;
import cz.uhk.fim.ppro.rental.domain.repository.EquipmentItemRepository;
import cz.uhk.fim.ppro.rental.domain.repository.PriceRuleRepository;
import cz.uhk.fim.ppro.rental.presentation.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RentalLifecycleIntegrationTest {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private EquipmentCategoryRepository categoryRepository;

    @Autowired
    private EquipmentItemRepository itemRepository;

    @Autowired
    private PriceRuleRepository priceRuleRepository;

    private Long skiCategoryId;
    private Long bootCategoryId;

    /** Testovací profil běží bez Flyway seedu – inventář si test zakládá sám. */
    @BeforeEach
    void setUpInventory() {
        EquipmentCategory skis = categoryRepository.save(new EquipmentCategory(null, "IT_SKI", "Lyže (test)", null));
        EquipmentCategory boots = categoryRepository.save(new EquipmentCategory(null, "IT_BOOT", "Boty (test)", null));
        skiCategoryId = skis.getId();
        bootCategoryId = boots.getId();

        priceRuleRepository.save(new PriceRule(skis, new BigDecimal("350"), new BigDecimal("600"), new BigDecimal("1000")));
        priceRuleRepository.save(new PriceRule(boots, new BigDecimal("150"), new BigDecimal("260"), new BigDecimal("500")));

        itemRepository.save(new EquipmentItem(skis, "IT-SKI-1", "170 cm", 2024, "Výborný", EquipmentStatus.AVAILABLE));
        itemRepository.save(new EquipmentItem(skis, "IT-SKI-2", "170 cm", 2023, "Dobrý", EquipmentStatus.AVAILABLE));
        itemRepository.save(new EquipmentItem(boots, "IT-BOOT-42", "42 EU", 2024, "Výborný", EquipmentStatus.AVAILABLE));
    }

    private CreateReservationRequest reservation(OffsetDateTime start, OffsetDateTime end, RequestedItemDto... items) {
        CreateReservationRequest request = new CreateReservationRequest();
        CustomerDto customer = new CustomerDto();
        customer.setFirstName("Petr");
        customer.setLastName("Testovací");
        customer.setPhone("+420777999888");
        customer.setIdCardNumber("99887766");
        request.setNewCustomer(customer);
        request.setStartsAt(start);
        request.setExpectedEndAt(end);
        request.setRequestedItems(List.of(items));
        return request;
    }

    @Test
    @DisplayName("Integrační test: Telefonická rezervace -> Výměna kusu u pultu -> Výdej -> Částečné vrácení -> Uzavření")
    void shouldExecuteFullRentalLifecycle() {
        // 1. Zákazník volá v úterý -> rezervace typ + velikost, systém sám přidělí konkrétní kusy
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = OffsetDateTime.now().plusDays(4);
        RentalOrderDto created = rentalService.createReservation(reservation(start, end,
                new RequestedItemDto(skiCategoryId, "170 cm"),
                new RequestedItemDto(bootCategoryId, "42 EU")));
        assertEquals(OrderStatus.RESERVATION, created.getStatus());
        assertEquals(2, created.getItems().size());

        RentalItemDetailDto skiLine = created.getItems().stream()
                .filter(i -> i.getInventoryNumber().startsWith("IT-SKI")).findFirst().orElseThrow();
        RentalItemDetailDto bootLine = created.getItems().stream()
                .filter(i -> i.getInventoryNumber().startsWith("IT-BOOT")).findFirst().orElseThrow();

        // 2. Sobota ráno u pultu -> zákazník chce jiný pár lyží (výměna kusu)
        List<EquipmentItemDto> swaps = rentalService.getAvailableItemsForSwap(skiLine.getId());
        assertFalse(swaps.isEmpty());
        assertTrue(swaps.stream().noneMatch(i -> i.getId().equals(skiLine.getItemId())), "Nabídka výměny nesmí obsahovat aktuální kus");
        Long newSkiId = swaps.get(0).getId();
        created = rentalService.swapItem(created.getId(), skiLine.getId(), newSkiId);

        // 3. Výdej se složenou kaucí
        HandoutOrderRequest handout = new HandoutOrderRequest();
        handout.setDepositAmount(new BigDecimal("1000"));
        RentalOrderDto active = rentalService.handoutOrder(created.getId(), handout);
        assertEquals(OrderStatus.ACTIVE, active.getStatus());
        assertEquals(EquipmentStatus.RENTED, itemRepository.findById(newSkiId).orElseThrow().getStatus());

        // 4. Částečné vrácení: lyže v poledne, boty zůstávají venku -> výpůjčka zůstává otevřená
        RentalOrderDto partial = rentalService.returnSingleItem(active.getId(), skiLine.getId(), new ReturnItemRequest());
        assertEquals(OrderStatus.ACTIVE, partial.getStatus());
        assertNull(partial.getActualClosedAt());

        // 5. Vrácení posledního kusu -> výpůjčka se uzavře
        RentalOrderDto closed = rentalService.returnSingleItem(active.getId(), bootLine.getId(), new ReturnItemRequest());
        assertEquals(OrderStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getActualClosedAt());
    }

    @Test
    @DisplayName("Integrační test: Třetí rezervace stejných lyží na stejný termín je odmítnuta (žádný překryv)")
    void shouldRejectOverbooking() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(10);
        OffsetDateTime end = OffsetDateTime.now().plusDays(12);

        RentalOrderDto first = rentalService.createReservation(reservation(start, end, new RequestedItemDto(skiCategoryId, "170 cm")));
        RentalOrderDto second = rentalService.createReservation(reservation(start, end, new RequestedItemDto(skiCategoryId, "170 cm")));
        assertNotEquals(first.getItems().get(0).getItemId(), second.getItems().get(0).getItemId(),
                "Dvě rezervace ve stejném termínu musí dostat různé kusy");

        assertThrows(BusinessValidationException.class, () ->
                rentalService.createReservation(reservation(start.plusHours(5), end, new RequestedItemDto(skiCategoryId, "170 cm"))));
    }
}
