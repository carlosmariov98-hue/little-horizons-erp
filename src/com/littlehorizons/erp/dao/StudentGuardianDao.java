package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianAssignment;
import com.littlehorizons.erp.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the relationship between students and guardians (table "estudiante_acudiente").
 * It also owns the operations that must change several tables atomically, such as registering a student
 * together with its guardians: either everything is saved or nothing is.
 */
public class StudentGuardianDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_FOREIGN_KEY_FAILS = 1452;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;

    public StudentGuardianDao(ConnectionProvider connectionProvider, StudentDao studentDao, GuardianDao guardianDao) {
        super(connectionProvider);
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
    }

    /**
     * Registers a student and its guardians in a single transaction. Guardians with id 0 are created;
     * guardians with an id must already exist. Exactly one guardian ends up marked as primary.
     *
     * @param student     the student to save
     * @param assignments the guardians of the student (at least one)
     * @return the generated student id
     * @throws BusinessRuleException   if there are no guardians, or a document number is already used
     * @throws EntityNotFoundException if an existing guardian cannot be found
     * @throws DatabaseException       if a technical database error happens
     */
    public int registerStudentWithGuardians(Student student, List<GuardianAssignment> assignments)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        if (assignments == null || assignments.isEmpty()) {
            throw new BusinessRuleException("El estudiante debe tener al menos un acudiente.");
        }
        normalizePrimary(assignments);
        return inTransaction("Could not register student " + student.getDocumentNumber() + " with guardians.",
                "No se pudo registrar al estudiante.", connection -> {
                    int studentId = studentDao.insert(connection, student);
                    for (GuardianAssignment assignment : assignments) {
                        saveLink(connection, studentId, assignment);
                    }
                    return studentId;
                });
    }

    /**
     * Links a guardian (new or existing) to an already registered student.
     * When the new link is marked as primary, the previous primary guardian is demoted.
     *
     * @param studentId  id of the student
     * @param assignment the guardian and the link options
     * @throws BusinessRuleException   if the guardian is already linked to the student
     * @throws EntityNotFoundException if the student or the guardian does not exist
     * @throws DatabaseException       if a technical database error happens
     */
    public void linkGuardian(int studentId, GuardianAssignment assignment)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        inTransaction("Could not link guardian to student id " + studentId + ".",
                "No se pudo vincular al acudiente.", connection -> {
                    if (countLinks(connection, studentId) == 0) {
                        assignment.setPrimary(true);
                    }
                    if (assignment.isPrimary()) {
                        demotePrimary(connection, studentId);
                    }
                    saveLink(connection, studentId, assignment);
                    return Boolean.TRUE;
                });
    }

    /**
     * Lists the guardians of a student, the primary one first.
     *
     * @param studentId id of the student
     * @return the guardians with their link options (empty list when there are none)
     * @throws DatabaseException if the query fails
     */
    public List<GuardianAssignment> findByStudent(int studentId) throws DatabaseException {
        String sql = "SELECT " + GuardianDao.columnsWithAlias("a") + ", ea.es_principal, ea.es_contacto_emergencia "
                + "FROM estudiante_acudiente ea JOIN acudientes a ON a.id_acudiente = ea.id_acudiente "
                + "WHERE ea.id_estudiante = ? ORDER BY ea.es_principal DESC, a.primer_apellido, a.primer_nombre";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GuardianAssignment> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    Guardian guardian = guardianDao.mapRow(resultSet);
                    assignments.add(new GuardianAssignment(guardian, resultSet.getBoolean("es_principal"),
                            resultSet.getBoolean("es_contacto_emergencia")));
                }
                return assignments;
            }
        } catch (SQLException e) {
            throw databaseError("Could not list guardians of student id " + studentId + ".",
                    "No se pudieron consultar los acudientes del estudiante.", e);
        }
    }

    /**
     * Lists the students a guardian is responsible for (for example, several siblings).
     *
     * @param guardianId id of the guardian
     * @return the students of the guardian (empty list when there are none)
     * @throws DatabaseException if the query fails
     */
    public List<Student> findStudentsByGuardian(int guardianId) throws DatabaseException {
        String sql = "SELECT " + StudentDao.columnsWithAlias("e") + " FROM estudiante_acudiente ea "
                + "JOIN estudiantes e ON e.id_estudiante = ea.id_estudiante WHERE ea.id_acudiente = ? "
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guardianId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Student> students = new ArrayList<>();
                while (resultSet.next()) {
                    students.add(studentDao.mapRow(resultSet));
                }
                return students;
            }
        } catch (SQLException e) {
            throw databaseError("Could not list students of guardian id " + guardianId + ".",
                    "No se pudieron consultar los estudiantes del acudiente.", e);
        }
    }

    // makes sure exactly one assignment is primary: the first one when none is marked
    private void normalizePrimary(List<GuardianAssignment> assignments) throws BusinessRuleException {
        long primaries = assignments.stream().filter(GuardianAssignment::isPrimary).count();
        if (primaries > 1) {
            throw new BusinessRuleException("Solo un acudiente puede ser el principal.");
        }
        if (primaries == 0) {
            assignments.get(0).setPrimary(true);
        }
    }

    // saves the guardian when it is new, then inserts the link row
    private void saveLink(Connection connection, int studentId, GuardianAssignment assignment)
            throws SQLException, BusinessRuleException, EntityNotFoundException {
        Guardian guardian = assignment.getGuardian();
        if (guardian.getGuardianId() == 0) {
            guardianDao.insert(connection, guardian);
        } else if (!guardianDao.exists(connection, guardian.getGuardianId())) {
            throw new EntityNotFoundException("No se encontró un acudiente con el id " + guardian.getGuardianId() + ".");
        }
        String sql = "INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
                + "es_contacto_emergencia) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, guardian.getGuardianId());
            statement.setBoolean(3, assignment.isPrimary());
            statement.setBoolean(4, assignment.isEmergencyContact());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new BusinessRuleException("El acudiente ya está vinculado a este estudiante.", e);
            }
            if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAILS) {
                throw new EntityNotFoundException("El estudiante o el acudiente indicado no existe.");
            }
            throw e;
        }
    }

    private int countLinks(Connection connection, int studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ?")) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private void demotePrimary(Connection connection, int studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE estudiante_acudiente SET es_principal = 0 WHERE id_estudiante = ?")) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }
}
