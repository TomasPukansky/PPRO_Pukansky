package cz.uhk.fim.ppro.rental.presentation.dto;

import jakarta.validation.constraints.NotNull;

public class SwapItemRequest {

    @NotNull(message = "ID nového vybraného kusu je povinné")
    private Long newItemId;

    public SwapItemRequest() {}

    public SwapItemRequest(Long newItemId) {
        this.newItemId = newItemId;
    }

    public Long getNewItemId() { return newItemId; }
    public void setNewItemId(Long newItemId) { this.newItemId = newItemId; }
}
