package cz.uhk.fim.ppro.rental.presentation.dto;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateEquipmentStatusRequest {

    @NotNull(message = "Stav kusu je povinný.")
    private EquipmentStatus status;

    private String note;

    public UpdateEquipmentStatusRequest() {
    }

    public UpdateEquipmentStatusRequest(EquipmentStatus status, String note) {
        this.status = status;
        this.note = note;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
