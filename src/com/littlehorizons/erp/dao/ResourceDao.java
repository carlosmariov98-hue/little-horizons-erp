package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.enums.ResourceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Data Access Object for the table "recursos". The status "Prestado" is managed only by the loans
 * (see {@link LoanDao}); this class refuses to change the status of a resource that is on loan.
 */
public class ResourceDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_FOREIGN_KEY_FAILS = 1452;
    private static final String SELECT = "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.id_categoria, "
            + "c.nombre_categoria, r.descripcion, r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso "
            + "FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria";
    private static final String ORDER = " ORDER BY r.codigo_recurso";

    public ResourceDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new resource (always as available) and sets the generated id on the given object.
     *
     * @return the generated id
     * @throws BusinessRuleException   if the inventory code is already used
     * @throws EntityNotFoundException if the category does not exist
     * @throws DatabaseException       if the resource cannot be saved
     */
    public int insert(Resource resource) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        String sql = "INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
                + "fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, 'Disponible')";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, resource.getCode());
            statement.setString(2, resource.getName());
            statement.setInt(3, resource.getCategoryId());
            statement.setString(4, blankToNull(resource.getDescription()));
            statement.setDate(5, toSqlDate(resource.getAcquisitionDate()));
            statement.setString(6, blankToNull(resource.getLocation()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    resource.setResourceId(keys.getInt(1));
                }
            }
            return resource.getResourceId();
        } catch (SQLException e) {
            throw resourceError(e, resource);
        }
    }

    /**
     * Updates the data of a resource. The inventory code and the status are not changed here.
     *
     * @throws EntityNotFoundException if the resource or the category does not exist
     * @throws DatabaseException       if the resource cannot be updated
     */
    public void update(Resource resource) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        String sql = "UPDATE recursos SET nombre_recurso = ?, id_categoria = ?, descripcion = ?, "
                + "fecha_adquisicion = ?, ubicacion_fisica = ? WHERE id_recurso = ?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resource.getName());
            statement.setInt(2, resource.getCategoryId());
            statement.setString(3, blankToNull(resource.getDescription()));
            statement.setDate(4, toSqlDate(resource.getAcquisitionDate()));
            statement.setString(5, blankToNull(resource.getLocation()));
            statement.setInt(6, resource.getResourceId());
            if (statement.executeUpdate() == 0) {
                requireExists("recursos", "id_recurso", resource.getResourceId(), notFound(resource.getResourceId()));
            }
        } catch (SQLException e) {
            throw resourceError(e, resource);
        }
    }

    // explains an insert/update SQL error to the user; the return type only helps the caller to write "throw"
    private DatabaseException resourceError(SQLException e, Resource resource)
            throws BusinessRuleException, EntityNotFoundException {
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            throw new BusinessRuleException("Ya existe un recurso con el código " + resource.getCode() + ".", e);
        }
        if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAILS) {
            throw new EntityNotFoundException("La categoría indicada no existe.", e);
        }
        return databaseError("Could not save resource " + resource.getCode() + ".",
                "No se pudo guardar el recurso.", e);
    }

    /**
     * Changes the status of a resource that is not on loan. The check is part of the UPDATE, so a resource
     * that was lent a moment ago can never be changed by mistake.
     *
     * @throws BusinessRuleException   if the resource is on loan
     * @throws EntityNotFoundException if the resource does not exist
     * @throws DatabaseException       if the status cannot be changed
     */
    public void changeStatus(int resourceId, ResourceStatus status)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        int rows = executeUpdate("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ? "
                        + "AND estado_recurso <> 'Prestado'", statement -> {
                    statement.setString(1, status.getLabel());
                    statement.setInt(2, resourceId);
                }, "", "Could not change the status of resource id " + resourceId + ".",
                "No se pudo cambiar el estado del recurso.");
        if (rows == 0) {
            requireExists("recursos", "id_recurso", resourceId, notFound(resourceId));
            throw new BusinessRuleException("El recurso está prestado: registre primero su devolución.");
        }
    }

    public Resource findById(int resourceId) throws DatabaseException, EntityNotFoundException {
        List<Resource> found = queryList(SELECT + " WHERE r.id_recurso = ?",
                statement -> statement.setInt(1, resourceId), this::mapRow,
                "Could not find resource id " + resourceId + ".", "No se pudo consultar el recurso.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException(notFound(resourceId));
        }
        return found.get(0);
    }

    public List<Resource> findAll() throws DatabaseException {
        return queryList(SELECT + ORDER, statement -> { }, this::mapRow, "Could not list resources.",
                "No se pudo consultar los recursos.");
    }

    public List<Resource> findByStatus(ResourceStatus status) throws DatabaseException {
        return queryList(SELECT + " WHERE r.estado_recurso = ?" + ORDER,
                statement -> statement.setString(1, status.getLabel()), this::mapRow,
                "Could not list resources with status " + status + ".", "No se pudo consultar los recursos.");
    }

    /** Resources whose code or name contains the text. */
    public List<Resource> search(String text) throws DatabaseException {
        String pattern = "%" + text + "%";
        return queryList(SELECT + " WHERE r.codigo_recurso LIKE ? OR r.nombre_recurso LIKE ?" + ORDER, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
        }, this::mapRow, "Could not search resources.", "No se pudo buscar los recursos.");
    }

    private static String notFound(int resourceId) {
        return "No se encontró un recurso con el id " + resourceId + ".";
    }

    private Resource mapRow(ResultSet resultSet) throws SQLException {
        Resource resource = new Resource();
        resource.setResourceId(resultSet.getInt("id_recurso"));
        resource.setCode(resultSet.getString("codigo_recurso"));
        resource.setName(resultSet.getString("nombre_recurso"));
        resource.setCategoryId(resultSet.getInt("id_categoria"));
        resource.setCategoryName(resultSet.getString("nombre_categoria"));
        resource.setDescription(resultSet.getString("descripcion"));
        resource.setAcquisitionDate(toLocalDate(resultSet.getDate("fecha_adquisicion")));
        resource.setLocation(resultSet.getString("ubicacion_fisica"));
        try {
            resource.setStatus(ResourceStatus.fromLabel(resultSet.getString("estado_recurso")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table recursos: " + e.getMessage(), e);
        }
        return resource;
    }
}