package com.littlehorizons.erp.model.enums;

/**
 * Employment status of a teacher.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum TeacherStatus {

    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    ON_LEAVE("Licencia");

    private final String label;

    TeacherStatus(String label) {
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
    public static TeacherStatus fromLabel(String label) {
        for (TeacherStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown teacher status: " + label);
    }
}