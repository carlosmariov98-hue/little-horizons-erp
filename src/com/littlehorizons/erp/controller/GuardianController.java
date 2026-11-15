package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianAssignment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.util.Validator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for guardians: validation of their data and the operations the menus can request.
 */
public class GuardianController {

    /** Relationships offered in the menus. */
    public static final List<String> RELATIONSHIPS = Arrays.asList("Padre", "Madre", "Abuelo", "Abuela", "Tío",
            "Tía", "Hermano", "Hermana", "Tutor Legal");

    /** Document types a guardian may have (the table does not accept RC or TI). */
    public static final List<DocumentType> DOCUMENT_TYPES = Arrays.asList(DocumentType.CITIZENSHIP_CARD,
            DocumentType.FOREIGNER_ID, DocumentType.PASSPORT);

    private final GuardianDao guardianDao;
    private final StudentGuardianDao studentGuardianDao;

    public GuardianController(GuardianDao guardianDao, StudentGuardianDao studentGuardianDao) {
        this.guardianDao = guardianDao;
        this.studentGuardianDao = studentGuardianDao;
    }

    /**
     * Validates and cleans all the data of a guardian, replacing each field with its cleaned value.
     *
     * @param guardian the guardian to validate
     * @throws ValidationException with a user-friendly message when a field is invalid
     */
    public static void validateGuardian(Guardian guardian) throws ValidationException {
        Validator.requireNotNull(guardian.getDocumentType(), "Tipo de documento");
        if (!DOCUMENT_TYPES.contains(guardian.getDocumentType())) {
            throw new ValidationException("El tipo de documento de un acudiente debe ser CC, CE o Pasaporte.");
        }
        guardian.setDocumentNumber(Validator.requireDocumentNumber(guardian.getDocumentNumber()));
        guardian.setFirstName(Validator.requireName(guardian.getFirstName(), "Primer nombre"));
        guardian.setMiddleName(Validator.optionalName(guardian.getMiddleName(), "Segundo nombre"));
        guardian.setFirstLastName(Validator.requireName(guardian.getFirstLastName(), "Primer apellido"));
        guardian.setSecondLastName(Validator.optionalName(guardian.getSecondLastName(), "Segundo apellido"));
        String relationship = Validator.requireText(guardian.getRelationship(), "Parentesco", 50);
        if (!RELATIONSHIPS.contains(relationship)) {
            throw new ValidationException("El parentesco debe ser uno de: " + String.join(", ", RELATIONSHIPS) + ".");
        }
        guardian.setRelationship(relationship);
        guardian.setPrimaryPhone(Validator.requirePhone(guardian.getPrimaryPhone(), "Teléfono principal"));
        guardian.setSecondaryPhone(Validator.optionalPhone(guardian.getSecondaryPhone(), "Teléfono secundario"));
        guardian.setEmail(Validator.optionalEmail(guardian.getEmail()));
        guardian.setAddress(Validator.optionalText(guardian.getAddress(), "Dirección", 255));
        guardian.setOccupation(Validator.optionalText(guardian.getOccupation(), "Ocupación", 100));
    }

    /**
     * Registers a new guardian.
     *
     * @param guardian the guardian to save
     * @return the generated id
     * @throws ErpException if the data is invalid, the document already exists or the database fails
     */
    public int registerGuardian(Guardian guardian) throws ErpException {
        validateGuardian(guardian);
        if (guardianDao.findByDocumentNumber(guardian.getDocumentNumber()).isPresent()) {
            throw new BusinessRuleException("Ya existe un acudiente con el documento "
                    + guardian.getDocumentNumber() + ".");
        }
        return guardianDao.insert(guardian);
    }

    /**
     * Updates the data of an existing guardian.
     *
     * @param guardian the guardian with the new data
     * @throws ErpException if the data is invalid, the document belongs to another guardian or the database fails
     */
    public void updateGuardian(Guardian guardian) throws ErpException {
        validateGuardian(guardian);
        Optional<Guardian> sameDocument = guardianDao.findByDocumentNumber(guardian.getDocumentNumber());
        if (sameDocument.isPresent() && sameDocument.get().getGuardianId() != guardian.getGuardianId()) {
            throw new BusinessRuleException("Ya existe otro acudiente con el documento "
                    + guardian.getDocumentNumber() + ".");
        }
        guardianDao.update(guardian);
    }

    /**
     * Looks a guardian up by document number.
     *
     * @param documentNumber the document number typed by the user
     * @return the guardian, or an empty Optional when there is none
     * @throws ErpException if the document is invalid or the database fails
     */
    public Optional<Guardian> findByDocument(String documentNumber) throws ErpException {
        return guardianDao.findByDocumentNumber(Validator.requireDocumentNumber(documentNumber));
    }

    /**
     * Searches guardians by name or document.
     *
     * @param text text to look for
     * @return matching guardians
     * @throws ErpException if the text is empty or the database fails
     */
    public List<Guardian> search(String text) throws ErpException {
        return guardianDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    /**
     * Lists the students a guardian is responsible for.
     *
     * @param guardianId id of the guardian
     * @return the students of the guardian
     * @throws DatabaseException if the query fails
     */
    public List<Student> getStudentsOf(int guardianId) throws DatabaseException {
        return studentGuardianDao.findStudentsByGuardian(guardianId);
    }

    /**
     * Links a guardian to an existing student.
     *
     * @param studentId  id of the student
     * @param assignment guardian and link options
     * @throws ErpException if the data is invalid, the link already exists, a record is missing or the database fails
     */
    public void linkToStudent(int studentId, GuardianAssignment assignment) throws ErpException {
        if (assignment.getGuardian().getGuardianId() == 0) {
            validateGuardian(assignment.getGuardian());
        }
        studentGuardianDao.linkGuardian(studentId, assignment);
    }

    /**
     * Gets a guardian by id.
     *
     * @param guardianId id of the guardian
     * @return the guardian
     * @throws EntityNotFoundException if it does not exist
     * @throws DatabaseException       if the query fails
     */
    public Guardian getById(int guardianId) throws DatabaseException, EntityNotFoundException {
        return guardianDao.findById(guardianId);
    }
}
