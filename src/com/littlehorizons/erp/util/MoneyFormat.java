package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.util.Locale;

/** Formats amounts of money in Colombian pesos, for example "$ 1.250.000,00". */
public final class MoneyFormat {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private MoneyFormat() {
    }

    public static String format(BigDecimal amount) {
        return String.format(COLOMBIA, "$ %,.2f", amount);
    }
}