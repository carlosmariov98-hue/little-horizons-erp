package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.TeacherStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Data Access Object for the table "profesores".
 */
public class TeacherDao extends BaseDao {

    private static final String SELECT_ALL = "SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, "
            + "fecha_contratacion, estado FROM profesores";
    private static final String ORDER_BY_NAME = " ORDER BY primer_apellido, segundo_apellido, primer_nombre";
    private static final String INSERT = "INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, "
            + "fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE profesores SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, "
            + "correo_electronico = ?, telefono = ?, especializacion = ?, fecha_contratacion = ?, estado = ? "
            + "WHERE id_profesor = ?";
    private static final String DUPLICATE = "Ya existe un profesor con ese documento o ese correo electrónico.";

    public TeacherDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new teacher and sets the generated id on the given object.
     *
     * @param teacher the teacher to save
     * @return the generated teacher id
     * @throws BusinessRuleException if the document number or the email is already used
     * @throws DatabaseException     if the teacher cannot be saved
     */
    public int insert(Teacher teacher) throws DatabaseException, BusinessRuleException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindTeacher(statement, teacher);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    teacher.setTeacherId(keys.getInt(1));
                }
            }
            return teacher.getTeacherId();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new BusinessRuleException(DUPLICATE, e);
            }
            throw databaseError("Could not insert teacher " + teacher.getDocumentNumber() + ".",
                    "No se pudo guardar el profesor.", e);
        }
    }

    /**
     * Updates all the data of an existing teacher.
     *
     * @param teacher the teacher with the new data; its id identifies the record
     * @throws EntityNotFoundException if the teacher does not exist
     * @throws BusinessRuleException   if the document number or the email belongs to another teacher
     * @throws DatabaseException       if the teacher cannot be updated
     */
    public void update(Teacher teacher) throws DatabaseException, EntityNotFoundException, BusinessRuleException {
        int rows = executeUpdate(UPDATE, statement -> {
                    bindTeacher(statement, teacher);
                    statement.setInt(12, teacher.getTeacherId());
                }, DUPLICATE, "Could not update teacher id " + teacher.getTeacherId() + ".",
                "No se pudo actualizar el profesor.");
        if (rows == 0) {
            requireExists("profesores", "id_profesor", teacher.getTeacherId(), notFound(teacher.getTeacherId()));
        }
    }

    /**
     * Changes only the status of a teacher (used instead of deleting records).
     *
     * @param teacherId id of the teacher
     * @param status    the new status
     * @throws EntityNotFoundException if the teacher does not exist
     * @throws DatabaseException       if the status cannot be changed
     */
    public void changeStatus(int teacherId, TeacherStatus status) throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE profesores SET estado = ? WHERE id_profesor = ?", statement -> {
                        statement.setString(1, status.getLabel());
                        statement.setInt(2, teacherId);
                    }, "", "Could not change status of teacher id " + teacherId + ".",
                    "No se pudo cambiar el estado del profesor.");
            if (rows == 0) {
                requireExists("profesores", "id_profesor", teacherId, notFound(teacherId));
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo cambiar el estado del profesor.", e);
        }
    }

    /**
     * Finds a teacher by its id.
     *
     * @param teacherId id of the teacher
     * @return the teacher
     * @throws EntityNotFoundException if no teacher has that id
     * @throws DatabaseException       if the query fails
     */
    public Teacher findById(int teacherId) throws DatabaseException, EntityNotFoundException {
        List<Teacher> found = queryList(SELECT_ALL + " WHERE id_profesor = ?", statement -> statement.setInt(1, teacherId),
                this::mapRow, "Could not find teacher id " + teacherId + ".", "No se pudo consultar el profesor.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException(notFound(teacherId));
        }
        return found.get(0);
    }

    public List<Teacher> findAll() throws DatabaseException {
        return queryList(SELECT_ALL + ORDER_BY_NAME, statement -> { }, this::mapRow,
                "Could not list teachers.", "No se pudo consultar la lista de profesores.");
    }

    public List<Teacher> findByStatus(TeacherStatus status) throws DatabaseException {
        return queryList(SELECT_ALL + " WHERE estado = ?" + ORDER_BY_NAME, statement -> statement.setString(1, status.getLabel()),
                this::mapRow, "Could not list teachers by status.", "No se pudo consultar la lista de profesores.");
    }

    /**
     * Searches teachers whose document, email, specialization or full name contains the given text.
     *
     * @param text text to look for
     * @return matching teachers
     * @throws DatabaseException if the query fails
     */
    public List<Teacher> search(String text) throws DatabaseException {
        String pattern = "%" + text.trim() + "%";
        String sql = SELECT_ALL + " WHERE numero_documento LIKE ? OR correo_electronico LIKE ? OR especializacion LIKE ? "
                + "OR CONCAT_WS(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido) LIKE ?"
                + ORDER_BY_NAME;
        return queryList(sql, statement -> {
            for (int i = 1; i <= 4; i++) {
                statement.setString(i, pattern);
            }
        }, this::mapRow, "Could not search teachers with text '" + text + "'.", "No se pudo buscar profesores.");
    }

    public boolean existsByDocumentNumber(String documentNumber, int excludedTeacherId) throws DatabaseException {
        return queryCount("SELECT COUNT(*) FROM profesores WHERE numero_documento = ? AND id_profesor <> ?",
                statement -> {
                    statement.setString(1, documentNumber);
                    statement.setInt(2, excludedTeacherId);
                }, "Could not check teacher document.", "No se pudo verificar el documento.") > 0;
    }

    public boolean existsByEmail(String email, int excludedTeacherId) throws DatabaseException {
        return queryCount("SELECT COUNT(*) FROM profesores WHERE correo_electronico = ? AND id_profesor <> ?",
                statement -> {
                    statement.setString(1, email);
                    statement.setInt(2, excludedTeacherId);
                }, "Could not check teacher email.", "No se pudo verificar el correo.") > 0;
    }

    private static String notFound(int teacherId) {
        return "No se encontró un profesor con el id " + teacherId + ".";
    }

    private void bindTeacher(PreparedStatement statement, Teacher teacher) throws SQLException {
        statement.setString(1, teacher.getDocumentType() == null ? null : teacher.getDocumentType().getLabel());
        statement.setString(2, blankToNull(teacher.getDocumentNumber()));
        statement.setString(3, blankToNull(teacher.getFirstName()));
        statement.setString(4, blankToNull(teacher.getMiddleName()));
        statement.setString(5, blankToNull(teacher.getFirstLastName()));
        statement.setString(6, blankToNull(teacher.getSecondLastName()));
        statement.setString(7, blankToNull(teacher.getEmail()));
        statement.setString(8, blankToNull(teacher.getPhone()));
        statement.setString(9, blankToNull(teacher.getSpecialization()));
        statement.setDate(10, toSqlDate(teacher.getHireDate()));
        statement.setString(11, teacher.getStatus() == null ? null : teacher.getStatus().getLabel());
    }

    Teacher mapRow(ResultSet resultSet) throws SQLException {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(resultSet.getInt("id_profesor"));
        try {
            teacher.setDocumentType(DocumentType.fromLabel(resultSet.getString("tipo_documento")));
            teacher.setStatus(TeacherStatus.fromLabel(resultSet.getString("estado")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table profesores: " + e.getMessage(), e);
        }
        teacher.setDocumentNumber(resultSet.getString("numero_documento"));
        teacher.setFirstName(resultSet.getString("primer_nombre"));
        teacher.setMiddleName(resultSet.getString("segundo_nombre"));
        teacher.setFirstLastName(resultSet.getString("primer_apellido"));
        teacher.setSecondLastName(resultSet.getString("segundo_apellido"));
        teacher.setEmail(resultSet.getString("correo_electronico"));
        teacher.setPhone(resultSet.getString("telefono"));
        teacher.setSpecialization(resultSet.getString("especializacion"));
        teacher.setHireDate(toLocalDate(resultSet.getDate("fecha_contratacion")));
        return teacher;
    }
}