package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Date parsing and formatting for the console (format dd/mm/yyyy).
 */
public final class DateUtils {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private DateUtils() {
    }

    /**
     * Converts text such as "25/03/2012" into a date. Impossible dates (31/02/2012) are rejected.
     *
     * @param text the text typed by the user
     * @return the parsed date
     * @throws ValidationException if the text is not a valid date
     */
    public static LocalDate parse(String text) throws ValidationException {
        try {
            return LocalDate.parse(text.trim(), FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Fecha inválida. Use el formato dd/mm/aaaa (por ejemplo 25/03/2012).", e);
        }
    }

    /**
     * Formats a date for display.
     *
     * @param date the date to format (may be null)
     * @return the date as dd/mm/yyyy, or "-" when it is null
     */
    public static String format(LocalDate date) {
        return date == null ? "-" : date.format(FORMAT);
    }
}
