package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;

import java.time.OffsetDateTime;

public class EquipmentItemDto {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private String inventoryNumber;
    private String size;
    private Integer purchaseYear;
    private String conditionState;
    private EquipmentStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public EquipmentItemDto() {
    }

    public EquipmentItemDto(Long id, Long categoryId, String categoryName, String categoryCode,
                            String inventoryNumber, String size, Integer purchaseYear,
                            String conditionState, EquipmentStatus status,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryCode = categoryCode;
        this.inventoryNumber = inventoryNumber;
        this.size = size;
        this.purchaseYear = purchaseYear;
        this.conditionState = conditionState;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
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
