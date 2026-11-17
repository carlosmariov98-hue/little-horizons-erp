package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * Tax rules of the school invoices (simulated). Formal education is excluded from IVA, while uniforms
 * and certificates are charged with 19% IVA. The amount stored in a payment is the total paid, with IVA included.
 */
public final class TaxPolicy {

    public static final BigDecimal VAT_RATE = new BigDecimal("0.19");

    private static final List<String> VAT_CONCEPT_CODES = Arrays.asList("UNIFORME", "CERTIFICADO");

    private TaxPolicy() {
    }

    public static boolean appliesVat(String conceptCode) {
        return VAT_CONCEPT_CODES.contains(conceptCode);
    }

    /** Value of the product before IVA (equal to the total when the concept has no IVA). */
    public static BigDecimal taxableBase(String conceptCode, BigDecimal total) {
        if (!appliesVat(conceptCode)) {
            return total;
        }
        return total.divide(BigDecimal.ONE.add(VAT_RATE), 2, RoundingMode.HALF_UP);
    }

    /** IVA included in the total (zero when the concept has no IVA). */
    public static BigDecimal vatAmount(String conceptCode, BigDecimal total) {
        return total.subtract(taxableBase(conceptCode, total));
    }
}