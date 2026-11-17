package com.littlehorizons.erp.model.enums;

/**
 * Period a payment applies to. The label is the exact text stored in the database and shown in the menus.
 */
public enum PaymentPeriod {

    FIRST("I"),
    SECOND("II"),
    THIRD("III"),
    FOURTH("IV"),
    ANNUAL("Anual");

    private final String label;

    PaymentPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Finds the constant that matches a database label.
     *
     * @param label text stored in the database
     * @return the matching constant
     * @throws IllegalArgumentException if no constant uses that label
     */
    public static PaymentPeriod fromLabel(String label) {
        for (PaymentPeriod value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown payment period: " + label);
    }
}