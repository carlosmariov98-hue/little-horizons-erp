package com.littlehorizons.erp.model.enums;

/** Role of a student in an event. The label is the exact text stored in the database. */
public enum ParticipationRole {

    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String label;

    ParticipationRole(String label) {
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
    public static ParticipationRole fromLabel(String label) {
        for (ParticipationRole value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown participation role: " + label);
    }
}