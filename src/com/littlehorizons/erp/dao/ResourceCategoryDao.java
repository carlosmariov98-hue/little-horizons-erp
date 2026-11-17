package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.ResourceCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Data Access Object for the table "categorias_recurso" (read only: the catalog is loaded with the database). */
public class ResourceCategoryDao extends BaseDao {

    private static final String SELECT = "SELECT id_categoria, nombre_categoria, descripcion FROM categorias_recurso";

    public ResourceCategoryDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public List<ResourceCategory> findAll() throws DatabaseException {
        return queryList(SELECT + " ORDER BY nombre_categoria", statement -> { }, this::mapRow,
                "Could not list resource categories.", "No se pudo consultar las categorías de recursos.");
    }

    public ResourceCategory findById(int categoryId) throws DatabaseException, EntityNotFoundException {
        List<ResourceCategory> found = queryList(SELECT + " WHERE id_categoria = ?",
                statement -> statement.setInt(1, categoryId), this::mapRow,
                "Could not find resource category id " + categoryId + ".", "No se pudo consultar la categoría.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException("No se encontró una categoría con el id " + categoryId + ".");
        }
        return found.get(0);
    }

    private ResourceCategory mapRow(ResultSet resultSet) throws SQLException {
        ResourceCategory category = new ResourceCategory();
        category.setCategoryId(resultSet.getInt("id_categoria"));
        category.setName(resultSet.getString("nombre_categoria"));
        category.setDescription(resultSet.getString("descripcion"));
        return category;
    }
}