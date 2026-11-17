package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.enums.EventType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

/**
 * Data Access Object for the table "eventos". Every event is read together with the name of its organizer
 * and the number of students that take part in it.
 */
public class EventDao extends BaseDao {

    private static final int MYSQL_FOREIGN_KEY_FAILS = 1452;
    private static final String SELECT = "SELECT v.id_evento, v.nombre_evento, v.descripcion, v.tipo_evento, "
            + "v.fecha_evento, v.lugar, v.tiene_competencia, v.id_organizador, "
            + "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) AS organizador, "
            + "v.esta_activo, (SELECT COUNT(*) FROM participantes_evento pe WHERE pe.id_evento = v.id_evento) "
            + "AS participantes FROM eventos v LEFT JOIN profesores p ON p.id_profesor = v.id_organizador";

    public EventDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new event and sets the generated id on the given object.
     *
     * @return the generated id
     * @throws EntityNotFoundException if the organizer does not exist
     * @throws DatabaseException       if the event cannot be saved
     */
    public int insert(SchoolEvent event) throws DatabaseException, EntityNotFoundException {
        String sql = "INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
                + "tiene_competencia, id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEvent(statement, event);
            statement.setBoolean(8, event.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    event.setEventId(keys.getInt(1));
                }
            }
            return event.getEventId();
        } catch (SQLException e) {
            throw eventError(e, event, "insert");
        }
    }

    /**
     * Updates the data of an event.
     *
     * @throws EntityNotFoundException if the event or the organizer does not exist
     * @throws DatabaseException       if the event cannot be updated
     */
    public void update(SchoolEvent event) throws DatabaseException, EntityNotFoundException {
        String sql = "UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, fecha_evento = ?, "
                + "lugar = ?, tiene_competencia = ?, id_organizador = ? WHERE id_evento = ?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEvent(statement, event);
            statement.setInt(8, event.getEventId());
            if (statement.executeUpdate() == 0) {
                requireExists("eventos", "id_evento", event.getEventId(), notFound(event.getEventId()));
            }
        } catch (SQLException e) {
            throw eventError(e, event, "update");
        }
    }

    private static void bindEvent(PreparedStatement statement, SchoolEvent event) throws SQLException {
        statement.setString(1, event.getName());
        statement.setString(2, blankToNull(event.getDescription()));
        statement.setString(3, event.getType().getLabel());
        statement.setDate(4, toSqlDate(event.getDate()));
        statement.setString(5, blankToNull(event.getPlace()));
        statement.setBoolean(6, event.hasCompetition());
        if (event.getOrganizerId() == 0) {
            statement.setNull(7, Types.INTEGER);
        } else {
            statement.setInt(7, event.getOrganizerId());
        }
    }

    // explains an insert/update SQL error to the user; the return type only helps the caller to write "throw"
    private DatabaseException eventError(SQLException e, SchoolEvent event, String action)
            throws EntityNotFoundException {
        if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAILS) {
            throw new EntityNotFoundException("El profesor organizador indicado no existe.", e);
        }
        return databaseError("Could not " + action + " event " + event.getName() + ".",
                "No se pudo guardar el evento.", e);
    }

    public void changeActive(int eventId, boolean active) throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE eventos SET esta_activo = ? WHERE id_evento = ?", statement -> {
                statement.setBoolean(1, active);
                statement.setInt(2, eventId);
            }, "", "Could not change the status of event id " + eventId + ".", "No se pudo cambiar el estado del evento.");
            if (rows == 0) {
                requireExists("eventos", "id_evento", eventId, notFound(eventId));
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo cambiar el estado del evento.", e);
        }
    }

    public SchoolEvent findById(int eventId) throws DatabaseException, EntityNotFoundException {
        List<SchoolEvent> found = queryList(SELECT + " WHERE v.id_evento = ?", statement -> statement.setInt(1, eventId),
                this::mapRow, "Could not find event id " + eventId + ".", "No se pudo consultar el evento.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException(notFound(eventId));
        }
        return found.get(0);
    }

    public List<SchoolEvent> findAll() throws DatabaseException {
        return queryList(SELECT + " ORDER BY v.fecha_evento DESC, v.id_evento DESC", statement -> { }, this::mapRow,
                "Could not list events.", "No se pudo consultar los eventos.");
    }

    public List<SchoolEvent> findActive() throws DatabaseException {
        return queryList(SELECT + " WHERE v.esta_activo = 1 ORDER BY v.fecha_evento DESC, v.id_evento DESC",
                statement -> { }, this::mapRow, "Could not list active events.", "No se pudo consultar los eventos.");
    }

    private static String notFound(int eventId) {
        return "No se encontró un evento con el id " + eventId + ".";
    }

    private SchoolEvent mapRow(ResultSet resultSet) throws SQLException {
        SchoolEvent event = new SchoolEvent();
        event.setEventId(resultSet.getInt("id_evento"));
        event.setName(resultSet.getString("nombre_evento"));
        event.setDescription(resultSet.getString("descripcion"));
        try {
            event.setType(EventType.fromLabel(resultSet.getString("tipo_evento")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table eventos: " + e.getMessage(), e);
        }
        event.setDate(toLocalDate(resultSet.getDate("fecha_evento")));
        event.setPlace(resultSet.getString("lugar"));
        event.setCompetition(resultSet.getBoolean("tiene_competencia"));
        event.setOrganizerId(resultSet.getInt("id_organizador"));
        event.setOrganizerName(resultSet.getString("organizador"));
        event.setActive(resultSet.getBoolean("esta_activo"));
        event.setParticipantCount(resultSet.getInt("participantes"));
        return event;
    }
}