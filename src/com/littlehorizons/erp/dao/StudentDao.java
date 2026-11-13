package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the table "estudiantes".
 * It is the only class that knows the SQL of students; the rest of the program works with Student objects.
 * All queries use PreparedStatement and all resources are closed with try-with-resources.
 */
public class StudentDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private static final String COLUMNS = "id_estudiante, tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, "
            + "correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado";
    private static final String SELECT_ALL = "SELECT " + COLUMNS + " FROM estudiantes";
    private static final String ORDER_BY_NAME = " ORDER BY primer_apellido, segundo_apellido, primer_nombre";

    private static final String INSERT = "INSERT INTO estudiantes (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, "
            + "direccion, telefono, correo_electronico, tipo_sangre, alergias, condiciones_medicas, "
            + "fecha_ingreso, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, "
            + "fecha_nacimiento = ?, genero = ?, direccion = ?, telefono = ?, correo_electronico = ?, "
            + "tipo_sangre = ?, alergias = ?, condiciones_medicas = ?, fecha_ingreso = ?, estado = ? "
            + "WHERE id_estudiante = ?";

    public StudentDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new student and sets the generated id on the given object.
     *
     * @param student the student to save
     * @return the generated student id
     * @throws BusinessRuleException if another student already has the same document number
     * @throws DatabaseException     if the student cannot be saved
     */
    public int insert(Student student) throws DatabaseException, BusinessRuleException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindStudent(statement, student);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    student.setStudentId(keys.getInt(1));
                }
            }
            return student.getStudentId();
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new BusinessRuleException("Ya existe un estudiante con el documento "
                        + student.getDocumentNumber() + ".", e);
            }
            throw databaseError("Could not insert student " + student.getDocumentNumber() + ".",
                    "No se pudo guardar el estudiante.", e);
        }
    }

    /**
     * Updates all the data of an existing student.
     *
     * @param student the student with the new data; its id identifies the record
     * @throws EntityNotFoundException if the student does not exist
     * @throws BusinessRuleException   if the new document number belongs to another student
     * @throws DatabaseException       if the student cannot be updated
     */
    public void update(Student student) throws DatabaseException, EntityNotFoundException, BusinessRuleException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bindStudent(statement, student);
            statement.setInt(17, student.getStudentId());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                requireExists(connection, student.getStudentId());
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new BusinessRuleException("Ya existe otro estudiante con el documento "
                        + student.getDocumentNumber() + ".", e);
            }
            throw databaseError("Could not update student id " + student.getStudentId() + ".",
                    "No se pudo actualizar el estudiante.", e);
        }
    }

    /**
     * Changes only the enrollment status of a student (used instead of deleting records).
     *
     * @param studentId id of the student
     * @param status    the new status
     * @throws EntityNotFoundException if the student does not exist
     * @throws DatabaseException       if the status cannot be changed
     */
    public void changeStatus(int studentId, StudentStatus status) throws DatabaseException, EntityNotFoundException {
        String sql = "UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.getLabel());
            statement.setInt(2, studentId);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                requireExists(connection, studentId);
            }
        } catch (SQLException e) {
            throw databaseError("Could not change status of student id " + studentId + ".",
                    "No se pudo cambiar el estado del estudiante.", e);
        }
    }

    /**
     * Finds a student by its id.
     *
     * @param studentId id of the student
     * @return the student
     * @throws EntityNotFoundException if no student has that id
     * @throws DatabaseException       if the query fails
     */
    public Student findById(int studentId) throws DatabaseException, EntityNotFoundException {
        String sql = SELECT_ALL + " WHERE id_estudiante = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw databaseError("Could not find student id " + studentId + ".",
                    "No se pudo consultar el estudiante.", e);
        }
        throw new EntityNotFoundException("No se encontró un estudiante con el id " + studentId + ".");
    }

    /**
     * Lists all students ordered by last name.
     *
     * @return all students (empty list when there are none)
     * @throws DatabaseException if the query fails
     */
    public List<Student> findAll() throws DatabaseException {
        return query(SELECT_ALL + ORDER_BY_NAME, null);
    }

    /**
     * Lists the students with a given status, ordered by last name.
     *
     * @param status the status to filter by
     * @return matching students (empty list when there are none)
     * @throws DatabaseException if the query fails
     */
    public List<Student> findByStatus(StudentStatus status) throws DatabaseException {
        return query(SELECT_ALL + " WHERE estado = ?" + ORDER_BY_NAME, status.getLabel());
    }

    /**
     * Searches students whose document number or full name contains the given text.
     *
     * @param text text to look for
     * @return matching students (empty list when there are none)
     * @throws DatabaseException if the query fails
     */
    public List<Student> search(String text) throws DatabaseException {
        String sql = SELECT_ALL + " WHERE numero_documento LIKE ? OR CONCAT_WS(' ', primer_nombre, "
                + "segundo_nombre, primer_apellido, segundo_apellido) LIKE ?" + ORDER_BY_NAME;
        String pattern = "%" + text.trim() + "%";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readStudents(resultSet);
            }
        } catch (SQLException e) {
            throw databaseError("Could not search students with text '" + text + "'.",
                    "No se pudo buscar estudiantes.", e);
        }
    }

    /**
     * Checks whether a document number is already used by a student.
     *
     * @param documentNumber the document number to look for
     * @return true if a student already has that document number
     * @throws DatabaseException if the query fails
     */
    public boolean existsByDocumentNumber(String documentNumber) throws DatabaseException {
        return existsByDocumentNumber(documentNumber, 0);
    }

    /**
     * Checks whether a document number is used by a student other than the given one.
     * Useful when editing a student, so its own document is not reported as a duplicate.
     *
     * @param documentNumber  the document number to look for
     * @param excludedStudentId id of the student to ignore (0 to ignore nobody)
     * @return true if another student has that document number
     * @throws DatabaseException if the query fails
     */
    public boolean existsByDocumentNumber(String documentNumber, int excludedStudentId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM estudiantes WHERE numero_documento = ? AND id_estudiante <> ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentNumber);
            statement.setInt(2, excludedStudentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw databaseError("Could not check document number " + documentNumber + ".",
                    "No se pudo verificar el documento.", e);
        }
    }

    // runs a list query with an optional single text parameter
    private List<Student> query(String sql, String parameter) throws DatabaseException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return readStudents(resultSet);
            }
        } catch (SQLException e) {
            throw databaseError("Could not list students.", "No se pudo consultar la lista de estudiantes.", e);
        }
    }

    private List<Student> readStudents(ResultSet resultSet) throws SQLException {
        List<Student> students = new ArrayList<>();
        while (resultSet.next()) {
            students.add(mapRow(resultSet));
        }
        return students;
    }

    // used when an UPDATE changed 0 rows: either the data was identical or the student does not exist
    private void requireExists(Connection connection, int studentId) throws SQLException, EntityNotFoundException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM estudiantes WHERE id_estudiante = ?")) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new EntityNotFoundException("No se encontró un estudiante con el id " + studentId + ".");
                }
            }
        }
    }

    private void bindStudent(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getDocumentType() == null ? null : student.getDocumentType().getLabel());
        statement.setString(2, blankToNull(student.getDocumentNumber()));
        statement.setString(3, blankToNull(student.getFirstName()));
        statement.setString(4, blankToNull(student.getMiddleName()));
        statement.setString(5, blankToNull(student.getFirstLastName()));
        statement.setString(6, blankToNull(student.getSecondLastName()));
        statement.setDate(7, toSqlDate(student.getBirthDate()));
        statement.setString(8, student.getGender() == null ? null : student.getGender().getLabel());
        statement.setString(9, blankToNull(student.getAddress()));
        statement.setString(10, blankToNull(student.getPhone()));
        statement.setString(11, blankToNull(student.getEmail()));
        statement.setString(12, blankToNull(student.getBloodType()));
        statement.setString(13, blankToNull(student.getAllergies()));
        statement.setString(14, blankToNull(student.getMedicalConditions()));
        statement.setDate(15, toSqlDate(student.getAdmissionDate()));
        statement.setString(16, student.getStatus() == null ? null : student.getStatus().getLabel());
    }

    private Student mapRow(ResultSet resultSet) throws SQLException {
        Student student = new Student();
        student.setStudentId(resultSet.getInt("id_estudiante"));
        try {
            student.setDocumentType(DocumentType.fromLabel(resultSet.getString("tipo_documento")));
            student.setGender(Gender.fromLabel(resultSet.getString("genero")));
            student.setStatus(StudentStatus.fromLabel(resultSet.getString("estado")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table estudiantes: " + e.getMessage(), e);
        }
        student.setDocumentNumber(resultSet.getString("numero_documento"));
        student.setFirstName(resultSet.getString("primer_nombre"));
        student.setMiddleName(resultSet.getString("segundo_nombre"));
        student.setFirstLastName(resultSet.getString("primer_apellido"));
        student.setSecondLastName(resultSet.getString("segundo_apellido"));
        student.setBirthDate(toLocalDate(resultSet.getDate("fecha_nacimiento")));
        student.setAddress(resultSet.getString("direccion"));
        student.setPhone(resultSet.getString("telefono"));
        student.setEmail(resultSet.getString("correo_electronico"));
        student.setBloodType(resultSet.getString("tipo_sangre"));
        student.setAllergies(resultSet.getString("alergias"));
        student.setMedicalConditions(resultSet.getString("condiciones_medicas"));
        student.setAdmissionDate(toLocalDate(resultSet.getDate("fecha_ingreso")));
        return student;
    }
}
