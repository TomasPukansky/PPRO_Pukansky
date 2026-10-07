package cz.uhk.fim.ppro.rental.domain.model;

public enum RentalItemStatus {
    /** Rezervováno */
    RESERVED,
    /** Vydáno a zapůjčeno venku */
    RENTED,
    /** Vráceno zákazníkem */
    RETURNED,
    /** Vráceno s poškozením (vyžaduje servis) */
    DAMAGED
}
