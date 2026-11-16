package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.TeachingAssignment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for the table "asignaciones_profesor" (subjects taught by each teacher).
 */
public class AssignmentDao extends BaseDao {

    private static final String SELECT = "SELECT a.id_asignacion, a.id_profesor, a.id_materia, a.anio_academico, "
            + "a.seccion, CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) "
            + "AS nombre_profesor, m.nombre_materia, g.nombre_grado FROM asignaciones_profesor a "
            + "JOIN profesores p ON p.id_profesor = a.id_profesor JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN grados g ON g.id_grado = m.id_grado";

    public AssignmentDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Assigns a subject to a teacher for a year and section.
     *
     * @param assignment the assignment to save
     * @throws BusinessRuleException   if the teacher already has that subject in that year and section
     * @throws EntityNotFoundException if the teacher or the subject does not exist
     * @throws DatabaseException       if the assignment cannot be saved
     */
    public void insert(TeachingAssignment assignment)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        executeUpdate("INSERT INTO asignaciones_profesor (id_profesor, id_materia, anio_academico, seccion) "
                        + "VALUES (?, ?, ?, ?)", statement -> {
                    statement.setInt(1, assignment.getTeacherId());
                    statement.setInt(2, assignment.getSubjectId());
                    statement.setInt(3, assignment.getYear());
                    statement.setString(4, assignment.getSection());
                }, "El profesor ya tiene asignada esa materia en ese año y sección.",
                "Could not assign subject " + assignment.getSubjectId() + " to teacher " + assignment.getTeacherId() + ".",
                "No se pudo asignar la materia al profesor.");
    }

    public List<TeachingAssignment> findByTeacher(int teacherId, int year) throws DatabaseException {
        return queryList(SELECT + " WHERE a.id_profesor = ? AND a.anio_academico = ? "
                        + "ORDER BY g.nivel_numerico, m.nombre_materia, a.seccion", statement -> {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, year);
                }, this::mapRow, "Could not list assignments of teacher id " + teacherId + ".",
                "No se pudo consultar la carga académica del profesor.");
    }

    public List<TeachingAssignment> findBySubject(int subjectId, int year) throws DatabaseException {
        return queryList(SELECT + " WHERE a.id_materia = ? AND a.anio_academico = ? ORDER BY a.seccion, nombre_profesor",
                statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, year);
                }, this::mapRow, "Could not list teachers of subject id " + subjectId + ".",
                "No se pudo consultar los profesores de la materia.");
    }

    private TeachingAssignment mapRow(ResultSet resultSet) throws SQLException {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setAssignmentId(resultSet.getInt("id_asignacion"));
        assignment.setTeacherId(resultSet.getInt("id_profesor"));
        assignment.setSubjectId(resultSet.getInt("id_materia"));
        assignment.setYear(resultSet.getInt("anio_academico"));
        assignment.setSection(resultSet.getString("seccion"));
        assignment.setTeacherName(resultSet.getString("nombre_profesor"));
        assignment.setSubjectName(resultSet.getString("nombre_materia"));
        assignment.setGradeName(resultSet.getString("nombre_grado"));
        return assignment;
    }
}