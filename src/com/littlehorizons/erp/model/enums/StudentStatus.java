package com.littlehorizons.erp.model.enums;

/**
 * Enrollment status of a student in the school.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum StudentStatus {

    ACTIVE("Activo"),
    GRADUATED("Graduado"),
    WITHDRAWN("Retirado"),
    SUSPENDED("Suspendido");

    private final String label;

    StudentStatus(String label) {
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
    public static StudentStatus fromLabel(String label) {
        for (StudentStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown student status: " + label);
    }
}
