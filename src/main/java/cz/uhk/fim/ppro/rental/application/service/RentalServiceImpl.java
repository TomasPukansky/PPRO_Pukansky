package cz.uhk.fim.ppro.rental.application.service;

import cz.uhk.fim.ppro.rental.application.exception.BusinessValidationException;
import cz.uhk.fim.ppro.rental.application.exception.ResourceNotFoundException;
import cz.uhk.fim.ppro.rental.domain.model.*;
import cz.uhk.fim.ppro.rental.domain.repository.*;
import cz.uhk.fim.ppro.rental.presentation.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RentalServiceImpl implements RentalService {

    private final RentalOrderRepository orderRepository;
    private final RentalItemRepository rentalItemRepository;
    private final EquipmentItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final ServiceLogRepository serviceLogRepository;
    private final DamageRecordRepository damageRecordRepository;

    public RentalServiceImpl(
            RentalOrderRepository orderRepository,
            RentalItemRepository rentalItemRepository,
            EquipmentItemRepository itemRepository,
            CustomerRepository customerRepository,
            PriceRuleRepository priceRuleRepository,
            ServiceLogRepository serviceLogRepository,
            DamageRecordRepository damageRecordRepository) {
        this.orderRepository = orderRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.itemRepository = itemRepository;
        this.customerRepository = customerRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.serviceLogRepository = serviceLogRepository;
        this.damageRecordRepository = damageRecordRepository;
    }

    @Override
    public RentalOrderDto createReservation(CreateReservationRequest request) {
        if (!request.getExpectedEndAt().isAfter(request.getStartsAt())) {
            throw new BusinessValidationException("Konec výpůjčky musí být po jejím začátku.");
        }

        // Zákazník: buď existující nebo vytvoříme nového
        Customer customer;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zákazník s ID " + request.getCustomerId() + " nebyl nalezen."));
        } else if (request.getNewCustomer() != null) {
            CustomerDto nc = request.getNewCustomer();
            customer = new Customer(nc.getFirstName(), nc.getLastName(), nc.getPhone(), nc.getIdCardNumber(), nc.getNote());
            customer = customerRepository.save(customer);
        } else {
            throw new BusinessValidationException("Zákazník musí být vybrán nebo nově zadán.");
        }

        String orderNumber = "REZ-" + LocalDate.now().getYear() + "-" + String.format("%04d", (orderRepository.count() + 1));
        RentalOrder order = new RentalOrder(orderNumber, customer, OrderStatus.RESERVATION, request.getStartsAt(), request.getExpectedEndAt());
        order.setDiscountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO);
        order.setNote(request.getNote());

        BigDecimal runningTotal = BigDecimal.ZERO;

        for (RequestedItemDto reqItem : request.getRequestedItems()) {
            EquipmentItem item;
            if (reqItem.getSpecificItemId() != null) {
                item = itemRepository.findById(reqItem.getSpecificItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Požadovaný kus nebyl nalezen."));
                validateItemAvailabilityForPeriod(item, request.getStartsAt(), request.getExpectedEndAt(), null);
            } else {
                // Automatická alokace volného kusu dané kategorie a velikosti
                item = autoAllocateItem(reqItem.getCategoryId(), reqItem.getSize(), request.getStartsAt(), request.getExpectedEndAt());
            }

            PriceRule rule = priceRuleRepository.findFirstByCategoryIdOrderByValidFromDesc(item.getCategory().getId())
                    .orElseThrow(() -> new BusinessValidationException("Pro kategorii '" + item.getCategory().getName() + "' není definován platný ceník."));

            BigDecimal itemPrice = PricingPolicy.calculateItemPrice(rule, request.getStartsAt(), request.getExpectedEndAt());

            RentalItem rentalItem = new RentalItem(
                    order,
                    item,
                    rule.getDailyRate(),
                    itemPrice,
                    RentalItemStatus.RESERVED
            );

            // Pokud je rezervace na dnešek nebo začíná ihned, kus se označí jako RESERVED
            if (item.getStatus() == EquipmentStatus.AVAILABLE) {
                item.setStatus(EquipmentStatus.RESERVED);
                itemRepository.save(item);
            }

            order.addItem(rentalItem);
            runningTotal = runningTotal.add(itemPrice);
        }

        BigDecimal netTotal = runningTotal.subtract(order.getDiscountAmount());
        order.setTotalPrice(netTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : netTotal);

        RentalOrder saved = orderRepository.save(order);
        return new RentalOrderDto(saved);
    }

    @Override
    public RentalOrderDto handoutOrder(Long orderId, HandoutOrderRequest request) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Objednávka " + orderId + " nenalezena."));

        if (order.getStatus() != OrderStatus.RESERVATION) {
            throw new BusinessValidationException("Lze vydat pouze objednávku ve stavu RESERVATION. Aktuální stav: " + order.getStatus());
        }

        // Fyzická kontrola u pultu: kus, který je právě venku nebo v servisu, nelze vydat – obsluha ho musí vyměnit
        for (RentalItem ri : order.getItems()) {
            EquipmentItem eq = ri.getItem();
            if (ri.getStatus() == RentalItemStatus.RESERVED
                    && (eq.getStatus() == EquipmentStatus.RENTED || eq.getStatus() == EquipmentStatus.IN_SERVICE)) {
                throw new BusinessValidationException("Kus " + eq.getInventoryNumber() + " je právě "
                        + (eq.getStatus() == EquipmentStatus.RENTED ? "venku u jiného zákazníka" : "v servisu")
                        + " – vyměňte ho za jiný volný kus a vydejte znovu.");
            }
        }

        order.setStatus(OrderStatus.ACTIVE);
        order.setDepositAmount(request.getDepositAmount());
        if (request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            order.setDiscountAmount(request.getDiscountAmount());
            BigDecimal recalculated = order.getItems().stream()
                    .map(RentalItem::getRentalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .subtract(order.getDiscountAmount());
            order.setTotalPrice(recalculated.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : recalculated);
        }
        if (request.getNote() != null) {
            order.setNote((order.getNote() != null ? order.getNote() + " | " : "") + request.getNote());
        }

        for (RentalItem ri : order.getItems()) {
            if (ri.getStatus() == RentalItemStatus.RESERVED) {
                ri.setStatus(RentalItemStatus.RENTED);
                EquipmentItem eq = ri.getItem();
                eq.setStatus(EquipmentStatus.RENTED);
                itemRepository.save(eq);
            }
        }

        return new RentalOrderDto(orderRepository.save(order));
    }

    @Override
    public RentalOrderDto returnSingleItem(Long orderId, Long rentalItemId, ReturnItemRequest request) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Objednávka " + orderId + " nenalezena."));

        RentalItem rentalItem = order.getItems().stream()
                .filter(i -> i.getId().equals(rentalItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Položka " + rentalItemId + " v objednávce nenalezena."));

        if (rentalItem.getStatus() == RentalItemStatus.RETURNED || rentalItem.getStatus() == RentalItemStatus.DAMAGED) {
            throw new BusinessValidationException("Tento kus již byl vrácen.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        rentalItem.setReturnedAt(now);

        // Výpočet penále, pokud je vráceno po termínu
        if (now.isAfter(order.getExpectedEndAt())) {
            BigDecimal lateFee = PricingPolicy.calculateLateFee(rentalItem.getDailyRateSnapshot(), order.getExpectedEndAt(), now);
            rentalItem.setLateFee(lateFee);
            order.setTotalLateFee(order.getTotalLateFee().add(lateFee));
        }

        EquipmentItem eq = rentalItem.getItem();

        if (request.isDamaged()) {
            rentalItem.setStatus(RentalItemStatus.DAMAGED);
            rentalItem.setDamageFee(request.getDamagePenaltyFee() != null ? request.getDamagePenaltyFee() : BigDecimal.ZERO);

            // Záznam škody
            DamageRecord damage = new DamageRecord(
                    rentalItem,
                    eq,
                    order.getCustomer(),
                    request.getDamageDescription() != null ? request.getDamageDescription() : "Poškození při vrácení",
                    rentalItem.getDamageFee(),
                    true
            );
            damageRecordRepository.save(damage);

            // Kus jde automaticky do servisu!
            eq.setStatus(EquipmentStatus.IN_SERVICE);
            eq.setConditionState("Poškozeno: " + request.getDamageDescription());
            itemRepository.save(eq);

            ServiceLog serviceLog = new ServiceLog(
                    eq,
                    now,
                    "Oprava poškození po výpůjčce " + order.getOrderNumber() + ": " + request.getDamageDescription(),
                    BigDecimal.ZERO
            );
            serviceLogRepository.save(serviceLog);
        } else {
            rentalItem.setStatus(RentalItemStatus.RETURNED);
            eq.setStatus(EquipmentStatus.AVAILABLE);
            itemRepository.save(eq);
        }

        if (request.getNote() != null) {
            rentalItem.setNote(request.getNote());
        }

        rentalItemRepository.save(rentalItem);

        // Zjištění, zda jsou již všechny položky vráceny
        boolean allReturned = order.getItems().stream()
                .allMatch(i -> i.getStatus() == RentalItemStatus.RETURNED || i.getStatus() == RentalItemStatus.DAMAGED);

        if (allReturned) {
            order.setStatus(OrderStatus.CLOSED);
            order.setActualClosedAt(now);
        }

        return new RentalOrderDto(orderRepository.save(order));
    }

    @Override
    public RentalOrderDto swapItem(Long orderId, Long rentalItemId, Long newItemId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Objednávka " + orderId + " nenalezena."));

        if (order.getStatus() != OrderStatus.RESERVATION && order.getStatus() != OrderStatus.ACTIVE) {
            throw new BusinessValidationException("Výměnu kusu lze provést pouze u rezervace nebo aktivní výpůjčky.");
        }

        RentalItem rentalItem = order.getItems().stream()
                .filter(i -> i.getId().equals(rentalItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Položka výpůjčky " + rentalItemId + " nenalezena."));

        EquipmentItem oldItem = rentalItem.getItem();
        EquipmentItem newItem = itemRepository.findById(newItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Nový kus " + newItemId + " nenalezen."));

        // Ověření dostupnosti nového kusu v termínu výpůjčky
        validateItemAvailabilityForPeriod(newItem, order.getStartsAt(), order.getExpectedEndAt(), order.getId());

        // Uvolnění starého kusu
        oldItem.setStatus(EquipmentStatus.AVAILABLE);
        itemRepository.save(oldItem);

        // Přiřazení nového kusu
        newItem.setStatus(order.getStatus() == OrderStatus.ACTIVE ? EquipmentStatus.RENTED : EquipmentStatus.RESERVED);
        itemRepository.save(newItem);

        rentalItem.setItem(newItem);
        rentalItemRepository.save(rentalItem);

        return new RentalOrderDto(orderRepository.save(order));
    }

    @Override
    public RentalOrderDto cancelOrder(Long orderId, String reason) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Objednávka " + orderId + " nenalezena."));

        if (order.getStatus() == OrderStatus.CLOSED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessValidationException("Tuto objednávku již nelze zrušit.");
        }

        // Uvolnění alokovaných kusů
        for (RentalItem ri : order.getItems()) {
            EquipmentItem eq = ri.getItem();
            if (eq.getStatus() == EquipmentStatus.RESERVED || eq.getStatus() == EquipmentStatus.RENTED) {
                eq.setStatus(EquipmentStatus.AVAILABLE);
                itemRepository.save(eq);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setNote((order.getNote() != null ? order.getNote() + " | " : "") + "Zrušeno: " + reason);
        return new RentalOrderDto(orderRepository.save(order));
    }

    @Override
    public int expireUnclaimedReservations() {
        OffsetDateTime now = OffsetDateTime.now();
        List<RentalOrder> unclaimed = orderRepository.findUnclaimedReservations(now);
        int expiredCount = 0;

        for (RentalOrder o : unclaimed) {
            o.setStatus(OrderStatus.EXPIRED);
            o.setNote((o.getNote() != null ? o.getNote() + " | " : "") + "Nevyzvednuto v termínu – expirováno v 10:00");
            for (RentalItem ri : o.getItems()) {
                EquipmentItem eq = ri.getItem();
                if (eq.getStatus() == EquipmentStatus.RESERVED) {
                    eq.setStatus(EquipmentStatus.AVAILABLE);
                    itemRepository.save(eq);
                }
            }
            orderRepository.save(o);
            expiredCount++;
        }
        return expiredCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalOrderDto> getOrders(String status) {
        List<RentalOrder> list;
        if (status != null && !status.isBlank()) {
            list = orderRepository.findByStatus(OrderStatus.valueOf(status.toUpperCase()));
        } else {
            list = orderRepository.findAll();
        }
        return list.stream().map(RentalOrderDto::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RentalOrderDto getOrderById(Long id) {
        RentalOrder o = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objednávka " + id + " nenalezena."));
        return new RentalOrderDto(o);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalOrderDto getOrderByNumber(String orderNumber) {
        RentalOrder o = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Objednávka " + orderNumber + " nenalezena."));
        return new RentalOrderDto(o);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentItemDto> getAvailableItemsForSwap(Long rentalItemId) {
        RentalItem ri = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Položka " + rentalItemId + " nenalezena."));

        EquipmentCategory cat = ri.getItem().getCategory();
        RentalOrder order = ri.getOrder();

        List<EquipmentItem> candidates = itemRepository.findByCategoryId(cat.getId());
        return candidates.stream()
                .filter(item -> !item.getId().equals(ri.getItem().getId()))
                .filter(item -> item.getStatus() != EquipmentStatus.IN_SERVICE && item.getStatus() != EquipmentStatus.RETIRED)
                .filter(item -> isItemAvailableForPeriod(item, order.getStartsAt(), order.getExpectedEndAt(), order.getId()))
                .map(this::mapItemToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalOrderDto> getActiveRentals() {
        return orderRepository.findByStatus(OrderStatus.ACTIVE).stream()
                .map(RentalOrderDto::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalOrderDto> getOverdueRentals() {
        return orderRepository.findOverdueOrders(OffsetDateTime.now()).stream()
                .map(RentalOrderDto::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getItemUsageReport() {
        List<EquipmentItem> allItems = itemRepository.findAll();
        List<RentalItem> allRentalItems = rentalItemRepository.findAll();

        Map<Long, List<RentalItem>> byItemId = allRentalItems.stream()
                .collect(Collectors.groupingBy(ri -> ri.getItem().getId()));

        List<Map<String, Object>> report = new ArrayList<>();
        for (EquipmentItem item : allItems) {
            List<RentalItem> usages = byItemId.getOrDefault(item.getId(), Collections.emptyList());
            Map<String, Object> row = new HashMap<>();
            row.put("itemId", item.getId());
            row.put("inventoryNumber", item.getInventoryNumber());
            row.put("categoryName", item.getCategory().getName());
            row.put("size", item.getSize());
            row.put("purchaseYear", item.getPurchaseYear());
            row.put("conditionState", item.getConditionState());
            row.put("status", item.getStatus());
            row.put("usageCount", usages.size());

            BigDecimal totalRevenue = usages.stream()
                    .map(RentalItem::getRentalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("totalRevenue", totalRevenue);

            report.add(row);
        }

        // Seřazení podle počtu výpůjček sestupně
        report.sort((a, b) -> Integer.compare((int) b.get("usageCount"), (int) a.get("usageCount")));
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailabilityMatrix(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        OffsetDateTime startOfDay = queryDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = queryDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<EquipmentItem> allItems = itemRepository.findAll();

        // Skupiny podle Category -> Size
        Map<String, Map<String, Map<String, Integer>>> matrix = new LinkedHashMap<>();

        for (EquipmentItem item : allItems) {
            String catName = item.getCategory().getName();
            String size = item.getSize();

            matrix.putIfAbsent(catName, new LinkedHashMap<>());
            matrix.get(catName).putIfAbsent(size, new HashMap<>(Map.of(
                    "total", 0,
                    "available", 0,
                    "in_service", 0,
                    "rented_or_reserved", 0
            )));

            Map<String, Integer> cell = matrix.get(catName).get(size);
            cell.put("total", cell.get("total") + 1);

            if (item.getStatus() == EquipmentStatus.IN_SERVICE) {
                cell.put("in_service", cell.get("in_service") + 1);
            } else if (isItemAvailableForPeriod(item, startOfDay, endOfDay, null)) {
                cell.put("available", cell.get("available") + 1);
            } else {
                cell.put("rented_or_reserved", cell.get("rented_or_reserved") + 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", queryDate);
        result.put("categories", matrix);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveServices() {
        List<ServiceLog> logs = serviceLogRepository.findByFinishedAtIsNullOrderByStartedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceLog log : logs) {
            Map<String, Object> row = new HashMap<>();
            row.put("serviceId", log.getId());
            row.put("itemId", log.getItem().getId());
            row.put("inventoryNumber", log.getItem().getInventoryNumber());
            row.put("categoryName", log.getItem().getCategory().getName());
            row.put("size", log.getItem().getSize());
            row.put("startedAt", log.getStartedAt());
            row.put("description", log.getDescription());
            result.add(row);
        }
        return result;
    }

    private EquipmentItem autoAllocateItem(Long categoryId, String size, OffsetDateTime start, OffsetDateTime end) {
        List<EquipmentItem> candidates = itemRepository.findByCategoryId(categoryId);

        // Nejprve zkusíme shodu velikosti
        List<EquipmentItem> matchingSize = candidates.stream()
                .filter(item -> size == null || size.isBlank() || item.getSize().equalsIgnoreCase(size.trim()))
                .filter(item -> item.getStatus() != EquipmentStatus.IN_SERVICE && item.getStatus() != EquipmentStatus.RETIRED)
                .filter(item -> isItemAvailableForPeriod(item, start, end, null))
                // Přednost mají kusy, které jsou teď fyzicky na skladě (ne venku u jiného zákazníka)
                .sorted(java.util.Comparator.comparing((EquipmentItem item) -> item.getStatus() != EquipmentStatus.AVAILABLE))
                .collect(Collectors.toList());

        if (!matchingSize.isEmpty()) {
            return matchingSize.get(0);
        }

        // Pokud není přesná velikost, ale velikost nebyla striktní
        if (size == null || size.isBlank()) {
            List<EquipmentItem> anyFree = candidates.stream()
                    .filter(item -> item.getStatus() != EquipmentStatus.IN_SERVICE && item.getStatus() != EquipmentStatus.RETIRED)
                    .filter(item -> isItemAvailableForPeriod(item, start, end, null))
                    .collect(Collectors.toList());
            if (!anyFree.isEmpty()) {
                return anyFree.get(0);
            }
        }

        throw new BusinessValidationException("V požadovaném termínu (" + start.toLocalDate() + " - " + end.toLocalDate() +
                ") není k dispozici žádný volný kus v dané kategorii/velikosti.");
    }

    private void validateItemAvailabilityForPeriod(EquipmentItem item, OffsetDateTime start, OffsetDateTime end, Long excludeOrderId) {
        if (item.getStatus() == EquipmentStatus.IN_SERVICE) {
            throw new BusinessValidationException("Kus " + item.getInventoryNumber() + " je v servisu a nelze jej vypůjčit.");
        }
        if (item.getStatus() == EquipmentStatus.RETIRED) {
            throw new BusinessValidationException("Kus " + item.getInventoryNumber() + " je vyřazen z provozu.");
        }

        List<RentalItem> overlaps = rentalItemRepository.findOverlappingActiveItems(item.getId(), start, end, excludeOrderId);
        if (!overlaps.isEmpty()) {
            RentalItem overlap = overlaps.get(0);
            throw new BusinessValidationException("Kus " + item.getInventoryNumber() + " je v daném termínu již rezervován/půjčen v objednávce " +
                    overlap.getOrder().getOrderNumber() + ".");
        }
    }

    private boolean isItemAvailableForPeriod(EquipmentItem item, OffsetDateTime start, OffsetDateTime end, Long excludeOrderId) {
        if (item.getStatus() == EquipmentStatus.IN_SERVICE || item.getStatus() == EquipmentStatus.RETIRED) {
            return false;
        }
        List<RentalItem> overlaps = rentalItemRepository.findOverlappingActiveItems(item.getId(), start, end, excludeOrderId);
        return overlaps.isEmpty();
    }

    private EquipmentItemDto mapItemToDto(EquipmentItem item) {
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
