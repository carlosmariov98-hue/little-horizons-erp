package com.littlehorizons.erp.model.enums;

/** Status of a school resource in the inventory. The label is the exact text stored in the database. */
public enum ResourceStatus {

    AVAILABLE("Disponible"),
    LOANED("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    RETIRED("Dado de Baja");

    private final String label;

    ResourceStatus(String label) {
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
    public static ResourceStatus fromLabel(String label) {
        for (ResourceStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown resource status: " + label);
    }
}