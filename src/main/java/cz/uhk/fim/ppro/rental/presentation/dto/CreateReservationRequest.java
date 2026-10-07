package cz.uhk.fim.ppro.rental.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreateReservationRequest {

    private Long customerId;
    private CustomerDto newCustomer; // pokud zákazník ještě v DB není

    @NotNull(message = "Začátek rezervace je povinný")
    private OffsetDateTime startsAt;

    @NotNull(message = "Konec rezervace je povinný")
    private OffsetDateTime expectedEndAt;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String note;

    @NotNull(message = "Musí být vybrána alespoň jedna položka")
    private List<RequestedItemDto> requestedItems = new ArrayList<>();

    public CreateReservationRequest() {}

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public CustomerDto getNewCustomer() { return newCustomer; }
    public void setNewCustomer(CustomerDto newCustomer) { this.newCustomer = newCustomer; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getExpectedEndAt() { return expectedEndAt; }
    public void setExpectedEndAt(OffsetDateTime expectedEndAt) { this.expectedEndAt = expectedEndAt; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<RequestedItemDto> getRequestedItems() { return requestedItems; }
    public void setRequestedItems(List<RequestedItemDto> requestedItems) { this.requestedItems = requestedItems; }
}
