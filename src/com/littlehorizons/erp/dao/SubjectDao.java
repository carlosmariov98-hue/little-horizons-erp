package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Subject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for the table "materias" (read only: the catalog is loaded with the database).
 */
public class SubjectDao extends BaseDao {

    private static final String SELECT_ALL = "SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, "
            + "m.id_grado, g.nombre_grado, m.horas_semanales, m.esta_activa FROM materias m "
            + "JOIN grados g ON g.id_grado = m.id_grado";

    public SubjectDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public List<Subject> findByGrade(int gradeId) throws DatabaseException {
        return queryList(SELECT_ALL + " WHERE m.id_grado = ? ORDER BY m.nombre_materia",
                statement -> statement.setInt(1, gradeId), this::mapRow,
                "Could not list subjects of grade id " + gradeId + ".", "No se pudo consultar las materias.");
    }

    public Subject findById(int subjectId) throws DatabaseException, EntityNotFoundException {
        List<Subject> found = queryList(SELECT_ALL + " WHERE m.id_materia = ?", statement -> statement.setInt(1, subjectId),
                this::mapRow, "Could not find subject id " + subjectId + ".", "No se pudo consultar la materia.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException("No se encontró una materia con el id " + subjectId + ".");
        }
        return found.get(0);
    }

    private Subject mapRow(ResultSet resultSet) throws SQLException {
        Subject subject = new Subject();
        subject.setSubjectId(resultSet.getInt("id_materia"));
        subject.setCode(resultSet.getString("codigo_materia"));
        subject.setName(resultSet.getString("nombre_materia"));
        subject.setDescription(resultSet.getString("descripcion"));
        subject.setGradeId(resultSet.getInt("id_grado"));
        subject.setGradeName(resultSet.getString("nombre_grado"));
        subject.setWeeklyHours(resultSet.getInt("horas_semanales"));
        subject.setActive(resultSet.getBoolean("esta_activa"));
        return subject;
    }
}