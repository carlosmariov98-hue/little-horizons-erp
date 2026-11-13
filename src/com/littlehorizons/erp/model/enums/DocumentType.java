package com.littlehorizons.erp.model.enums;

/**
 * Identification document types accepted for students.
 * The label is the exact text stored in the database and shown in the menus.
 */
public enum DocumentType {

    CIVIL_REGISTRY("RC"),
    IDENTITY_CARD("TI"),
    CITIZENSHIP_CARD("CC"),
    FOREIGNER_ID("CE"),
    PASSPORT("Pasaporte");

    private final String label;

    DocumentType(String label) {
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
    public static DocumentType fromLabel(String label) {
        for (DocumentType value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown document type: " + label);
    }
}
