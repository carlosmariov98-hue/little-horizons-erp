package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.enums.DocumentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object for the table "acudientes".
 */
public class GuardianDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private static final String[] COLUMN_LIST = {"id_acudiente", "tipo_documento", "numero_documento",
            "primer_nombre", "segundo_nombre", "primer_apellido", "segundo_apellido", "parentesco",
            "telefono_principal", "telefono_secundario", "correo_electronico", "direccion", "ocupacion",
            "es_responsable_financiero", "esta_activo"};
    private static final String SELECT_ALL = "SELECT " + String.join(", ", COLUMN_LIST) + " FROM acudientes";
    private static final String ORDER_BY_NAME = " ORDER BY primer_apellido, segundo_apellido, primer_nombre";

    private static final String INSERT = "INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, "
            + "telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero, "
            + "esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, parentesco = ?, "
            + "telefono_principal = ?, telefono_secundario = ?, correo_electronico = ?, direccion = ?, "
            + "ocupacion = ?, es_responsable_financiero = ?, esta_activo = ? WHERE id_acudiente = ?";

    public GuardianDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    static String columnsWithAlias(String alias) {
        return Arrays.stream(COLUMN_LIST).map(column -> alias + "." + column).collect(Collectors.joining(", "));
    }

    /**
     * Saves a new guardian and sets the generated id on the given object.
     *
     * @param guardian the guardian to save
     * @return the generated guardian id
     * @throws BusinessRuleException if another guardian already has the same document number
     * @throws DatabaseException     if the guardian cannot be saved
     */
    public int insert(Guardian guardian) throws DatabaseException, BusinessRuleException {
        try (Connection connection = getConnection()) {
            return insert(connection, guardian);
        } catch (SQLException e) {
            throw databaseError("Could not insert guardian " + guardian.getDocumentNumber() + ".",
                    "No se pudo guardar el acudiente.", e);
        }
    }

    /**
     * Saves a new guardian using an existing connection, so it can take part in a larger transaction.
     */
    int insert(Connection connection, Guardian guardian) throws SQLException, BusinessRuleException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindGuardian(statement, guardian);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    guardian.setGuardianId(keys.getInt(1));
                }
            }
            return guardian.getGuardianId();
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new BusinessRuleException("Ya existe un acudiente con el documento "
                        + guardian.getDocumentNumber() + ".", e);
            }
            throw e;
        }
    }

    /**
     * Updates all the data of an existing guardian.
     *
     * @param guardian the guardian with the new data; its id identifies the record
     * @throws EntityNotFoundException if the guardian does not exist
     * @throws BusinessRuleException   if the new document number belongs to another guardian
     * @throws DatabaseException       if the guardian cannot be updated
     */
    public void update(Guardian guardian) throws DatabaseException, EntityNotFoundException, BusinessRuleException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bindGuardian(statement, guardian);
            statement.setInt(15, guardian.getGuardianId());
            if (statement.executeUpdate() == 0 && !exists(connection, guardian.getGuardianId())) {
                throw new EntityNotFoundException("No se encontró un acudiente con el id "
                        + guardian.getGuardianId() + ".");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new BusinessRuleException("Ya existe otro acudiente con el documento "
                        + guardian.getDocumentNumber() + ".", e);
            }
            throw databaseError("Could not update guardian id " + guardian.getGuardianId() + ".",
                    "No se pudo actualizar el acudiente.", e);
        }
    }

    /**
     * Finds a guardian by its id.
     *
     * @param guardianId id of the guardian
     * @return the guardian
     * @throws EntityNotFoundException if no guardian has that id
     * @throws DatabaseException       if the query fails
     */
    public Guardian findById(int guardianId) throws DatabaseException, EntityNotFoundException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL + " WHERE id_acudiente = ?")) {
            statement.setInt(1, guardianId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw databaseError("Could not find guardian id " + guardianId + ".",
                    "No se pudo consultar el acudiente.", e);
        }
        throw new EntityNotFoundException("No se encontró un acudiente con el id " + guardianId + ".");
    }

    /**
     * Finds a guardian by document number. A missing guardian is not an error, so an Optional is returned.
     *
     * @param documentNumber the document number to look for
     * @return the guardian, or an empty Optional when there is none
     * @throws DatabaseException if the query fails
     */
    public Optional<Guardian> findByDocumentNumber(String documentNumber) throws DatabaseException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL + " WHERE numero_documento = ?")) {
            statement.setString(1, documentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError("Could not find guardian by document " + documentNumber + ".",
                    "No se pudo consultar el acudiente.", e);
        }
    }

    /**
     * Searches guardians whose document number or full name contains the given text.
     *
     * @param text text to look for
     * @return matching guardians (empty list when there are none)
     * @throws DatabaseException if the query fails
     */
    public List<Guardian> search(String text) throws DatabaseException {
        String sql = SELECT_ALL + " WHERE numero_documento LIKE ? OR CONCAT_WS(' ', primer_nombre, segundo_nombre, "
                + "primer_apellido, segundo_apellido) LIKE ?" + ORDER_BY_NAME;
        String pattern = "%" + text.trim() + "%";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Guardian> guardians = new ArrayList<>();
                while (resultSet.next()) {
                    guardians.add(mapRow(resultSet));
                }
                return guardians;
            }
        } catch (SQLException e) {
            throw databaseError("Could not search guardians with text '" + text + "'.",
                    "No se pudo buscar acudientes.", e);
        }
    }

    /**
     * Checks whether a guardian exists, using an existing connection.
     */
    boolean exists(Connection connection, int guardianId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM acudientes WHERE id_acudiente = ?")) {
            statement.setInt(1, guardianId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void bindGuardian(PreparedStatement statement, Guardian guardian) throws SQLException {
        statement.setString(1, guardian.getDocumentType() == null ? null : guardian.getDocumentType().getLabel());
        statement.setString(2, blankToNull(guardian.getDocumentNumber()));
        statement.setString(3, blankToNull(guardian.getFirstName()));
        statement.setString(4, blankToNull(guardian.getMiddleName()));
        statement.setString(5, blankToNull(guardian.getFirstLastName()));
        statement.setString(6, blankToNull(guardian.getSecondLastName()));
        statement.setString(7, blankToNull(guardian.getRelationship()));
        statement.setString(8, blankToNull(guardian.getPrimaryPhone()));
        statement.setString(9, blankToNull(guardian.getSecondaryPhone()));
        statement.setString(10, blankToNull(guardian.getEmail()));
        statement.setString(11, blankToNull(guardian.getAddress()));
        statement.setString(12, blankToNull(guardian.getOccupation()));
        statement.setBoolean(13, guardian.isFinanciallyResponsible());
        statement.setBoolean(14, guardian.isActive());
    }

    Guardian mapRow(ResultSet resultSet) throws SQLException {
        Guardian guardian = new Guardian();
        guardian.setGuardianId(resultSet.getInt("id_acudiente"));
        try {
            guardian.setDocumentType(DocumentType.fromLabel(resultSet.getString("tipo_documento")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table acudientes: " + e.getMessage(), e);
        }
        guardian.setDocumentNumber(resultSet.getString("numero_documento"));
        guardian.setFirstName(resultSet.getString("primer_nombre"));
        guardian.setMiddleName(resultSet.getString("segundo_nombre"));
        guardian.setFirstLastName(resultSet.getString("primer_apellido"));
        guardian.setSecondLastName(resultSet.getString("segundo_apellido"));
        guardian.setRelationship(resultSet.getString("parentesco"));
        guardian.setPrimaryPhone(resultSet.getString("telefono_principal"));
        guardian.setSecondaryPhone(resultSet.getString("telefono_secundario"));
        guardian.setEmail(resultSet.getString("correo_electronico"));
        guardian.setAddress(resultSet.getString("direccion"));
        guardian.setOccupation(resultSet.getString("ocupacion"));
        guardian.setFinanciallyResponsible(resultSet.getBoolean("es_responsable_financiero"));
        guardian.setActive(resultSet.getBoolean("esta_activo"));
        return guardian;
    }
}
