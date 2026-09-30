package cz.uhk.fim.ppro.rental.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Klíčová doménová entita reprezentující konkrétní fyzický inventární kus.
 */
@Entity
@Table(name = "equipment_items")
public class EquipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private EquipmentCategory category;

    @Column(name = "inventory_number", nullable = false, unique = true, length = 50)
    private String inventoryNumber;

    @Column(nullable = false, length = 50)
    private String size;

    @Column(name = "purchase_year", nullable = false)
    private Integer purchaseYear;

    @Column(name = "condition_state", nullable = false, length = 255)
    private String conditionState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public EquipmentItem() {
    }

    public EquipmentItem(EquipmentCategory category, String inventoryNumber, String size, Integer purchaseYear, String conditionState, EquipmentStatus status) {
        this.category = category;
        this.inventoryNumber = inventoryNumber;
        this.size = size;
        this.purchaseYear = purchaseYear;
        this.conditionState = conditionState;
        this.status = status != null ? status : EquipmentStatus.AVAILABLE;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public void setCategory(EquipmentCategory category) {
        this.category = category;
    }

    public String getInventoryNumber() {
        return inventoryNumber;
    }

    public void setInventoryNumber(String inventoryNumber) {
        this.inventoryNumber = inventoryNumber;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getPurchaseYear() {
        return purchaseYear;
    }

    public void setPurchaseYear(Integer purchaseYear) {
        this.purchaseYear = purchaseYear;
    }

    public String getConditionState() {
        return conditionState;
    }

    public void setConditionState(String conditionState) {
        this.conditionState = conditionState;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
