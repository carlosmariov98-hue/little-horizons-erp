package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for the table "inscripciones_materia" (registration of students in subjects).
 */
public class SubjectEnrollmentDao extends BaseDao {

    private static final String DUPLICATE = "El estudiante ya está inscrito en esa materia para ese año y período.";

    public SubjectEnrollmentDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Registers a student in one subject.
     *
     * @param enrollment the registration to save
     * @throws BusinessRuleException   if the student is already registered in that subject for that year and period
     * @throws EntityNotFoundException if the student or the subject does not exist
     * @throws DatabaseException       if the registration cannot be saved
     */
    public void insert(SubjectEnrollment enrollment)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        executeUpdate("INSERT INTO inscripciones_materia (id_estudiante, id_materia, anio_academico, "
                        + "periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)", statement -> {
                    statement.setInt(1, enrollment.getStudentId());
                    statement.setInt(2, enrollment.getSubjectId());
                    statement.setInt(3, enrollment.getYear());
                    statement.setString(4, enrollment.getPeriod().getLabel());
                    statement.setString(5, enrollment.getSection());
                    statement.setDate(6, toSqlDate(enrollment.getEnrollmentDate()));
                    statement.setString(7, enrollment.getStatus().getLabel());
                }, DUPLICATE, "Could not register student id " + enrollment.getStudentId() + " in a subject.",
                "No se pudo inscribir al estudiante en la materia.");
    }

    /**
     * Registers a student in every active subject of a grade for a year and period, in a single statement.
     * Subjects the student is already registered in are skipped.
     *
     * @param studentId id of the student
     * @param gradeId   grade whose subjects are used
     * @param year      academic year
     * @param period    academic period
     * @param section   section (group) of the student
     * @param date      registration date
     * @return the number of subjects the student was registered in
     * @throws DatabaseException if the registration fails
     */
    public int enrollInAllSubjects(int studentId, int gradeId, int year, AcademicPeriod period, String section,
                                   LocalDate date) throws DatabaseException {
        String sql = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, anio_academico, "
                + "periodo_academico, seccion, fecha_inscripcion, estado) SELECT ?, m.id_materia, ?, ?, ?, ?, 'Activa' "
                + "FROM materias m WHERE m.id_grado = ? AND m.esta_activa = 1 AND NOT EXISTS (SELECT 1 FROM "
                + "inscripciones_materia i WHERE i.id_estudiante = ? AND i.id_materia = m.id_materia "
                + "AND i.anio_academico = ? AND i.periodo_academico = ?)";
        try {
            return executeUpdate(sql, statement -> {
                        statement.setInt(1, studentId);
                        statement.setInt(2, year);
                        statement.setString(3, period.getLabel());
                        statement.setString(4, section);
                        statement.setDate(5, toSqlDate(date));
                        statement.setInt(6, gradeId);
                        statement.setInt(7, studentId);
                        statement.setInt(8, year);
                        statement.setString(9, period.getLabel());
                    }, DUPLICATE, "Could not register student id " + studentId + " in the subjects of grade " + gradeId + ".",
                    "No se pudo inscribir al estudiante en las materias del grado.");
        } catch (BusinessRuleException | EntityNotFoundException e) {
            throw new DatabaseException("No se pudo inscribir al estudiante en las materias del grado.", e);
        }
    }

    public List<SubjectEnrollment> findByStudent(int studentId, int year) throws DatabaseException {
        return queryList("SELECT i.id_inscripcion, i.id_estudiante, i.id_materia, m.nombre_materia, i.anio_academico, "
                        + "i.periodo_academico, i.seccion, i.fecha_inscripcion, i.estado FROM inscripciones_materia i "
                        + "JOIN materias m ON m.id_materia = i.id_materia WHERE i.id_estudiante = ? AND i.anio_academico = ? "
                        + "ORDER BY FIELD(i.periodo_academico, 'I', 'II', 'III', 'IV'), m.nombre_materia", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, year);
                }, this::mapRow, "Could not list subject registrations of student id " + studentId + ".",
                "No se pudo consultar las inscripciones del estudiante.");
    }

    private SubjectEnrollment mapRow(ResultSet resultSet) throws SQLException {
        SubjectEnrollment enrollment = new SubjectEnrollment();
        enrollment.setId(resultSet.getInt("id_inscripcion"));
        enrollment.setStudentId(resultSet.getInt("id_estudiante"));
        enrollment.setSubjectId(resultSet.getInt("id_materia"));
        enrollment.setSubjectName(resultSet.getString("nombre_materia"));
        enrollment.setYear(resultSet.getInt("anio_academico"));
        try {
            enrollment.setPeriod(AcademicPeriod.fromLabel(resultSet.getString("periodo_academico")));
            enrollment.setStatus(EnrollmentStatus.fromLabel(resultSet.getString("estado")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table inscripciones_materia: " + e.getMessage(), e);
        }
        enrollment.setSection(resultSet.getString("seccion"));
        enrollment.setEnrollmentDate(toLocalDate(resultSet.getDate("fecha_inscripcion")));
        return enrollment;
    }
}