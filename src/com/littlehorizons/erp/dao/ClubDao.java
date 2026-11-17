package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Club;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

/**
 * Data Access Object for the table "clubes". Every club is read together with the name of its supervisor
 * and the number of members that are currently active.
 */
public class ClubDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_FOREIGN_KEY_FAILS = 1452;
    private static final String SELECT = "SELECT c.id_club, c.nombre_club, c.descripcion, c.id_profesor_supervisor, "
            + "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) AS supervisor, "
            + "c.capacidad_maxima, c.horario_reunion, c.esta_activo, "
            + "(SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') "
            + "AS miembros FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor";

    public ClubDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a new club and sets the generated id on the given object.
     *
     * @return the generated id
     * @throws BusinessRuleException   if the name is already used
     * @throws EntityNotFoundException if the supervisor does not exist
     * @throws DatabaseException       if the club cannot be saved
     */
    public int insert(Club club) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        String sql = "INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, "
                + "horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindClub(statement, club);
            statement.setBoolean(6, club.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    club.setClubId(keys.getInt(1));
                }
            }
            return club.getClubId();
        } catch (SQLException e) {
            throw clubError(e, club, "insert");
        }
    }

    /**
     * Updates the data of a club (name, description, supervisor, capacity and schedule).
     *
     * @throws BusinessRuleException   if the new name is already used by another club
     * @throws EntityNotFoundException if the club or the supervisor does not exist
     * @throws DatabaseException       if the club cannot be updated
     */
    public void update(Club club) throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        String sql = "UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, "
                + "capacidad_maxima = ?, horario_reunion = ? WHERE id_club = ?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindClub(statement, club);
            statement.setInt(6, club.getClubId());
            if (statement.executeUpdate() == 0) {
                requireExists("clubes", "id_club", club.getClubId(), notFound(club.getClubId()));
            }
        } catch (SQLException e) {
            throw clubError(e, club, "update");
        }
    }

    private static void bindClub(PreparedStatement statement, Club club) throws SQLException {
        statement.setString(1, club.getName());
        statement.setString(2, blankToNull(club.getDescription()));
        if (club.getSupervisorId() == 0) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, club.getSupervisorId());
        }
        statement.setInt(4, club.getMaxCapacity());
        statement.setString(5, blankToNull(club.getSchedule()));
    }

    // explains an insert/update SQL error to the user; always throws, the return type only helps the caller
    private DatabaseException clubError(SQLException e, Club club, String action)
            throws BusinessRuleException, EntityNotFoundException {
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            throw new BusinessRuleException("Ya existe un club con el nombre " + club.getName() + ".", e);
        }
        if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAILS) {
            throw new EntityNotFoundException("El profesor supervisor indicado no existe.", e);
        }
        return databaseError("Could not " + action + " club " + club.getName() + ".",
                "No se pudo guardar el club.", e);
    }

    public void changeActive(int clubId, boolean active) throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE clubes SET esta_activo = ? WHERE id_club = ?", statement -> {
                statement.setBoolean(1, active);
                statement.setInt(2, clubId);
            }, "", "Could not change the status of club id " + clubId + ".", "No se pudo cambiar el estado del club.");
            if (rows == 0) {
                requireExists("clubes", "id_club", clubId, notFound(clubId));
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo cambiar el estado del club.", e);
        }
    }

    public Club findById(int clubId) throws DatabaseException, EntityNotFoundException {
        List<Club> found = queryList(SELECT + " WHERE c.id_club = ?", statement -> statement.setInt(1, clubId),
                this::mapRow, "Could not find club id " + clubId + ".", "No se pudo consultar el club.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException(notFound(clubId));
        }
        return found.get(0);
    }

    public List<Club> findAll() throws DatabaseException {
        return queryList(SELECT + " ORDER BY c.nombre_club", statement -> { }, this::mapRow,
                "Could not list clubs.", "No se pudo consultar los clubes.");
    }

    public List<Club> findActive() throws DatabaseException {
        return queryList(SELECT + " WHERE c.esta_activo = 1 ORDER BY c.nombre_club", statement -> { }, this::mapRow,
                "Could not list active clubs.", "No se pudo consultar los clubes.");
    }

    private static String notFound(int clubId) {
        return "No se encontró un club con el id " + clubId + ".";
    }

    private Club mapRow(ResultSet resultSet) throws SQLException {
        Club club = new Club();
        club.setClubId(resultSet.getInt("id_club"));
        club.setName(resultSet.getString("nombre_club"));
        club.setDescription(resultSet.getString("descripcion"));
        club.setSupervisorId(resultSet.getInt("id_profesor_supervisor"));
        club.setSupervisorName(resultSet.getString("supervisor"));
        club.setMaxCapacity(resultSet.getInt("capacidad_maxima"));
        club.setSchedule(resultSet.getString("horario_reunion"));
        club.setActive(resultSet.getBoolean("esta_activo"));
        club.setActiveMembers(resultSet.getInt("miembros"));
        return club;
    }
}