package com.littlehorizons.erp.model.enums;

/**
 * Academic periods of a school year. FINAL is the yearly result.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum AcademicPeriod {

    FIRST("I"),
    SECOND("II"),
    THIRD("III"),
    FOURTH("IV"),
    FINAL("Final");

    private final String label;

    AcademicPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Tells whether the period is one of the four regular periods (I to IV), not the yearly result.
     *
     * @return true for I, II, III and IV
     */
    public boolean isRegular() {
        return this != FINAL;
    }

    /**
     * Finds the constant that matches a database label.
     *
     * @param label text stored in the database
     * @return the matching constant
     * @throws IllegalArgumentException if no constant uses that label
     */
    public static AcademicPeriod fromLabel(String label) {
        for (AcademicPeriod value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown academic period: " + label);
    }
}