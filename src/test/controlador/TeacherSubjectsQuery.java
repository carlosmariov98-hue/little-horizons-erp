package test.controlador;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.util.AppLogger;
import test.modelo.AssignedSubject;
import test.modelo.TeacherInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Database queries of the exporter. It joins the tables profesores, asignaciones_profesor, materias and grados.
 * All the queries use PreparedStatement and close their resources with try-with-resources.
 */
public class TeacherSubjectsQuery {

    private static final String FIND_TEACHER = "SELECT id_profesor, tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido "
            + "FROM profesores WHERE id_profesor = ?";

    // GROUP BY: a teacher that has the same subject in two sections shows the subject only once;
    // the order is the order in which the subjects were assigned
    private static final String FIND_SUBJECTS = "SELECT m.codigo_materia, m.nombre_materia, g.nombre_grado "
            + "FROM asignaciones_profesor a "
            + "JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN grados g ON g.id_grado = m.id_grado "
            + "WHERE a.id_profesor = ? AND a.anio_academico = ? "
            + "GROUP BY m.id_materia, m.codigo_materia, m.nombre_materia, g.nombre_grado "
            + "ORDER BY MIN(a.id_asignacion)";

    private final ConnectionProvider connectionProvider;

    public TeacherSubjectsQuery(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Looks for a teacher by id.
     *
     * @param teacherId id of the teacher
     * @return the teacher, or empty if there is no teacher with that id
     * @throws DatabaseException if the query fails
     */
    public Optional<TeacherInfo> findTeacher(int teacherId) throws DatabaseException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TEACHER)) {
            statement.setInt(1, teacherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new TeacherInfo(resultSet.getInt("id_profesor"),
                        resultSet.getString("tipo_documento"), resultSet.getString("numero_documento"),
                        resultSet.getString("primer_nombre"), resultSet.getString("segundo_nombre"),
                        resultSet.getString("primer_apellido"), resultSet.getString("segundo_apellido")));
            }
        } catch (SQLException e) {
            AppLogger.getInstance().error("Could not find teacher id " + teacherId + ".", e);
            throw new DatabaseException("No se pudo consultar el profesor en la base de datos.", e);
        }
    }

    /**
     * Lists the subjects assigned to a teacher in an academic year, in the order they were assigned.
     *
     * @param teacherId id of the teacher
     * @param year      academic year to filter by
     * @return the subjects; the list is empty if the teacher has no assignments in that year
     * @throws DatabaseException if the query fails
     */
    public List<AssignedSubject> findAssignedSubjects(int teacherId, int year) throws DatabaseException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_SUBJECTS)) {
            statement.setInt(1, teacherId);
            statement.setInt(2, year);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AssignedSubject> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(new AssignedSubject(resultSet.getString("codigo_materia"),
                            resultSet.getString("nombre_materia"), resultSet.getString("nombre_grado")));
                }
                return subjects;
            }
        } catch (SQLException e) {
            AppLogger.getInstance().error("Could not list subjects of teacher id " + teacherId + ".", e);
            throw new DatabaseException("No se pudieron consultar las materias asignadas del profesor.", e);
        }
    }
}