package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for teachers and for the subjects assigned to them.
 */
public class TeacherController {

    private static final int FIRST_YEAR = 2000;

    private final TeacherDao teacherDao;
    private final AssignmentDao assignmentDao;
    private final SubjectDao subjectDao;

    public TeacherController(TeacherDao teacherDao, AssignmentDao assignmentDao, SubjectDao subjectDao) {
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
        this.subjectDao = subjectDao;
    }

    /**
     * Validates and cleans all the data of a teacher, replacing each field with its cleaned value.
     *
     * @param teacher the teacher to validate
     * @throws ValidationException with a user-friendly message when a field is invalid
     */
    public void validateTeacher(Teacher teacher) throws ValidationException {
        Validator.requireNotNull(teacher.getDocumentType(), "Tipo de documento");
        if (!GuardianController.DOCUMENT_TYPES.contains(teacher.getDocumentType())) {
            throw new ValidationException("El tipo de documento de un profesor debe ser CC, CE o Pasaporte.");
        }
        teacher.setDocumentNumber(Validator.requireDocumentNumber(teacher.getDocumentNumber()));
        teacher.setFirstName(Validator.requireName(teacher.getFirstName(), "Primer nombre"));
        teacher.setMiddleName(Validator.optionalName(teacher.getMiddleName(), "Segundo nombre"));
        teacher.setFirstLastName(Validator.requireName(teacher.getFirstLastName(), "Primer apellido"));
        teacher.setSecondLastName(Validator.optionalName(teacher.getSecondLastName(), "Segundo apellido"));
        teacher.setEmail(Validator.requireEmail(teacher.getEmail()));
        teacher.setPhone(Validator.optionalPhone(teacher.getPhone(), "Teléfono"));
        teacher.setSpecialization(Validator.requireText(teacher.getSpecialization(), "Especialización", 100));
        Validator.requirePastOrToday(teacher.getHireDate(), "Fecha de contratación");
        Validator.requireNotNull(teacher.getStatus(), "Estado");
    }

    /**
     * Checks that no other teacher uses the document number or the email.
     *
     * @param teacher           teacher with the (validated) document and email
     * @param excludedTeacherId id of the teacher being edited (0 for a new teacher)
     * @throws BusinessRuleException if the document or the email belongs to another teacher
     * @throws DatabaseException     if a query fails
     */
    public void ensureUnique(Teacher teacher, int excludedTeacherId) throws BusinessRuleException, DatabaseException {
        ensureDocumentAvailable(teacher.getDocumentNumber(), excludedTeacherId);
        ensureEmailAvailable(teacher.getEmail(), excludedTeacherId);
    }

    public void ensureDocumentAvailable(String documentNumber, int excludedTeacherId)
            throws BusinessRuleException, DatabaseException {
        if (teacherDao.existsByDocumentNumber(documentNumber, excludedTeacherId)) {
            throw new BusinessRuleException("Ya existe un profesor con el documento " + documentNumber + ".");
        }
    }

    public void ensureEmailAvailable(String email, int excludedTeacherId) throws BusinessRuleException, DatabaseException {
        if (teacherDao.existsByEmail(email, excludedTeacherId)) {
            throw new BusinessRuleException("Ya existe un profesor con el correo " + email + ".");
        }
    }

    /**
     * Registers a new teacher.
     *
     * @param teacher the teacher to register
     * @return the generated id
     * @throws ErpException if the data is invalid, the document or email is used, or the database fails
     */
    public int registerTeacher(Teacher teacher) throws ErpException {
        validateTeacher(teacher);
        ensureUnique(teacher, 0);
        return teacherDao.insert(teacher);
    }

    /**
     * Updates the data of an existing teacher.
     *
     * @param teacher the teacher with the new data
     * @throws ErpException if the data is invalid, the document or email belongs to another teacher, or the database fails
     */
    public void updateTeacher(Teacher teacher) throws ErpException {
        validateTeacher(teacher);
        ensureUnique(teacher, teacher.getTeacherId());
        teacherDao.update(teacher);
    }

    /**
     * Changes the status of a teacher.
     *
     * @param teacherId id of the teacher
     * @param newStatus the new status
     * @throws ErpException if the teacher does not exist, already has that status, or the database fails
     */
    public void changeStatus(int teacherId, TeacherStatus newStatus) throws ErpException {
        Teacher teacher = teacherDao.findById(teacherId);
        if (teacher.getStatus() == newStatus) {
            throw new BusinessRuleException("El profesor ya se encuentra en estado " + newStatus.getLabel() + ".");
        }
        teacherDao.changeStatus(teacherId, newStatus);
    }

    /**
     * Assigns a subject to a teacher for a year and section. Only active teachers and active subjects are allowed.
     *
     * @param teacherId id of the teacher
     * @param subjectId id of the subject
     * @param year      academic year
     * @param section   section letter (A to Z)
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public void assignSubject(int teacherId, int subjectId, int year, String section) throws ErpException {
        Validator.requireIntInRange(year, FIRST_YEAR, LocalDate.now().getYear() + 1, "Año académico");
        String cleanSection = Validator.requireText(section, "Sección", 1).toUpperCase();
        if (!cleanSection.matches("^[A-Z]$")) {
            throw new ValidationException("La sección debe ser una letra de la A a la Z.");
        }
        Teacher teacher = teacherDao.findById(teacherId);
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Solo se pueden asignar materias a profesores activos (estado actual: "
                    + teacher.getStatus().getLabel() + ").");
        }
        Subject subject = subjectDao.findById(subjectId);
        if (!subject.isActive()) {
            throw new BusinessRuleException("La materia " + subject.getName() + " no está activa.");
        }
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setTeacherId(teacherId);
        assignment.setSubjectId(subjectId);
        assignment.setYear(year);
        assignment.setSection(cleanSection);
        assignmentDao.insert(assignment);
    }

    public Teacher getById(int teacherId) throws DatabaseException, EntityNotFoundException {
        return teacherDao.findById(teacherId);
    }

    public List<Teacher> listAll() throws DatabaseException {
        return teacherDao.findAll();
    }

    public List<Teacher> listByStatus(TeacherStatus status) throws DatabaseException {
        return teacherDao.findByStatus(status);
    }

    /**
     * Searches teachers by name, document, email or specialization.
     *
     * @param text text to look for
     * @return matching teachers
     * @throws ErpException if the text is empty or the database fails
     */
    public List<Teacher> search(String text) throws ErpException {
        return teacherDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public List<TeachingAssignment> getAssignments(int teacherId, int year) throws DatabaseException {
        return assignmentDao.findByTeacher(teacherId, year);
    }

    public List<TeachingAssignment> getTeachersOfSubject(int subjectId, int year) throws DatabaseException {
        return assignmentDao.findBySubject(subjectId, year);
    }

    public Subject getSubject(int subjectId) throws DatabaseException, EntityNotFoundException {
        return subjectDao.findById(subjectId);
    }
}