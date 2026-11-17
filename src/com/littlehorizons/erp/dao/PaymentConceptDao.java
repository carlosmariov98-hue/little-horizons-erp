package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.PaymentConcept;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the table "conceptos_pago" (catalog of what the school charges for).
 */
public class PaymentConceptDao extends BaseDao {

    private static final String SELECT = "SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, "
            + "monto_base, es_recurrente, esta_activo FROM conceptos_pago";

    public PaymentConceptDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new concept and sets the generated id on the given object.
     *
     * @param concept the concept to save
     * @return the generated id
     * @throws BusinessRuleException if the code is already used
     * @throws DatabaseException     if the concept cannot be saved
     */
    public int insert(PaymentConcept concept) throws DatabaseException, BusinessRuleException {
        try (java.sql.Connection connection = getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement("INSERT INTO conceptos_pago "
                     + "(codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo) "
                     + "VALUES (?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, concept.getCode());
            statement.setString(2, concept.getName());
            statement.setString(3, blankToNull(concept.getDescription()));
            statement.setBigDecimal(4, concept.getBaseAmount());
            statement.setBoolean(5, concept.isRecurring());
            statement.setBoolean(6, concept.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    concept.setConceptId(keys.getInt(1));
                }
            }
            return concept.getConceptId();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new BusinessRuleException("Ya existe un concepto de pago con el código " + concept.getCode() + ".", e);
            }
            throw databaseError("Could not insert payment concept " + concept.getCode() + ".",
                    "No se pudo guardar el concepto de pago.", e);
        }
    }

    /**
     * Changes the base amount of a concept.
     *
     * @throws EntityNotFoundException if the concept does not exist
     * @throws DatabaseException       if the amount cannot be changed
     */
    public void updateBaseAmount(int conceptId, BigDecimal baseAmount) throws DatabaseException, EntityNotFoundException {
        updateColumn("monto_base = ?", conceptId, statement -> statement.setBigDecimal(1, baseAmount));
    }

    /**
     * Activates or deactivates a concept (concepts are never deleted because payments refer to them).
     *
     * @throws EntityNotFoundException if the concept does not exist
     * @throws DatabaseException       if the status cannot be changed
     */
    public void changeActive(int conceptId, boolean active) throws DatabaseException, EntityNotFoundException {
        updateColumn("esta_activo = ?", conceptId, statement -> statement.setBoolean(1, active));
    }

    // the assignment text is always a constant written in this class, never user input
    private void updateColumn(String assignment, int conceptId, Binder valueBinder)
            throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE conceptos_pago SET " + assignment + " WHERE id_concepto = ?", statement -> {
                valueBinder.bind(statement);
                statement.setInt(2, conceptId);
            }, "", "Could not update payment concept id " + conceptId + ".", "No se pudo actualizar el concepto de pago.");
            if (rows == 0) {
                requireExists("conceptos_pago", "id_concepto", conceptId, notFound(conceptId));
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo actualizar el concepto de pago.", e);
        }
    }

    public PaymentConcept findById(int conceptId) throws DatabaseException, EntityNotFoundException {
        List<PaymentConcept> found = queryList(SELECT + " WHERE id_concepto = ?", statement -> statement.setInt(1, conceptId),
                this::mapRow, "Could not find payment concept id " + conceptId + ".", "No se pudo consultar el concepto.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException(notFound(conceptId));
        }
        return found.get(0);
    }

    public Optional<PaymentConcept> findByCode(String code) throws DatabaseException {
        return queryList(SELECT + " WHERE codigo_concepto = ?", statement -> statement.setString(1, code), this::mapRow,
                "Could not find payment concept " + code + ".", "No se pudo consultar el concepto.").stream().findFirst();
    }

    public List<PaymentConcept> findAll() throws DatabaseException {
        return queryList(SELECT + " ORDER BY nombre_concepto", statement -> { }, this::mapRow,
                "Could not list payment concepts.", "No se pudo consultar los conceptos de pago.");
    }

    public List<PaymentConcept> findActive() throws DatabaseException {
        return queryList(SELECT + " WHERE esta_activo = 1 ORDER BY nombre_concepto", statement -> { }, this::mapRow,
                "Could not list active payment concepts.", "No se pudo consultar los conceptos de pago.");
    }

    private static String notFound(int conceptId) {
        return "No se encontró un concepto de pago con el id " + conceptId + ".";
    }

    private PaymentConcept mapRow(ResultSet resultSet) throws SQLException {
        PaymentConcept concept = new PaymentConcept();
        concept.setConceptId(resultSet.getInt("id_concepto"));
        concept.setCode(resultSet.getString("codigo_concepto"));
        concept.setName(resultSet.getString("nombre_concepto"));
        concept.setDescription(resultSet.getString("descripcion"));
        concept.setBaseAmount(resultSet.getBigDecimal("monto_base"));
        concept.setRecurring(resultSet.getBoolean("es_recurrente"));
        concept.setActive(resultSet.getBoolean("esta_activo"));
        return concept;
    }
}