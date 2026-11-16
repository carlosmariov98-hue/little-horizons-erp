package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the table "matriculas" (enrollment of a student in a grade for a year).
 */
public class EnrollmentDao extends BaseDao {

    private static final String SELECT = "SELECT m.id_matricula, m.id_estudiante, m.id_grado, g.nombre_grado, "
            + "m.anio_academico, m.estado_matricula, m.fecha_matricula, m.fecha_retiro, m.observaciones "
            + "FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado";

    private final StudentDao studentDao;

    public EnrollmentDao(ConnectionProvider connectionProvider, StudentDao studentDao) {
        super(connectionProvider);
        this.studentDao = studentDao;
    }

    /**
     * Saves a new enrollment.
     *
     * @param enrollment the enrollment to save
     * @throws BusinessRuleException   if the student already has an enrollment for that year
     * @throws EntityNotFoundException if the student or the grade does not exist
     * @throws DatabaseException       if the enrollment cannot be saved
     */
    public void insert(Enrollment enrollment) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        executeUpdate("INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, estado_matricula, "
                        + "fecha_matricula, fecha_retiro, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?)", statement -> {
                    statement.setInt(1, enrollment.getStudentId());
                    statement.setInt(2, enrollment.getGradeId());
                    statement.setInt(3, enrollment.getYear());
                    statement.setString(4, enrollment.getStatus().getLabel());
                    statement.setDate(5, toSqlDate(enrollment.getEnrollmentDate()));
                    statement.setDate(6, toSqlDate(enrollment.getWithdrawalDate()));
                    statement.setString(7, blankToNull(enrollment.getNotes()));
                }, "El estudiante ya tiene una matrícula para ese año.",
                "Could not enroll student id " + enrollment.getStudentId() + ".", "No se pudo registrar la matrícula.");
    }

    public List<Enrollment> findByStudent(int studentId) throws DatabaseException {
        return queryList(SELECT + " WHERE m.id_estudiante = ? ORDER BY m.anio_academico DESC",
                statement -> statement.setInt(1, studentId), this::mapRow,
                "Could not list enrollments of student id " + studentId + ".",
                "No se pudo consultar las matrículas del estudiante.");
    }

    /**
     * Finds the enrollment of a student for a year. A student may have none, so an Optional is returned.
     *
     * @param studentId id of the student
     * @param year      academic year
     * @return the enrollment, or an empty Optional
     * @throws DatabaseException if the query fails
     */
    public Optional<Enrollment> findByStudentAndYear(int studentId, int year) throws DatabaseException {
        return queryList(SELECT + " WHERE m.id_estudiante = ? AND m.anio_academico = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, year);
                }, this::mapRow, "Could not find enrollment of student id " + studentId + " for " + year + ".",
                "No se pudo consultar la matrícula del estudiante.").stream().findFirst();
    }

    /**
     * Changes the status of an enrollment and, for withdrawals, records the withdrawal date.
     *
     * @param enrollmentId   id of the enrollment
     * @param status         the new status
     * @param withdrawalDate withdrawal date (null unless the status is Retirada)
     * @throws EntityNotFoundException if the enrollment does not exist
     * @throws DatabaseException       if the status cannot be changed
     */
    public void updateStatus(int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate)
            throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? WHERE id_matricula = ?",
                    statement -> {
                        statement.setString(1, status.getLabel());
                        statement.setDate(2, toSqlDate(withdrawalDate));
                        statement.setInt(3, enrollmentId);
                    }, "", "Could not update enrollment id " + enrollmentId + ".",
                    "No se pudo actualizar la matrícula.");
            if (rows == 0) {
                requireExists("matriculas", "id_matricula", enrollmentId,
                        "No se encontró una matrícula con el id " + enrollmentId + ".");
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo actualizar la matrícula.", e);
        }
    }

    /**
     * Lists the students with an active enrollment in a grade and year (the "course").
     *
     * @param gradeId id of the grade
     * @param year    academic year
     * @return the students of the course ordered by name
     * @throws DatabaseException if the query fails
     */
    public List<Student> findStudentsByGradeAndYear(int gradeId, int year) throws DatabaseException {
        String sql = "SELECT " + StudentDao.columnsWithAlias("e") + " FROM matriculas m "
                + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante WHERE m.id_grado = ? "
                + "AND m.anio_academico = ? AND m.estado_matricula = 'Activa' "
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";
        return queryList(sql, statement -> {
                    statement.setInt(1, gradeId);
                    statement.setInt(2, year);
                }, studentDao::mapRow, "Could not list students of grade id " + gradeId + " for " + year + ".",
                "No se pudo consultar los estudiantes del curso.");
    }

    private Enrollment mapRow(ResultSet resultSet) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(resultSet.getInt("id_matricula"));
        enrollment.setStudentId(resultSet.getInt("id_estudiante"));
        enrollment.setGradeId(resultSet.getInt("id_grado"));
        enrollment.setGradeName(resultSet.getString("nombre_grado"));
        enrollment.setYear(resultSet.getInt("anio_academico"));
        try {
            enrollment.setStatus(EnrollmentStatus.fromLabel(resultSet.getString("estado_matricula")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table matriculas: " + e.getMessage(), e);
        }
        enrollment.setEnrollmentDate(toLocalDate(resultSet.getDate("fecha_matricula")));
        enrollment.setWithdrawalDate(toLocalDate(resultSet.getDate("fecha_retiro")));
        enrollment.setNotes(resultSet.getString("observaciones"));
        return enrollment;
    }
}