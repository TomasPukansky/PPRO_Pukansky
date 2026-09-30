package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateEquipmentItemRequest {

    @NotNull(message = "Kategorie je povinná.")
    private Long categoryId;

    @NotBlank(message = "Inventární číslo nesmí být prázdné.")
    @Size(max = 50, message = "Inventární číslo může mít maximálně 50 znaků.")
    private String inventoryNumber;

    @NotBlank(message = "Velikost nesmí být prázdná.")
    @Size(max = 50, message = "Velikost může mít maximálně 50 znaků.")
    private String size;

    @NotNull(message = "Rok pořízení je povinný.")
    @Min(value = 1990, message = "Rok pořízení musí být minimálně 1990.")
    private Integer purchaseYear;

    @NotBlank(message = "Stav opotřebení nesmí být prázdný.")
    @Size(max = 255, message = "Stav opotřebení může mít maximálně 255 znaků.")
    private String conditionState;

    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    public CreateEquipmentItemRequest() {
    }

    public CreateEquipmentItemRequest(Long categoryId, String inventoryNumber, String size, Integer purchaseYear, String conditionState, EquipmentStatus status) {
        this.categoryId = categoryId;
        this.inventoryNumber = inventoryNumber;
        this.size = size;
        this.purchaseYear = purchaseYear;
        this.conditionState = conditionState;
        this.status = status != null ? status : EquipmentStatus.AVAILABLE;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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
}
