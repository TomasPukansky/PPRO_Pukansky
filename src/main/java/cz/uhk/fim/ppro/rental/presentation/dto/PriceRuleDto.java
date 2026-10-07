package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.PriceRule;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceRuleDto {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal dailyRate;
    private BigDecimal weekendRate;
    private BigDecimal depositRate;
    private LocalDate validFrom;

    public PriceRuleDto() {}

    public PriceRuleDto(PriceRule p) {
        this.id = p.getId();
        this.categoryId = p.getCategory().getId();
        this.categoryName = p.getCategory().getName();
        this.dailyRate = p.getDailyRate();
        this.weekendRate = p.getWeekendRate();
        this.depositRate = p.getDepositRate();
        this.validFrom = p.getValidFrom();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public BigDecimal getWeekendRate() { return weekendRate; }
    public void setWeekendRate(BigDecimal weekendRate) { this.weekendRate = weekendRate; }
    public BigDecimal getDepositRate() { return depositRate; }
    public void setDepositRate(BigDecimal depositRate) { this.depositRate = depositRate; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
}
