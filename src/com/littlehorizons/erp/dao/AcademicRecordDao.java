package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.enums.AcademicPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the table "registros_academicos" (grades).
 */
public class AcademicRecordDao extends BaseDao {

    private static final String SELECT = "SELECT r.id_registro, r.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre_estudiante, "
            + "r.id_materia, m.nombre_materia, r.id_profesor, "
            + "CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS nombre_profesor, r.anio_academico, "
            + "r.periodo_academico, r.valor_nota, r.observaciones, r.fecha_registro FROM registros_academicos r "
            + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante JOIN materias m ON m.id_materia = r.id_materia "
            + "JOIN profesores p ON p.id_profesor = r.id_profesor";
    private static final String ORDER_BY_PERIOD = "FIELD(r.periodo_academico, 'I', 'II', 'III', 'IV', 'Final')";

    public AcademicRecordDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new grade.
     *
     * @param record the grade to save
     * @throws BusinessRuleException   if the student already has a grade for that subject, year and period
     * @throws EntityNotFoundException if the student, subject or teacher does not exist
     * @throws DatabaseException       if the grade cannot be saved
     */
    public void insert(AcademicRecord record) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        executeUpdate("INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, anio_academico, "
                        + "periodo_academico, valor_nota, observaciones, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, record.getStudentId());
                    statement.setInt(2, record.getSubjectId());
                    statement.setInt(3, record.getTeacherId());
                    statement.setInt(4, record.getYear());
                    statement.setString(5, record.getPeriod().getLabel());
                    statement.setBigDecimal(6, record.getScore());
                    statement.setString(7, blankToNull(record.getObservations()));
                    statement.setDate(8, toSqlDate(record.getRecordDate()));
                }, "El estudiante ya tiene una nota en esa materia para ese año y período.",
                "Could not insert grade for student id " + record.getStudentId() + ".",
                "No se pudo guardar la nota.");
    }

    /**
     * Changes the score of an existing grade.
     *
     * @param recordId     id of the grade
     * @param score        the new score
     * @param observations new observations (may be null)
     * @param teacherId    teacher who makes the correction
     * @throws EntityNotFoundException if the grade does not exist
     * @throws DatabaseException       if the grade cannot be updated
     */
    public void updateScore(int recordId, java.math.BigDecimal score, String observations, int teacherId)
            throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE registros_academicos SET valor_nota = ?, observaciones = ?, "
                    + "id_profesor = ?, fecha_registro = CURRENT_DATE WHERE id_registro = ?", statement -> {
                statement.setBigDecimal(1, score);
                statement.setString(2, blankToNull(observations));
                statement.setInt(3, teacherId);
                statement.setInt(4, recordId);
            }, "", "Could not update grade id " + recordId + ".", "No se pudo corregir la nota.");
            if (rows == 0) {
                requireExists("registros_academicos", "id_registro", recordId,
                        "No se encontró la nota con el id " + recordId + ".");
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo corregir la nota.", e);
        }
    }

    public Optional<AcademicRecord> find(int studentId, int subjectId, int year, AcademicPeriod period)
            throws DatabaseException {
        return queryList(SELECT + " WHERE r.id_estudiante = ? AND r.id_materia = ? AND r.anio_academico = ? "
                        + "AND r.periodo_academico = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, year);
                    statement.setString(4, period.getLabel());
                }, this::mapRow, "Could not find grade of student id " + studentId + ".",
                "No se pudo consultar la nota.").stream().findFirst();
    }

    /** All the grades of a student in one year, ordered by period and subject. */
    public List<AcademicRecord> findByStudentAndYear(int studentId, int year) throws DatabaseException {
        return queryList(SELECT + " WHERE r.id_estudiante = ? AND r.anio_academico = ? ORDER BY " + ORDER_BY_PERIOD
                        + ", m.nombre_materia", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, year);
                }, this::mapRow, "Could not list grades of student id " + studentId + ".",
                "No se pudo consultar las notas del estudiante.");
    }

    /** All the grades of a student in every year (academic history). */
    public List<AcademicRecord> findByStudent(int studentId) throws DatabaseException {
        return queryList(SELECT + " WHERE r.id_estudiante = ? ORDER BY r.anio_academico, " + ORDER_BY_PERIOD
                        + ", m.nombre_materia", statement -> statement.setInt(1, studentId), this::mapRow,
                "Could not list the academic history of student id " + studentId + ".",
                "No se pudo consultar el historial académico.");
    }

    /** Grades of one subject in one year and period, for all the students. */
    public List<AcademicRecord> findBySubject(int subjectId, int year, AcademicPeriod period) throws DatabaseException {
        return queryList(SELECT + " WHERE r.id_materia = ? AND r.anio_academico = ? AND r.periodo_academico = ? "
                        + "ORDER BY e.primer_apellido, e.primer_nombre", statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getLabel());
                }, this::mapRow, "Could not list grades of subject id " + subjectId + ".",
                "No se pudo consultar las notas de la materia.");
    }

    /** Grades of every subject of a grade (course) in one year and period. */
    public List<AcademicRecord> findByGrade(int gradeId, int year, AcademicPeriod period) throws DatabaseException {
        return queryList(SELECT + " WHERE m.id_grado = ? AND r.anio_academico = ? AND r.periodo_academico = ?",
                statement -> {
                    statement.setInt(1, gradeId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getLabel());
                }, this::mapRow, "Could not list grades of grade id " + gradeId + ".",
                "No se pudo consultar las notas del curso.");
    }

    private AcademicRecord mapRow(ResultSet resultSet) throws SQLException {
        AcademicRecord record = new AcademicRecord();
        record.setRecordId(resultSet.getInt("id_registro"));
        record.setStudentId(resultSet.getInt("id_estudiante"));
        record.setStudentName(resultSet.getString("nombre_estudiante"));
        record.setSubjectId(resultSet.getInt("id_materia"));
        record.setSubjectName(resultSet.getString("nombre_materia"));
        record.setTeacherId(resultSet.getInt("id_profesor"));
        record.setTeacherName(resultSet.getString("nombre_profesor"));
        record.setYear(resultSet.getInt("anio_academico"));
        try {
            record.setPeriod(AcademicPeriod.fromLabel(resultSet.getString("periodo_academico")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table registros_academicos: " + e.getMessage(), e);
        }
        record.setScore(resultSet.getBigDecimal("valor_nota"));
        record.setObservations(resultSet.getString("observaciones"));
        record.setRecordDate(toLocalDate(resultSet.getDate("fecha_registro")));
        return record;
    }
}