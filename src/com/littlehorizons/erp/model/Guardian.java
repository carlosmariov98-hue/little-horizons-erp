package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.DocumentType;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A guardian (parent, relative or legal tutor) of one or more students. Maps the table "acudientes".
 * A guardianId of 0 means the guardian has not been saved in the database yet.
 */
public class Guardian {

    private int guardianId;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstLastName;
    private String secondLastName;
    private String relationship;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String occupation;
    private boolean financiallyResponsible = true;
    private boolean active = true;

    public int getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(int guardianId) {
        this.guardianId = guardianId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getFirstLastName() {
        return firstLastName;
    }

    public void setFirstLastName(String firstLastName) {
        this.firstLastName = firstLastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public boolean isFinanciallyResponsible() {
        return financiallyResponsible;
    }

    public void setFinanciallyResponsible(boolean financiallyResponsible) {
        this.financiallyResponsible = financiallyResponsible;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Builds the full name, skipping the parts that are empty.
     *
     * @return first name, middle name, first last name and second last name separated by spaces
     */
    public String getFullName() {
        return Stream.of(firstName, middleName, firstLastName, secondLastName)
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return "Guardian{id=" + guardianId + ", document=" + documentNumber + ", name=" + getFullName() + "}";
    }
}
