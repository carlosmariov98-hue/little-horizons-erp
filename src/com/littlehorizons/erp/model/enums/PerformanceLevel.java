package com.littlehorizons.erp.model.enums;

import java.math.BigDecimal;

/**
 * Performance level of a grade on the Colombian 0.0 - 5.0 scale (Decreto 1290).
 * It is calculated from the score, it is not stored in the database.
 */
public enum PerformanceLevel {

    LOW("Bajo", "0.0"),
    BASIC("Básico", "3.0"),
    HIGH("Alto", "4.0"),
    SUPERIOR("Superior", "4.6");

    private final String label;
    private final BigDecimal minimum;

    PerformanceLevel(String label, String minimum) {
        this.label = label;
        this.minimum = new BigDecimal(minimum);
    }

    public String getLabel() {
        return label;
    }

    /**
     * Finds the level that corresponds to a score.
     *
     * @param score a score or average between 0.0 and 5.0
     * @return the performance level
     */
    public static PerformanceLevel fromScore(BigDecimal score) {
        PerformanceLevel result = LOW;
        for (PerformanceLevel level : values()) {
            if (score.compareTo(level.minimum) >= 0) {
                result = level;
            }
        }
        return result;
    }
}
