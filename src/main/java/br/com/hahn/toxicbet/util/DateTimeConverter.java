package br.com.hahn.toxicbet.util;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility class for converting and formatting date-time values.
 *
 * <p>
 * This class provides synchronous and reactive helpers for converting and formatting
 * date/time values used across the application. It supports parsing strings in the
 * pattern {@code dd/MM/yyyy HH:mm} into {@link LocalDateTime} and formatting
 * {@link Instant} to strings with a defined formatter.
 * </p>
 *
 * @author HahnGuil
 */
public final class DateTimeConverter {

    /**
     * Formats the provided {@link Instant} into a string using the configured formatter.
     * Returns an empty string when {@code instant} is {@code null}.
     *
     * @param instant the instant to format, may be {@code null}
     * @return formatted date/time string or empty string if {@code instant} is {@code null}
     */
    public static String formatInstant(Instant instant) {
        if (Objects.isNull(instant)) {
            return "";
        }
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        return zdt.format(FORMATTER);
    }

    /**
     * Formats the current instant (now) into a string using the configured formatter.
     *
     * @return formatted current date/time string
     */
    public static String formatInstantNow() {
        return formatInstant(Instant.now());
    }

    /**
     * Reactive variant that returns the formatted current instant as a {@link Mono}.
     *
     * @return a {@link Mono} emitting the formatted current date/time string
     */
    public static Mono<String> formatInstantNowReactive() {
        return Mono.fromSupplier(DateTimeConverter::formatInstantNow);
    }

    /**
     * Parses a string in the pattern {@code dd/MM/yyyy HH:mm} into a {@link LocalDateTime}.
     *
     * <p>
     * Returns {@code null} if the input is {@code null} or blank. If the input does not
     * match the expected pattern, the method attempts to parse using a more permissive
     * formatter (which may accept time zone/seconds). If parsing still fails, an
     * {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param dateTimeStr the date/time string to parse, expected in {@code dd/MM/yyyy HH:mm} format
     * @return parsed {@link LocalDateTime} or {@code null} when input is {@code null} or blank
     * @throws IllegalArgumentException when the input cannot be parsed
     */
    public static LocalDateTime parseToLocalDateTime(String dateTimeStr) {
        if (Objects.isNull(dateTimeStr) || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, PARSER_NO_SECONDS);
        } catch (DateTimeParseException ex) {
            // Try to accept strings with zone/seconds using the existing FORMATTER
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr, FORMATTER);
                return zdt.toLocalDateTime();
            } catch (DateTimeParseException ex2) {
                throw new IllegalArgumentException("Invalid date/time format. Use dd/MM/yyyy HH:mm", ex2);
            }
        }
    }

    /**
     * Formats a {@link LocalDateTime} into a string using the pattern dd/MM/yyyy HH:mm.
     *
     * @param localDateTime the LocalDateTime to format, may be {@code null}
     * @return formatted date/time string or empty string if {@code localDateTime} is {@code null}
     */
    public static String formatLocalDateTime(LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return "";
        }
        return localDateTime.format(PARSER_NO_SECONDS);
    }
    
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
                    .withLocale(Locale.getDefault());

    private static final DateTimeFormatter PARSER_NO_SECONDS =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withLocale(Locale.getDefault());

    private DateTimeConverter() { }
}
