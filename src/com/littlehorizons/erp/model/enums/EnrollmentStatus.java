package com.littlehorizons.erp.model.enums;

/**
 * Status of a grade enrollment or of a subject registration.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum EnrollmentStatus {

    ACTIVE("Activa"),
    COMPLETED("Completada"),
    WITHDRAWN("Retirada");

    private final String label;

    EnrollmentStatus(String label) {
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
    public static EnrollmentStatus fromLabel(String label) {
        for (EnrollmentStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown enrollment status: " + label);
    }
}