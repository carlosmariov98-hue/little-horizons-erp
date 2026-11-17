package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.enums.AttendanceStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the table "asistencias" (daily attendance).
 */
public class AttendanceDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final String STUDENT_NAME = "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
            + "e.segundo_apellido)";
    private static final String SELECT = "SELECT a.id_asistencia, a.id_estudiante, " + STUDENT_NAME
            + " AS nombre_estudiante, a.id_materia, m.nombre_materia, a.fecha_asistencia, a.estado_asistencia, "
            + "a.observaciones, a.registrado_por FROM asistencias a "
            + "JOIN estudiantes e ON e.id_estudiante = a.id_estudiante JOIN materias m ON m.id_materia = a.id_materia";
    private static final String INSERT = "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, "
            + "estado_asistencia, observaciones, registrado_por) VALUES (?, ?, ?, ?, ?, ?)";

    public AttendanceDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    private static void bindAttendance(PreparedStatement statement, Attendance attendance) throws SQLException {
        statement.setInt(1, attendance.getStudentId());
        statement.setInt(2, attendance.getSubjectId());
        statement.setDate(3, toSqlDate(attendance.getDate()));
        statement.setString(4, attendance.getStatus().getLabel());
        statement.setString(5, blankToNull(attendance.getObservations()));
        statement.setInt(6, attendance.getRegisteredBy());
    }

    /**
     * Saves the attendance of one student.
     *
     * @param attendance the attendance to save
     * @throws BusinessRuleException   if the attendance of that student, subject and day already exists
     * @throws EntityNotFoundException if the student, subject or teacher does not exist
     * @throws DatabaseException       if the attendance cannot be saved
     */
    public void insert(Attendance attendance) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        executeUpdate(INSERT, statement -> bindAttendance(statement, attendance),
                "La asistencia de ese estudiante en esa materia y fecha ya fue registrada.",
                "Could not insert attendance of student id " + attendance.getStudentId() + ".",
                "No se pudo guardar la asistencia.");
    }

    /**
     * Saves the attendance of a whole class in one transaction: either every student is saved or none.
     *
     * @param list the attendance of each student
     * @return the number of students saved
     * @throws BusinessRuleException   if any student already has attendance for that day
     * @throws EntityNotFoundException if a referenced record does not exist
     * @throws DatabaseException       if the attendance cannot be saved (everything is rolled back)
     */
    public int insertAll(List<Attendance> list) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        return inTransaction("Could not save the attendance roll.", "No se pudo guardar la lista de asistencia.",
                connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                        for (Attendance attendance : list) {
                            bindAttendance(statement, attendance);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                        return list.size();
                    } catch (SQLException e) {
                        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                            throw new BusinessRuleException(
                                    "Alguno de los estudiantes ya tiene asistencia registrada en esa fecha.", e);
                        }
                        throw e;
                    }
                });
    }

    /**
     * Changes the status of an existing attendance.
     *
     * @param attendanceId id of the attendance
     * @param status       the new status
     * @param observations new observations (may be null)
     * @param teacherId    teacher who makes the correction
     * @throws EntityNotFoundException if the attendance does not exist
     * @throws DatabaseException       if it cannot be updated
     */
    public void updateStatus(int attendanceId, AttendanceStatus status, String observations, int teacherId)
            throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, "
                            + "registrado_por = ? WHERE id_asistencia = ?", statement -> {
                        statement.setString(1, status.getLabel());
                        statement.setString(2, blankToNull(observations));
                        statement.setInt(3, teacherId);
                        statement.setInt(4, attendanceId);
                    }, "", "Could not update attendance id " + attendanceId + ".",
                    "No se pudo corregir la asistencia.");
            if (rows == 0) {
                requireExists("asistencias", "id_asistencia", attendanceId,
                        "No se encontró la asistencia con el id " + attendanceId + ".");
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo corregir la asistencia.", e);
        }
    }

    public Optional<Attendance> find(int studentId, int subjectId, LocalDate date) throws DatabaseException {
        return queryList(SELECT + " WHERE a.id_estudiante = ? AND a.id_materia = ? AND a.fecha_asistencia = ?",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setDate(3, toSqlDate(date));
                }, this::mapRow, "Could not find attendance of student id " + studentId + ".",
                "No se pudo consultar la asistencia.").stream().findFirst();
    }

    /** Attendance of a student between two dates (both included). */
    public List<Attendance> findByStudent(int studentId, LocalDate from, LocalDate to) throws DatabaseException {
        return queryList(SELECT + " WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ? "
                        + "ORDER BY a.fecha_asistencia, m.nombre_materia", statement -> {
                    statement.setInt(1, studentId);
                    statement.setDate(2, toSqlDate(from));
                    statement.setDate(3, toSqlDate(to));
                }, this::mapRow, "Could not list attendance of student id " + studentId + ".",
                "No se pudo consultar la asistencia del estudiante.");
    }

    /** Attendance of every student in one subject on one day. */
    public List<Attendance> findBySubjectAndDate(int subjectId, LocalDate date) throws DatabaseException {
        return queryList(SELECT + " WHERE a.id_materia = ? AND a.fecha_asistencia = ? "
                        + "ORDER BY e.primer_apellido, e.primer_nombre", statement -> {
                    statement.setInt(1, subjectId);
                    statement.setDate(2, toSqlDate(date));
                }, this::mapRow, "Could not list attendance of subject id " + subjectId + ".",
                "No se pudo consultar la asistencia de la materia.");
    }

    /** Attendance of all students in all subjects between two dates (used by the weekly report). */
    public List<Attendance> findByDateRange(LocalDate from, LocalDate to) throws DatabaseException {
        return queryList(SELECT + " WHERE a.fecha_asistencia BETWEEN ? AND ? ORDER BY a.fecha_asistencia",
                statement -> {
                    statement.setDate(1, toSqlDate(from));
                    statement.setDate(2, toSqlDate(to));
                }, this::mapRow, "Could not list attendance between " + from + " and " + to + ".",
                "No se pudo consultar la asistencia del período.");
    }

    /**
     * Students that still need attendance for a subject on a day: active students with an active
     * registration in the subject that year and no attendance saved that day.
     */
    public List<Attendance> findPendingRoster(int subjectId, int year, LocalDate date) throws DatabaseException {
        String sql = "SELECT DISTINCT e.id_estudiante, " + STUDENT_NAME + " AS nombre_estudiante, e.primer_apellido, "
                + "e.primer_nombre FROM inscripciones_materia i JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
                + "WHERE i.id_materia = ? AND i.anio_academico = ? AND i.estado = 'Activa' AND e.estado = 'Activo' "
                + "AND NOT EXISTS (SELECT 1 FROM asistencias a WHERE a.id_estudiante = e.id_estudiante "
                + "AND a.id_materia = i.id_materia AND a.fecha_asistencia = ?) "
                + "ORDER BY e.primer_apellido, e.primer_nombre";
        return queryList(sql, statement -> {
            statement.setInt(1, subjectId);
            statement.setInt(2, year);
            statement.setDate(3, toSqlDate(date));
        }, resultSet -> {
            Attendance entry = new Attendance();
            entry.setStudentId(resultSet.getInt("id_estudiante"));
            entry.setStudentName(resultSet.getString("nombre_estudiante"));
            entry.setSubjectId(subjectId);
            entry.setDate(date);
            return entry;
        }, "Could not list the roster of subject id " + subjectId + ".", "No se pudo consultar la lista de estudiantes.");
    }

    private Attendance mapRow(ResultSet resultSet) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(resultSet.getInt("id_asistencia"));
        attendance.setStudentId(resultSet.getInt("id_estudiante"));
        attendance.setStudentName(resultSet.getString("nombre_estudiante"));
        attendance.setSubjectId(resultSet.getInt("id_materia"));
        attendance.setSubjectName(resultSet.getString("nombre_materia"));
        attendance.setDate(toLocalDate(resultSet.getDate("fecha_asistencia")));
        try {
            attendance.setStatus(AttendanceStatus.fromLabel(resultSet.getString("estado_asistencia")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table asistencias: " + e.getMessage(), e);
        }
        attendance.setObservations(resultSet.getString("observaciones"));
        attendance.setRegisteredBy(resultSet.getInt("registrado_por"));
        return attendance;
    }
}