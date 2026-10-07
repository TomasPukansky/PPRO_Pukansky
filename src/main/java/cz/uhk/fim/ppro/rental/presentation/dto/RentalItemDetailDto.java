package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.RentalItem;
import cz.uhk.fim.ppro.rental.domain.model.RentalItemStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class RentalItemDetailDto {
    private Long id;
    private Long itemId;
    private String inventoryNumber;
    private String categoryName;
    private String size;
    private BigDecimal dailyRateSnapshot;
    private BigDecimal rentalPrice;
    private OffsetDateTime returnedAt;
    private RentalItemStatus status;
    private BigDecimal lateFee;
    private BigDecimal damageFee;
    private String note;

    public RentalItemDetailDto() {}

    public RentalItemDetailDto(RentalItem ri) {
        this.id = ri.getId();
        this.itemId = ri.getItem().getId();
        this.inventoryNumber = ri.getItem().getInventoryNumber();
        this.categoryName = ri.getItem().getCategory().getName();
        this.size = ri.getItem().getSize();
        this.dailyRateSnapshot = ri.getDailyRateSnapshot();
        this.rentalPrice = ri.getRentalPrice();
        this.returnedAt = ri.getReturnedAt();
        this.status = ri.getStatus();
        this.lateFee = ri.getLateFee();
        this.damageFee = ri.getDamageFee();
        this.note = ri.getNote();
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getInventoryNumber() { return inventoryNumber; }
    public String getCategoryName() { return categoryName; }
    public String getSize() { return size; }
    public BigDecimal getDailyRateSnapshot() { return dailyRateSnapshot; }
    public BigDecimal getRentalPrice() { return rentalPrice; }
    public OffsetDateTime getReturnedAt() { return returnedAt; }
    public RentalItemStatus getStatus() { return status; }
    public BigDecimal getLateFee() { return lateFee; }
    public BigDecimal getDamageFee() { return damageFee; }
    public String getNote() { return note; }
}
