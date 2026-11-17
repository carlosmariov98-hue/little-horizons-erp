package com.littlehorizons.erp.model.enums;

/** Status of a loan. The label is the exact text stored in the database. */
public enum LoanStatus {

    ACTIVE("Activo"),
    RETURNED("Devuelto"),
    OVERDUE("Vencido");

    private final String label;

    LoanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True while the resource has not been returned (active or overdue). */
    public boolean isOpen() {
        return this != RETURNED;
    }

    /**
     * Finds the constant that matches a database label.
     *
     * @param label text stored in the database
     * @return the matching constant
     * @throws IllegalArgumentException if no constant uses that label
     */
    public static LoanStatus fromLabel(String label) {
        for (LoanStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown loan status: " + label);
    }
}
