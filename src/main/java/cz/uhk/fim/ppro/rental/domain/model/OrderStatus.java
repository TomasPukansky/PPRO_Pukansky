package cz.uhk.fim.ppro.rental.domain.model;

public enum OrderStatus {
    /** Rezervace dopředu (telefon v úterý, vyzvednutí v sobotu) */
    RESERVATION,
    /** Aktivně vypůjčeno zákazníkovi u pultu */
    ACTIVE,
    /** Uzavřeno – všechny kusy vráceny a doplatky vyrovnány */
    CLOSED,
    /** Propadlá rezervace (nevyzvednuto po 10:00) */
    EXPIRED,
    /** Zrušeno zákazníkem či obsluhou */
    CANCELLED
}
