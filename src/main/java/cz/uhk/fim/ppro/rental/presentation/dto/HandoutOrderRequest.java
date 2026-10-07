package cz.uhk.fim.ppro.rental.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class HandoutOrderRequest {

    @NotNull(message = "Výše kauce v hotovosti je povinná")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String note;

    public HandoutOrderRequest() {}

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
