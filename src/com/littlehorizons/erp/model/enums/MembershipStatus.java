package com.littlehorizons.erp.model.enums;

/** Status of the membership of a student in a club. The label is the exact text stored in the database. */
public enum MembershipStatus {

    ACTIVE("Activa"),
    WITHDRAWN("Retirada");

    private final String label;

    MembershipStatus(String label) {
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
    public static MembershipStatus fromLabel(String label) {
        for (MembershipStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown membership status: " + label);
    }
}