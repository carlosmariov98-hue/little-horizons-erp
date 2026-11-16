package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for the academic structure: grade enrollment (matrícula) and registration in subjects (inscripción).
 */
public class AcademicController {

    private static final int FIRST_YEAR = 2000;

    private final StudentDao studentDao;
    private final GradeDao gradeDao;
    private final SubjectDao subjectDao;
    private final EnrollmentDao enrollmentDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;

    public AcademicController(StudentDao studentDao, GradeDao gradeDao, SubjectDao subjectDao,
                              EnrollmentDao enrollmentDao, SubjectEnrollmentDao subjectEnrollmentDao) {
        this.studentDao = studentDao;
        this.gradeDao = gradeDao;
        this.subjectDao = subjectDao;
        this.enrollmentDao = enrollmentDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
    }

    private static void validateYear(int year) throws ValidationException {
        Validator.requireIntInRange(year, FIRST_YEAR, LocalDate.now().getYear() + 1, "Año académico");
    }

    private static String cleanSection(String section) throws ValidationException {
        String text = Validator.requireText(section, "Sección", 1).toUpperCase();
        if (!text.matches("^[A-Z]$")) {
            throw new ValidationException("La sección debe ser una letra de la A a la Z.");
        }
        return text;
    }

    public List<Grade> listGrades() throws DatabaseException {
        return gradeDao.findAll();
    }

    /**
     * Lists the subjects of a grade.
     *
     * @param gradeId id of the grade
     * @return the subjects of the grade
     * @throws EntityNotFoundException if the grade does not exist
     * @throws DatabaseException       if the query fails
     */
    public List<Subject> listSubjectsOfGrade(int gradeId) throws DatabaseException, EntityNotFoundException {
        gradeDao.findById(gradeId);
        return subjectDao.findByGrade(gradeId);
    }

    /**
     * Enrolls a student in a grade for an academic year. Only active students can be enrolled, and only once per year.
     *
     * @param studentId id of the student
     * @param gradeId   id of the grade
     * @param year      academic year
     * @param date      enrollment date
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public void enrollStudentInGrade(int studentId, int gradeId, int year, LocalDate date) throws ErpException {
        validateYear(year);
        Validator.requirePastOrToday(date, "Fecha de matrícula");
        Student student = studentDao.findById(studentId);
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Solo se puede matricular a estudiantes activos (estado actual: "
                    + student.getStatus().getLabel() + ").");
        }
        Grade grade = gradeDao.findById(gradeId);
        if (enrollmentDao.findByStudentAndYear(studentId, year).isPresent()) {
            throw new BusinessRuleException("El estudiante ya tiene una matrícula para el año " + year + ".");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setGradeId(grade.getGradeId());
        enrollment.setYear(year);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrollmentDate(date);
        enrollmentDao.insert(enrollment);
    }

    public List<Enrollment> getEnrollments(int studentId) throws DatabaseException {
        return enrollmentDao.findByStudent(studentId);
    }

    /**
     * Changes the status of the enrollment of a student for a year. A withdrawal records today as the withdrawal date.
     *
     * @param studentId id of the student
     * @param year      academic year
     * @param newStatus the new status
     * @throws ErpException if the student has no enrollment that year, it already has that status, or the database fails
     */
    public void changeEnrollmentStatus(int studentId, int year, EnrollmentStatus newStatus) throws ErpException {
        validateYear(year);
        Enrollment enrollment = enrollmentDao.findByStudentAndYear(studentId, year).orElseThrow(
                () -> new EntityNotFoundException("El estudiante no tiene una matrícula para el año " + year + "."));
        if (enrollment.getStatus() == newStatus) {
            throw new BusinessRuleException("La matrícula ya se encuentra en estado " + newStatus.getLabel() + ".");
        }
        LocalDate withdrawalDate = newStatus == EnrollmentStatus.WITHDRAWN ? LocalDate.now() : null;
        enrollmentDao.updateStatus(enrollment.getEnrollmentId(), newStatus, withdrawalDate);
    }

    private Enrollment requireActiveEnrollment(int studentId, int year) throws ErpException {
        Enrollment enrollment = enrollmentDao.findByStudentAndYear(studentId, year).orElseThrow(
                () -> new BusinessRuleException("El estudiante no tiene una matrícula para el año " + year + "."));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("La matrícula " + year + " del estudiante está en estado "
                    + enrollment.getStatus().getLabel() + ".");
        }
        return enrollment;
    }

    /**
     * Registers a student in all the subjects of the grade he or she is enrolled in.
     *
     * @param studentId id of the student
     * @param year      academic year
     * @param period    academic period (I to IV)
     * @param section   section (A to Z)
     * @return the number of subjects the student was registered in
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public int enrollInAllSubjects(int studentId, int year, AcademicPeriod period, String section) throws ErpException {
        validateYear(year);
        if (!period.isRegular()) {
            throw new ValidationException("Solo se puede inscribir en los períodos I, II, III o IV.");
        }
        String cleanSection = cleanSection(section);
        studentDao.findById(studentId);
        Enrollment enrollment = requireActiveEnrollment(studentId, year);
        int inserted = subjectEnrollmentDao.enrollInAllSubjects(studentId, enrollment.getGradeId(), year, period,
                cleanSection, LocalDate.now());
        if (inserted == 0) {
            throw new BusinessRuleException("El estudiante ya estaba inscrito en todas las materias de "
                    + enrollment.getGradeName() + " para el período " + period.getLabel() + " de " + year + ".");
        }
        return inserted;
    }

    /**
     * Registers a student in one subject, which must belong to the grade the student is enrolled in.
     *
     * @param studentId id of the student
     * @param subjectId id of the subject
     * @param year      academic year
     * @param period    academic period (I to IV)
     * @param section   section (A to Z)
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public void enrollInSubject(int studentId, int subjectId, int year, AcademicPeriod period, String section)
            throws ErpException {
        validateYear(year);
        if (!period.isRegular()) {
            throw new ValidationException("Solo se puede inscribir en los períodos I, II, III o IV.");
        }
        String cleanSection = cleanSection(section);
        studentDao.findById(studentId);
        Enrollment enrollment = requireActiveEnrollment(studentId, year);
        Subject subject = subjectDao.findById(subjectId);
        if (!subject.isActive()) {
            throw new BusinessRuleException("La materia " + subject.getName() + " no está activa.");
        }
        if (subject.getGradeId() != enrollment.getGradeId()) {
            throw new BusinessRuleException("La materia " + subject.getName() + " pertenece a " + subject.getGradeName()
                    + " y el estudiante está matriculado en " + enrollment.getGradeName() + ".");
        }
        SubjectEnrollment registration = new SubjectEnrollment();
        registration.setStudentId(studentId);
        registration.setSubjectId(subjectId);
        registration.setYear(year);
        registration.setPeriod(period);
        registration.setSection(cleanSection);
        registration.setEnrollmentDate(LocalDate.now());
        registration.setStatus(EnrollmentStatus.ACTIVE);
        subjectEnrollmentDao.insert(registration);
    }

    public List<SubjectEnrollment> getSubjectEnrollments(int studentId, int year) throws DatabaseException {
        return subjectEnrollmentDao.findByStudent(studentId, year);
    }

    /**
     * Lists the students with an active enrollment in a grade and year.
     *
     * @param gradeId id of the grade
     * @param year    academic year
     * @return the students of the course
     * @throws ErpException if the grade does not exist, the year is invalid, or the database fails
     */
    public List<Student> getCourse(int gradeId, int year) throws ErpException {
        validateYear(year);
        gradeDao.findById(gradeId);
        return enrollmentDao.findStudentsByGradeAndYear(gradeId, year);
    }
}