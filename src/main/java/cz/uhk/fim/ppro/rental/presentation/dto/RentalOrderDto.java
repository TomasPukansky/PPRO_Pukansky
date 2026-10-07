package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.OrderStatus;
import cz.uhk.fim.ppro.rental.domain.model.RentalOrder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RentalOrderDto {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerIdCard;
    private OrderStatus status;
    private OffsetDateTime startsAt;
    private OffsetDateTime expectedEndAt;
    private OffsetDateTime actualClosedAt;
    private BigDecimal depositAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private BigDecimal totalLateFee;
    private String note;
    private List<RentalItemDetailDto> items;
    private OffsetDateTime createdAt;

    public RentalOrderDto() {}

    public RentalOrderDto(RentalOrder o) {
        this.id = o.getId();
        this.orderNumber = o.getOrderNumber();
        this.customerId = o.getCustomer().getId();
        this.customerName = o.getCustomer().getFullName();
        this.customerPhone = o.getCustomer().getPhone();
        this.customerIdCard = o.getCustomer().getIdCardNumber();
        this.status = o.getStatus();
        this.startsAt = o.getStartsAt();
        this.expectedEndAt = o.getExpectedEndAt();
        this.actualClosedAt = o.getActualClosedAt();
        this.depositAmount = o.getDepositAmount();
        this.discountAmount = o.getDiscountAmount();
        this.totalPrice = o.getTotalPrice();
        this.totalLateFee = o.getTotalLateFee();
        this.note = o.getNote();
        this.createdAt = o.getCreatedAt();
        if (o.getItems() != null) {
            this.items = o.getItems().stream().map(RentalItemDetailDto::new).collect(Collectors.toList());
        }
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerIdCard() { return customerIdCard; }
    public OrderStatus getStatus() { return status; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getExpectedEndAt() { return expectedEndAt; }
    public OffsetDateTime getActualClosedAt() { return actualClosedAt; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BigDecimal getTotalLateFee() { return totalLateFee; }
    public String getNote() { return note; }
    public List<RentalItemDetailDto> getItems() { return items; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
