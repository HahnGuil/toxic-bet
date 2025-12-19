package br.com.hahn.toxicbet.util;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility class for converting and formatting date-time values.
 * <p>
 * This class provides both synchronous and reactive helpers. The reactive
 * methods use Mono.fromSupplier to defer evaluation until subscription.
 *
 * @author HahnGuil
 */
public final class DateTimeConverter {

    public static String formatInstant(Instant instant) {
        if (Objects.isNull(instant)) {
            return "";
        }
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        return zdt.format(FORMATTER);
    }

    public static String formatInstantNow() {
        return formatInstant(Instant.now());
    }

    /**
     * Reactive variant for current instant.
     */
    public static Mono<String> formatInstantNowReactive() {
        return Mono.fromSupplier(DateTimeConverter::formatInstantNow);
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
                    .withLocale(Locale.getDefault());

    private DateTimeConverter() { }
}
