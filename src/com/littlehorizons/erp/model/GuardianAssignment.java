package com.littlehorizons.erp.model;

/**
 * Link between a student and one guardian (table "estudiante_acudiente"):
 * whether the guardian is the main contact and whether he or she is an emergency contact.
 */
public class GuardianAssignment {

    private final Guardian guardian;
    private boolean primary;
    private boolean emergencyContact;

    public GuardianAssignment(Guardian guardian, boolean primary, boolean emergencyContact) {
        this.guardian = guardian;
        this.primary = primary;
        this.emergencyContact = emergencyContact;
    }

    public Guardian getGuardian() {
        return guardian;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(boolean emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
