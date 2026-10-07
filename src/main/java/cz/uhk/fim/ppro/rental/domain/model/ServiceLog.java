package cz.uhk.fim.ppro.rental.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "service_logs")
public class ServiceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private EquipmentItem item;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_cost", precision = 10, scale = 2)
    private BigDecimal serviceCost = BigDecimal.ZERO;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public ServiceLog() {
    }

    public ServiceLog(EquipmentItem item, OffsetDateTime startedAt, String description, BigDecimal serviceCost) {
        this.item = item;
        this.startedAt = startedAt != null ? startedAt : OffsetDateTime.now();
        this.description = description;
        this.serviceCost = serviceCost != null ? serviceCost : BigDecimal.ZERO;
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

    public EquipmentItem getItem() {
        return item;
    }

    public void setItem(EquipmentItem item) {
        this.item = item;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getServiceCost() {
        return serviceCost;
    }

    public void setServiceCost(BigDecimal serviceCost) {
        this.serviceCost = serviceCost;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
