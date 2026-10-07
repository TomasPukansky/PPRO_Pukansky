package cz.uhk.fim.ppro.rental.domain;

import cz.uhk.fim.ppro.rental.domain.model.EquipmentCategory;
import cz.uhk.fim.ppro.rental.domain.model.PriceRule;
import cz.uhk.fim.ppro.rental.domain.model.PricingPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PricingPolicyTest {

    private final EquipmentCategory category = new EquipmentCategory(1L, "SKI_CARV", "Sjezdové lyže", "Test");
    private final PriceRule priceRule = new PriceRule(category, BigDecimal.valueOf(350.00), BigDecimal.valueOf(600.00), BigDecimal.valueOf(1000.00));

    @Test
    @DisplayName("Ceník: Běžná jednodenní výpůjčka (středa ráno až středa večer = 1 den)")
    void shouldCalculateSingleDayPrice() {
        OffsetDateTime start = OffsetDateTime.of(2026, 10, 14, 9, 0, 0, 0, ZoneOffset.UTC); // Středa
        OffsetDateTime end = OffsetDateTime.of(2026, 10, 14, 18, 0, 0, 0, ZoneOffset.UTC);

        BigDecimal price = PricingPolicy.calculateItemPrice(priceRule, start, end);
        assertEquals(BigDecimal.valueOf(350.00), price);
    }

    @Test
    @DisplayName("Ceník: Víkendový balíček od pátku 14:00 do neděle večer = paušál 600 Kč")
    void shouldApplyWeekendRateFromFridayAfternoon() {
        OffsetDateTime fridayAfternoon = OffsetDateTime.of(2026, 10, 16, 14, 30, 0, 0, ZoneOffset.UTC); // Pátek 14:30
        OffsetDateTime sundayEvening = OffsetDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneOffset.UTC);   // Neděle 18:00

        assertTrue(PricingPolicy.isWeekendRental(fridayAfternoon, sundayEvening));
        BigDecimal price = PricingPolicy.calculateItemPrice(priceRule, fridayAfternoon, sundayEvening);
        assertEquals(BigDecimal.valueOf(600.00), price);
    }

    @Test
    @DisplayName("Ceník: Pátek dopoledne před 14:00 není automatický víkendový balíček, ale standardní denní sazba")
    void shouldNotApplyWeekendRateBeforeFriday14Hour() {
        OffsetDateTime fridayMorning = OffsetDateTime.of(2026, 10, 16, 9, 0, 0, 0, ZoneOffset.UTC); // Pátek 09:00
        OffsetDateTime sundayEvening = OffsetDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneOffset.UTC); // Neděle 18:00

        assertFalse(PricingPolicy.isWeekendRental(fridayMorning, sundayEvening));
        BigDecimal price = PricingPolicy.calculateItemPrice(priceRule, fridayMorning, sundayEvening);
        // 57 hodin = 3 dny * 350 = 1050 Kč
        assertEquals(BigDecimal.valueOf(1050.00), price);
    }

    @Test
    @DisplayName("Penále: Vrácení včas = 0 Kč penále")
    void shouldReturnZeroLateFeeWhenReturnedOnTime() {
        OffsetDateTime expected = OffsetDateTime.of(2026, 10, 14, 17, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime returned = OffsetDateTime.of(2026, 10, 14, 16, 45, 0, 0, ZoneOffset.UTC);

        BigDecimal lateFee = PricingPolicy.calculateLateFee(priceRule.getDailyRate(), expected, returned);
        assertEquals(BigDecimal.ZERO, lateFee);
    }

    @Test
    @DisplayName("Penále: Zpoždění o 2 hodiny = 1 započatý den prodlení")
    void shouldChargeOneDayPenaltyForShortDelay() {
        OffsetDateTime expected = OffsetDateTime.of(2026, 10, 14, 17, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime returned = OffsetDateTime.of(2026, 10, 14, 19, 0, 0, 0, ZoneOffset.UTC); // +2 hodiny

        BigDecimal lateFee = PricingPolicy.calculateLateFee(priceRule.getDailyRate(), expected, returned);
        assertEquals(BigDecimal.valueOf(350.00), lateFee);
    }

    @Test
    @DisplayName("Penále: Zpoždění o 26 hodin = 2 započaté dny prodlení")
    void shouldChargeTwoDaysPenaltyForOver24HoursDelay() {
        OffsetDateTime expected = OffsetDateTime.of(2026, 10, 14, 17, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime returned = OffsetDateTime.of(2026, 10, 15, 19, 0, 0, 0, ZoneOffset.UTC); // +26 hodin

        BigDecimal lateFee = PricingPolicy.calculateLateFee(priceRule.getDailyRate(), expected, returned);
        assertEquals(BigDecimal.valueOf(700.00), lateFee);
    }

    @Test
    @DisplayName("Pátek 14:00 se posuzuje v místním čase (Europe/Prague), ne v UTC")
    void shouldEvaluateFridayAfternoonInLocalTime() {
        // 14:00 v Praze = 12:00 UTC – dříve se chybně vyhodnotilo jako „před 14:00“
        OffsetDateTime fridayLocal = OffsetDateTime.of(2026, 10, 16, 14, 0, 0, 0, ZoneOffset.ofHours(2));
        OffsetDateTime sundayLocal = OffsetDateTime.of(2026, 10, 18, 18, 0, 0, 0, ZoneOffset.ofHours(2));

        assertTrue(PricingPolicy.isWeekendRental(fridayLocal.withOffsetSameInstant(ZoneOffset.UTC),
                sundayLocal.withOffsetSameInstant(ZoneOffset.UTC)));
    }
}
