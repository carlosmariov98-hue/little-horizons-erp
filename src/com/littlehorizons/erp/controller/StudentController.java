package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.GuardianAssignment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Business logic for students. It validates the data and applies the school rules before the DAOs save anything.
 */
public class StudentController {

    private static final int MIN_AGE = 2;
    private static final int MAX_AGE = 25;

    private final StudentDao studentDao;
    private final StudentGuardianDao studentGuardianDao;

    public StudentController(StudentDao studentDao, StudentGuardianDao studentGuardianDao) {
        this.studentDao = studentDao;
        this.studentGuardianDao = studentGuardianDao;
    }

    private static int ageOf(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Returns the identity document that corresponds to an age in Colombia:
     * Registro Civil under 7 years, Tarjeta de Identidad from 7 to 17 and Cédula from 18.
     *
     * @param birthDate birth date of the student
     * @return RC, TI or CC
     */
    public static DocumentType suggestedDocumentType(LocalDate birthDate) {
        int age = ageOf(birthDate);
        if (age < 7) {
            return DocumentType.CIVIL_REGISTRY;
        }
        return age < 18 ? DocumentType.IDENTITY_CARD : DocumentType.CITIZENSHIP_CARD;
    }

    /**
     * Checks that the birth date gives a plausible student age.
     *
     * @param birthDate birth date to check
     * @throws ValidationException if the date is in the future or the age is out of range
     */
    public static void validateBirthDate(LocalDate birthDate) throws ValidationException {
        Validator.requirePastOrToday(birthDate, "Fecha de nacimiento");
        int age = ageOf(birthDate);
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new ValidationException("La edad del estudiante debe estar entre " + MIN_AGE + " y " + MAX_AGE
                    + " años (la fecha indicada da " + age + ").");
        }
    }

    /**
     * Checks that the document type matches the student's age. Foreign documents (CE, Pasaporte) are always allowed.
     *
     * @param type      selected document type
     * @param birthDate birth date of the student
     * @throws ValidationException if the type does not match the age
     */
    public static void validateDocumentTypeForAge(DocumentType type, LocalDate birthDate) throws ValidationException {
        Validator.requireNotNull(type, "Tipo de documento");
        if (type == DocumentType.FOREIGNER_ID || type == DocumentType.PASSPORT) {
            return;
        }
        DocumentType expected = suggestedDocumentType(birthDate);
        if (type != expected) {
            throw new ValidationException("Para un estudiante de " + ageOf(birthDate) + " años corresponde el documento "
                    + expected.getLabel() + " (o CE / Pasaporte si es extranjero).");
        }
    }

    /**
     * Validates and cleans all the data of a student, replacing each field with its cleaned value.
     *
     * @param student the student to validate
     * @throws ValidationException with a user-friendly message when a field is invalid
     */
    public void validateStudent(Student student) throws ValidationException {
        student.setDocumentNumber(Validator.requireDocumentNumber(student.getDocumentNumber()));
        student.setFirstName(Validator.requireName(student.getFirstName(), "Primer nombre"));
        student.setMiddleName(Validator.optionalName(student.getMiddleName(), "Segundo nombre"));
        student.setFirstLastName(Validator.requireName(student.getFirstLastName(), "Primer apellido"));
        student.setSecondLastName(Validator.optionalName(student.getSecondLastName(), "Segundo apellido"));
        Validator.requireNotNull(student.getBirthDate(), "Fecha de nacimiento");
        validateBirthDate(student.getBirthDate());
        validateDocumentTypeForAge(student.getDocumentType(), student.getBirthDate());
        Validator.requireNotNull(student.getGender(), "Género");
        Validator.requireNotNull(student.getStatus(), "Estado");
        Validator.requirePastOrToday(student.getAdmissionDate(), "Fecha de ingreso");
        if (student.getAdmissionDate().isBefore(student.getBirthDate())) {
            throw new ValidationException("La fecha de ingreso no puede ser anterior a la fecha de nacimiento.");
        }
        student.setAddress(Validator.optionalText(student.getAddress(), "Dirección", 255));
        student.setPhone(Validator.optionalPhone(student.getPhone(), "Teléfono"));
        student.setEmail(Validator.optionalEmail(student.getEmail()));
        student.setBloodType(Validator.optionalBloodType(student.getBloodType()));
        student.setAllergies(Validator.optionalText(student.getAllergies(), "Alergias", 1000));
        student.setMedicalConditions(Validator.optionalText(student.getMedicalConditions(),
                "Condiciones médicas", 1000));
    }

    /**
     * Checks that no student uses the given document number.
     *
     * @param documentNumber the (already validated) document number
     * @throws BusinessRuleException if a student already has it
     * @throws DatabaseException     if the query fails
     */
    public void ensureDocumentAvailable(String documentNumber) throws BusinessRuleException, DatabaseException {
        ensureDocumentAvailable(documentNumber, 0);
    }

    /**
     * Checks that no student other than the given one uses the document number (used when editing).
     *
     * @param documentNumber    the (already validated) document number
     * @param excludedStudentId id of the student being edited
     * @throws BusinessRuleException if another student already has it
     * @throws DatabaseException     if the query fails
     */
    public void ensureDocumentAvailable(String documentNumber, int excludedStudentId)
            throws BusinessRuleException, DatabaseException {
        if (studentDao.existsByDocumentNumber(documentNumber, excludedStudentId)) {
            throw new BusinessRuleException("Ya existe un estudiante con el documento " + documentNumber + ".");
        }
    }

    /**
     * Registers a student together with its guardians in a single transaction.
     * The student must have at least one guardian.
     *
     * @param student     the student to register
     * @param assignments the guardians (new guardians have id 0)
     * @return the generated student id
     * @throws ErpException if the data is invalid, a rule is broken or the database fails
     */
    public int registerStudent(Student student, List<GuardianAssignment> assignments) throws ErpException {
        validateStudent(student);
        if (assignments == null || assignments.isEmpty()) {
            throw new BusinessRuleException("El estudiante debe tener al menos un acudiente.");
        }
        for (GuardianAssignment assignment : assignments) {
            if (assignment.getGuardian().getGuardianId() == 0) {
                GuardianController.validateGuardian(assignment.getGuardian());
            }
        }
        if (studentDao.existsByDocumentNumber(student.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe un estudiante con el documento "
                    + student.getDocumentNumber() + ".");
        }
        return studentGuardianDao.registerStudentWithGuardians(student, assignments);
    }

    /**
     * Updates the data of an existing student.
     *
     * @param student the student with the new data
     * @throws ErpException if the data is invalid, the document belongs to another student or the database fails
     */
    public void updateStudent(Student student) throws ErpException {
        validateStudent(student);
        if (studentDao.existsByDocumentNumber(student.getDocumentNumber(), student.getStudentId())) {
            throw new BusinessRuleException("Ya existe otro estudiante con el documento "
                    + student.getDocumentNumber() + ".");
        }
        studentDao.update(student);
    }

    /**
     * Changes the enrollment status of a student.
     *
     * @param studentId id of the student
     * @param newStatus the new status
     * @throws ErpException if the student does not exist, already has that status or the database fails
     */
    public void changeStatus(int studentId, StudentStatus newStatus) throws ErpException {
        Student student = studentDao.findById(studentId);
        if (student.getStatus() == newStatus) {
            throw new BusinessRuleException("El estudiante ya se encuentra en estado " + newStatus.getLabel() + ".");
        }
        studentDao.changeStatus(studentId, newStatus);
    }

    public Student getById(int studentId) throws DatabaseException, EntityNotFoundException {
        return studentDao.findById(studentId);
    }

    public List<Student> listAll() throws DatabaseException {
        return studentDao.findAll();
    }

    public List<Student> listByStatus(StudentStatus status) throws DatabaseException {
        return studentDao.findByStatus(status);
    }

    /**
     * Searches students by name or document.
     *
     * @param text text to look for
     * @return matching students
     * @throws ErpException if the text is empty or the database fails
     */
    public List<Student> search(String text) throws ErpException {
        return studentDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public List<GuardianAssignment> getGuardiansOf(int studentId) throws DatabaseException {
        return studentGuardianDao.findByStudent(studentId);
    }
}
