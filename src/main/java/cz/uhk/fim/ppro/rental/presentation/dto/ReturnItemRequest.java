package cz.uhk.fim.ppro.rental.presentation.dto;

import java.math.BigDecimal;

public class ReturnItemRequest {

    private boolean damaged = false;
    private String damageDescription;
    private BigDecimal damagePenaltyFee = BigDecimal.ZERO;
    private String note;

    public ReturnItemRequest() {}

    public boolean isDamaged() { return damaged; }
    public void setDamaged(boolean damaged) { this.damaged = damaged; }
    public String getDamageDescription() { return damageDescription; }
    public void setDamageDescription(String damageDescription) { this.damageDescription = damageDescription; }
    public BigDecimal getDamagePenaltyFee() { return damagePenaltyFee; }
    public void setDamagePenaltyFee(BigDecimal damagePenaltyFee) { this.damagePenaltyFee = damagePenaltyFee; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
