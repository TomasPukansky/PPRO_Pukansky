package cz.uhk.fim.ppro.rental.application.exception;

public class DuplicateInventoryNumberException extends BusinessValidationException {
    public DuplicateInventoryNumberException(String inventoryNumber) {
        super("Kus s inventárním číslem '" + inventoryNumber + "' již v systému existuje.");
    }
}
