package com.littlehorizons.erp.model.enums;

/**
 * Gender of a person.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum Gender {

    MALE("M"),
    FEMALE("F"),
    OTHER("Otro");

    private final String label;

    Gender(String label) {
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
    public static Gender fromLabel(String label) {
        for (Gender value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown gender: " + label);
    }
}
