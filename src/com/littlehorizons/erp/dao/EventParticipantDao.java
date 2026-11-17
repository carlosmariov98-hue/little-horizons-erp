package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.enums.EventPosition;
import com.littlehorizons.erp.model.enums.ParticipationRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Data Access Object for the table "participantes_evento". */
public class EventParticipantDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_FOREIGN_KEY_FAILS = 1452;
    private static final String SELECT = "SELECT pe.id_participacion, pe.id_evento, v.nombre_evento, pe.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS estudiante, "
            + "pe.rol_participacion, pe.posicion_obtenida, pe.observaciones FROM participantes_evento pe "
            + "JOIN eventos v ON v.id_evento = pe.id_evento JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante";

    public EventParticipantDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Registers a student in an event.
     *
     * @return the id of the participation
     * @throws BusinessRuleException   if the student is already registered in the event
     * @throws EntityNotFoundException if the event or the student does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public int insert(EventParticipant participant)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        String sql = "INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, observaciones) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, participant.getEventId());
            statement.setInt(2, participant.getStudentId());
            statement.setString(3, participant.getRole().getLabel());
            statement.setString(4, blankToNull(participant.getNotes()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    participant.setParticipationId(keys.getInt(1));
                }
            }
            return participant.getParticipationId();
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                throw new BusinessRuleException("El estudiante ya está registrado en este evento.", e);
            }
            if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAILS) {
                throw new EntityNotFoundException("El evento o el estudiante indicado no existe.", e);
            }
            throw databaseError("Could not register student id " + participant.getStudentId() + " in event id "
                    + participant.getEventId() + ".", "No se pudo registrar la participación.", e);
        }
    }

    public void delete(int participationId) throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("DELETE FROM participantes_evento WHERE id_participacion = ?",
                    statement -> statement.setInt(1, participationId), "",
                    "Could not delete participation id " + participationId + ".",
                    "No se pudo retirar la participación.");
            if (rows == 0) {
                throw new EntityNotFoundException(notFound(participationId));
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo retirar la participación.", e);
        }
    }

    /**
     * Gives a position to a competitor. The check that no other competitor of the event has the same podium
     * place and the update are done in one transaction.
     *
     * @throws BusinessRuleException   if another competitor already has that podium place
     * @throws EntityNotFoundException if the participation does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public void assignPosition(int participationId, EventPosition position)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        inTransaction("Could not assign a position to participation id " + participationId + ".",
                "No se pudo guardar la posición. No se guardó ningún dato.", connection -> {
                    int eventId;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT id_evento FROM participantes_evento WHERE id_participacion = ? FOR UPDATE")) {
                        statement.setInt(1, participationId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                throw new EntityNotFoundException(notFound(participationId));
                            }
                            eventId = resultSet.getInt(1);
                        }
                    }
                    if (position.isPodium()) {
                        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM "
                                + "participantes_evento WHERE id_evento = ? AND posicion_obtenida = ? "
                                + "AND id_participacion <> ?")) {
                            statement.setInt(1, eventId);
                            statement.setString(2, position.getLabel());
                            statement.setInt(3, participationId);
                            try (ResultSet resultSet = statement.executeQuery()) {
                                if (resultSet.next() && resultSet.getInt(1) > 0) {
                                    throw new BusinessRuleException("Otro competidor ya tiene el " + position.getLabel()
                                            + " en este evento.");
                                }
                            }
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE participantes_evento SET posicion_obtenida = ? WHERE id_participacion = ?")) {
                        statement.setString(1, position.getLabel());
                        statement.setInt(2, participationId);
                        statement.executeUpdate();
                    }
                    return null;
                });
    }

    public EventParticipant findById(int participationId) throws DatabaseException, EntityNotFoundException {
        List<EventParticipant> found = queryList(SELECT + " WHERE pe.id_participacion = ?",
                statement -> statement.setInt(1, participationId), this::mapRow,
                "Could not find participation id " + participationId + ".", "No se pudo consultar la participación.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException(notFound(participationId));
        }
        return found.get(0);
    }

    public List<EventParticipant> findByEvent(int eventId) throws DatabaseException {
        return queryList(SELECT + " WHERE pe.id_evento = ? ORDER BY e.primer_apellido, e.primer_nombre",
                statement -> statement.setInt(1, eventId), this::mapRow,
                "Could not list participants of event id " + eventId + ".", "No se pudo consultar los participantes.");
    }

    public List<EventParticipant> findByStudent(int studentId) throws DatabaseException {
        return queryList(SELECT + " WHERE pe.id_estudiante = ? ORDER BY v.fecha_evento DESC",
                statement -> statement.setInt(1, studentId), this::mapRow,
                "Could not list events of student id " + studentId + ".", "No se pudo consultar los eventos del estudiante.");
    }

    /** Every participation in a competition that has a position, used for the podium ranking. */
    public List<EventParticipant> findAllWithPosition() throws DatabaseException {
        return queryList(SELECT + " WHERE pe.posicion_obtenida IS NOT NULL", statement -> { }, this::mapRow,
                "Could not list the positions obtained in events.", "No se pudo consultar los resultados.");
    }

    private static String notFound(int participationId) {
        return "No se encontró una participación con el id " + participationId + ".";
    }

    private EventParticipant mapRow(ResultSet resultSet) throws SQLException {
        EventParticipant participant = new EventParticipant();
        participant.setParticipationId(resultSet.getInt("id_participacion"));
        participant.setEventId(resultSet.getInt("id_evento"));
        participant.setEventName(resultSet.getString("nombre_evento"));
        participant.setStudentId(resultSet.getInt("id_estudiante"));
        participant.setStudentName(resultSet.getString("estudiante"));
        try {
            participant.setRole(ParticipationRole.fromLabel(resultSet.getString("rol_participacion")));
            String position = resultSet.getString("posicion_obtenida");
            participant.setPosition(position == null ? null : EventPosition.fromLabel(position));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table participantes_evento: " + e.getMessage(), e);
        }
        participant.setNotes(resultSet.getString("observaciones"));
        return participant;
    }
}