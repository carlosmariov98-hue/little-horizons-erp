package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Grade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for the table "grados" (read only: the catalog is loaded with the database).
 */
public class GradeDao extends BaseDao {

    private static final String SELECT_ALL = "SELECT id_grado, nombre_grado, nivel_numerico FROM grados";

    public GradeDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public List<Grade> findAll() throws DatabaseException {
        return queryList(SELECT_ALL + " ORDER BY nivel_numerico", statement -> { }, this::mapRow,
                "Could not list grades.", "No se pudo consultar la lista de grados.");
    }

    public Grade findById(int gradeId) throws DatabaseException, EntityNotFoundException {
        List<Grade> found = queryList(SELECT_ALL + " WHERE id_grado = ?", statement -> statement.setInt(1, gradeId),
                this::mapRow, "Could not find grade id " + gradeId + ".", "No se pudo consultar el grado.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException("No se encontró un grado con el id " + gradeId + ".");
        }
        return found.get(0);
    }

    private Grade mapRow(ResultSet resultSet) throws SQLException {
        Grade grade = new Grade();
        grade.setGradeId(resultSet.getInt("id_grado"));
        grade.setName(resultSet.getString("nombre_grado"));
        grade.setLevel(resultSet.getInt("nivel_numerico"));
        return grade;
    }
}
