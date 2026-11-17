package com.littlehorizons.erp.model.enums;

/**
 * Attendance status of a student in a class.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum AttendanceStatus {

    PRESENT("Presente"),
    ABSENT("Ausente"),
    LATE("Tardanza"),
    MEDICAL_EXCUSE("Excusa Médica");

    private final String label;

    AttendanceStatus(String label) {
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
    public static AttendanceStatus fromLabel(String label) {
        for (AttendanceStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown attendance status: " + label);
    }
}