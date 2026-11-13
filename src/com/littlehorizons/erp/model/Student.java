package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;

import java.time.LocalDate;
import java.time.Period;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A student of the school. Maps the table "estudiantes".
 * It only holds data: validation lives in the controller layer and persistence in the DAO layer.
 * A studentId of 0 means the student has not been saved in the database yet.
 */
public class Student {

    private int studentId;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstLastName;
    private String secondLastName;
    private LocalDate birthDate;
    private Gender gender;
    private String address;
    private String phone;
    private String email;
    private String bloodType;
    private String allergies;
    private String medicalConditions;
    private LocalDate admissionDate;
    private StudentStatus status;

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = medicalConditions;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    /**
     * Builds the full name, skipping the parts that are empty (for example, no middle name).
     *
     * @return first name, middle name, first last name and second last name separated by spaces
     */
    public String getFullName() {
        return Stream.of(firstName, middleName, firstLastName, secondLastName)
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.joining(" "));
    }

    /**
     * Calculates the age of the student on a given date.
     *
     * @param referenceDate date used to calculate the age
     * @return completed years between the birth date and the reference date
     */
    public int getAgeOn(LocalDate referenceDate) {
        return Period.between(birthDate, referenceDate).getYears();
    }

    /**
     * Calculates the current age of the student.
     *
     * @return completed years between the birth date and today
     */
    public int getAge() {
        return getAgeOn(LocalDate.now());
    }

    @Override
    public String toString() {
        return "Student{id=" + studentId + ", document=" + (documentType == null ? null : documentType.getLabel())
                + " " + documentNumber + ", name=" + getFullName() + "}";
    }
}
