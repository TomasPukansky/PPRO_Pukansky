package cz.uhk.fim.ppro.rental.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "rental_items")
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private EquipmentItem item;

    @Column(name = "daily_rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRateSnapshot;

    @Column(name = "rental_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentalPrice = BigDecimal.ZERO;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RentalItemStatus status = RentalItemStatus.RESERVED;

    @Column(name = "late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "damage_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal damageFee = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public RentalItem() {
    }

    public RentalItem(RentalOrder order, EquipmentItem item, BigDecimal dailyRateSnapshot, BigDecimal rentalPrice, RentalItemStatus status) {
        this.order = order;
        this.item = item;
        this.dailyRateSnapshot = dailyRateSnapshot;
        this.rentalPrice = rentalPrice;
        this.status = status != null ? status : RentalItemStatus.RESERVED;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RentalOrder getOrder() {
        return order;
    }

    public void setOrder(RentalOrder order) {
        this.order = order;
    }

    public EquipmentItem getItem() {
        return item;
    }

    public void setItem(EquipmentItem item) {
        this.item = item;
    }

    public BigDecimal getDailyRateSnapshot() {
        return dailyRateSnapshot;
    }

    public void setDailyRateSnapshot(BigDecimal dailyRateSnapshot) {
        this.dailyRateSnapshot = dailyRateSnapshot;
    }

    public BigDecimal getRentalPrice() {
        return rentalPrice;
    }

    public void setRentalPrice(BigDecimal rentalPrice) {
        this.rentalPrice = rentalPrice;
    }

    public OffsetDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(OffsetDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public RentalItemStatus getStatus() {
        return status;
    }

    public void setStatus(RentalItemStatus status) {
        this.status = status;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public BigDecimal getDamageFee() {
        return damageFee;
    }

    public void setDamageFee(BigDecimal damageFee) {
        this.damageFee = damageFee;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
