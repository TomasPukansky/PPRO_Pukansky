package cz.uhk.fim.ppro.rental.domain.model;

/**
 * Životní cyklus a stav konkrétního kusu vybavení.
 */
public enum EquipmentStatus {
    /** Volné k okamžitému zapůjčení či rezervaci */
    AVAILABLE,
    /** Rezervováno na budoucí období */
    RESERVED,
    /** Aktuálně zapůjčeno zákazníkovi */
    RENTED,
    /** V servisu / na opravě – nesmí se půjčovat ani rezervovat */
    IN_SERVICE,
    /** Trvale vyřazeno z inventáře */
    RETIRED
}
