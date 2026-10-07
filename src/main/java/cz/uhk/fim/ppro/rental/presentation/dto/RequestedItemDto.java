package cz.uhk.fim.ppro.rental.presentation.dto;

import jakarta.validation.constraints.NotNull;

public class RequestedItemDto {

    @NotNull(message = "Kategorie je povinná")
    private Long categoryId;

    private String size;

    private Long specificItemId; // volitelně konkrétní kus, jinak auto-alokace

    public RequestedItemDto() {}

    public RequestedItemDto(Long categoryId, String size) {
        this.categoryId = categoryId;
        this.size = size;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Long getSpecificItemId() { return specificItemId; }
    public void setSpecificItemId(Long specificItemId) { this.specificItemId = specificItemId; }
}
