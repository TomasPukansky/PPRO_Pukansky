package cz.uhk.fim.ppro.rental.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "damage_records")
public class DamageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_item_id")
    private RentalItem rentalItem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private EquipmentItem item;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "penalty_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal penaltyFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean resolved = false;

    public DamageRecord() {
    }

    public DamageRecord(RentalItem rentalItem, EquipmentItem item, Customer customer,
                        String description, BigDecimal penaltyFee, Boolean resolved) {
        this.rentalItem = rentalItem;
        this.item = item;
        this.customer = customer;
        this.description = description;
        this.penaltyFee = penaltyFee != null ? penaltyFee : BigDecimal.ZERO;
        this.resolved = resolved != null ? resolved : false;
        this.reportedAt = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (reportedAt == null) {
            reportedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RentalItem getRentalItem() {
        return rentalItem;
    }

    public void setRentalItem(RentalItem rentalItem) {
        this.rentalItem = rentalItem;
    }

    public EquipmentItem getItem() {
        return item;
    }

    public void setItem(EquipmentItem item) {
        this.item = item;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPenaltyFee() {
        return penaltyFee;
    }

    public void setPenaltyFee(BigDecimal penaltyFee) {
        this.penaltyFee = penaltyFee;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }
}
