package cz.uhk.fim.ppro.rental.domain.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class PricingPolicy {

    /** Víkend se počítá od pátku 14:00 */
    public static final int WEEKEND_START_HOUR = 14;

    /** Pásmo půjčovny – „pátek odpoledne“ se posuzuje v místním čase, ne v UTC */
    public static final ZoneId LOCAL_ZONE = ZoneId.of("Europe/Prague");

    private PricingPolicy() {}

    /**
     * Vypočítá cenu výpůjčky kusu podle termínu a ceníkového pravidla.
     * Víkend (od pátku 14:00 do neděle/pondělí) se počítá jako 2 dny za zvýhodněný weekend_rate.
     */
    public static BigDecimal calculateItemPrice(PriceRule rule, OffsetDateTime startsAt, OffsetDateTime expectedEndAt) {
        if (rule == null) {
            return BigDecimal.ZERO;
        }

        if (isWeekendRental(startsAt, expectedEndAt)) {
            return rule.getWeekendRate();
        }

        long billableDays = calculateBillableDays(startsAt, expectedEndAt);
        return rule.getDailyRate().multiply(BigDecimal.valueOf(billableDays));
    }

    /**
     * Spočítá penále za pozdní vrácení položky.
     * Penále se účtuje za každý započatý den prodlení.
     */
    public static BigDecimal calculateLateFee(BigDecimal dailyRate, OffsetDateTime expectedEndAt, OffsetDateTime returnedAt) {
        if (returnedAt == null || !returnedAt.isAfter(expectedEndAt) || dailyRate == null) {
            return BigDecimal.ZERO;
        }

        Duration delay = Duration.between(expectedEndAt, returnedAt);
        long hours = delay.toHours();
        // Každých započatých 24 hodin = 1 den penále
        long daysLate = (hours / 24) + ((hours % 24 > 0 || delay.toMinutesPart() > 0) ? 1 : 0);
        if (daysLate == 0) {
            daysLate = 1;
        }

        return dailyRate.multiply(BigDecimal.valueOf(daysLate));
    }

    /**
     * Zjistí, zda se jedná o standardní víkendovou výpůjčku.
     * Začátek: Pátek od 14:00, Konec: Neděle nebo Pondělí dopoledne.
     */
    public static boolean isWeekendRental(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) return false;
        ZonedDateTime localStart = start.atZoneSameInstant(LOCAL_ZONE);
        ZonedDateTime localEnd = end.atZoneSameInstant(LOCAL_ZONE);

        boolean startsFridayAfternoon = localStart.getDayOfWeek() == DayOfWeek.FRIDAY && localStart.getHour() >= WEEKEND_START_HOUR;
        boolean endsSundayOrMonday = localEnd.getDayOfWeek() == DayOfWeek.SUNDAY ||
                (localEnd.getDayOfWeek() == DayOfWeek.MONDAY && localEnd.getHour() <= 12);

        long totalDays = calculateBillableDays(start, end);
        return startsFridayAfternoon && endsSundayOrMonday && totalDays <= 3;
    }

    /**
     * Počet účtovaných dnů: do 24 hod = 1 den, každých dalších započatých 24 hod = +1 den.
     */
    public static long calculateBillableDays(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) return 1;
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        if (hours <= 0) return 1;

        long days = hours / 24;
        if (hours % 24 > 0 || duration.toMinutesPart() > 0) {
            days++;
        }
        return Math.max(1, days);
    }
}
