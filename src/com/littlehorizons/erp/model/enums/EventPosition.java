package com.littlehorizons.erp.model.enums;

/**
 * Result of a competitor in an event. The label is the exact text stored in the database.
 * The constants are declared from the best to the worst result, so the ordinal gives the ranking.
 */
public enum EventPosition {

    FIRST("1er Lugar"),
    SECOND("2do Lugar"),
    THIRD("3er Lugar"),
    PARTICIPANT("Participante");

    private final String label;

    EventPosition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True for the three podium places (1st, 2nd and 3rd), which can be given to only one competitor each. */
    public boolean isPodium() {
        return this != PARTICIPANT;
    }

    /**
     * Finds the constant that matches a database label.
     *
     * @param label text stored in the database
     * @return the matching constant
     * @throws IllegalArgumentException if no constant uses that label
     */
    public static EventPosition fromLabel(String label) {
        for (EventPosition value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown event position: " + label);
    }
}