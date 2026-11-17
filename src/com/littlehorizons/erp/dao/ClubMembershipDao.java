package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.enums.MembershipStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for the table "membresias_club". A student has at most one row per club:
 * when a student who left a club joins it again, the same row is activated again.
 */
public class ClubMembershipDao extends BaseDao {

    private static final String SELECT = "SELECT m.id_membresia, m.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS estudiante, "
            + "m.id_club, c.nombre_club, m.fecha_ingreso, m.estado_membresia, m.fecha_retiro, m.motivo_retiro "
            + "FROM membresias_club m JOIN estudiantes e ON e.id_estudiante = m.id_estudiante "
            + "JOIN clubes c ON c.id_club = m.id_club";

    public ClubMembershipDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Enrolls a student in a club. The capacity is checked inside a transaction that locks the club row,
     * so two people enrolling at the same time can never exceed the maximum number of members.
     *
     * @return the id of the membership
     * @throws BusinessRuleException   if the club is inactive, full, or the student is already an active member
     * @throws EntityNotFoundException if the club or the student does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public int join(int studentId, int clubId, LocalDate date)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        return inTransaction("Could not enroll student id " + studentId + " in club id " + clubId + ".",
                "No se pudo inscribir al estudiante en el club. No se guardó ningún dato.", connection -> {
                    int capacity;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT capacidad_maxima, esta_activo FROM clubes WHERE id_club = ? FOR UPDATE")) {
                        statement.setInt(1, clubId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                throw new EntityNotFoundException("No se encontró un club con el id " + clubId + ".");
                            }
                            if (!resultSet.getBoolean("esta_activo")) {
                                throw new BusinessRuleException("El club está inactivo y no recibe nuevos miembros.");
                            }
                            capacity = resultSet.getInt("capacidad_maxima");
                        }
                    }
                    int members = scalar(connection, "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? "
                            + "AND estado_membresia = 'Activa'", clubId);
                    if (members >= capacity) {
                        throw new BusinessRuleException("El club está lleno (" + members + " de " + capacity + " cupos).");
                    }
                    return saveMembership(connection, studentId, clubId, date);
                });
    }

    // creates the membership, or activates again the one of a student who had left the club
    private int saveMembership(Connection connection, int studentId, int clubId, LocalDate date)
            throws SQLException, BusinessRuleException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id_membresia, estado_membresia "
                + "FROM membresias_club WHERE id_estudiante = ? AND id_club = ? FOR UPDATE")) {
            statement.setInt(1, studentId);
            statement.setInt(2, clubId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    if (MembershipStatus.fromLabel(resultSet.getString("estado_membresia")) == MembershipStatus.ACTIVE) {
                        throw new BusinessRuleException("El estudiante ya es miembro activo de este club.");
                    }
                    int id = resultSet.getInt("id_membresia");
                    try (PreparedStatement update = connection.prepareStatement("UPDATE membresias_club SET "
                            + "estado_membresia = 'Activa', fecha_ingreso = ?, fecha_retiro = NULL, motivo_retiro = NULL "
                            + "WHERE id_membresia = ?")) {
                        update.setDate(1, toSqlDate(date));
                        update.setInt(2, id);
                        update.executeUpdate();
                    }
                    return id;
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO membresias_club "
                + "(id_estudiante, id_club, fecha_ingreso) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setInt(1, studentId);
            insert.setInt(2, clubId);
            insert.setDate(3, toSqlDate(date));
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the generated id.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static int scalar(Connection connection, String sql, int parameter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    /**
     * Marks an active membership as withdrawn.
     *
     * @throws BusinessRuleException   if the membership is not active
     * @throws EntityNotFoundException if the membership does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public void withdraw(int membershipId, LocalDate date, String reason)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        int rows = executeUpdate("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, "
                        + "motivo_retiro = ? WHERE id_membresia = ? AND estado_membresia = 'Activa'", statement -> {
                    statement.setDate(1, toSqlDate(date));
                    statement.setString(2, reason);
                    statement.setInt(3, membershipId);
                }, "", "Could not withdraw membership id " + membershipId + ".",
                "No se pudo retirar al estudiante del club.");
        if (rows == 0) {
            requireExists("membresias_club", "id_membresia", membershipId,
                    "No se encontró una membresía con el id " + membershipId + ".");
            throw new BusinessRuleException("La membresía ya estaba retirada.");
        }
    }

    public ClubMembership findById(int membershipId) throws DatabaseException, EntityNotFoundException {
        List<ClubMembership> found = queryList(SELECT + " WHERE m.id_membresia = ?",
                statement -> statement.setInt(1, membershipId), this::mapRow,
                "Could not find membership id " + membershipId + ".", "No se pudo consultar la membresía.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException("No se encontró una membresía con el id " + membershipId + ".");
        }
        return found.get(0);
    }

    /** The active membership of a student in a club, or an empty list when there is none. */
    public List<ClubMembership> findActiveOf(int studentId, int clubId) throws DatabaseException {
        return queryList(SELECT + " WHERE m.id_estudiante = ? AND m.id_club = ? AND m.estado_membresia = 'Activa'",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, clubId);
                }, this::mapRow, "Could not find the membership of student id " + studentId + " in club id " + clubId
                        + ".", "No se pudo consultar la membresía.");
    }

    public List<ClubMembership> findByClub(int clubId) throws DatabaseException {
        return queryList(SELECT + " WHERE m.id_club = ? ORDER BY m.estado_membresia, e.primer_apellido, e.primer_nombre",
                statement -> statement.setInt(1, clubId), this::mapRow,
                "Could not list members of club id " + clubId + ".", "No se pudo consultar los miembros del club.");
    }

    public List<ClubMembership> findByStudent(int studentId) throws DatabaseException {
        return queryList(SELECT + " WHERE m.id_estudiante = ? ORDER BY m.estado_membresia, c.nombre_club",
                statement -> statement.setInt(1, studentId), this::mapRow,
                "Could not list clubs of student id " + studentId + ".", "No se pudo consultar los clubes del estudiante.");
    }

    private ClubMembership mapRow(ResultSet resultSet) throws SQLException {
        ClubMembership membership = new ClubMembership();
        membership.setMembershipId(resultSet.getInt("id_membresia"));
        membership.setStudentId(resultSet.getInt("id_estudiante"));
        membership.setStudentName(resultSet.getString("estudiante"));
        membership.setClubId(resultSet.getInt("id_club"));
        membership.setClubName(resultSet.getString("nombre_club"));
        membership.setJoinDate(toLocalDate(resultSet.getDate("fecha_ingreso")));
        try {
            membership.setStatus(MembershipStatus.fromLabel(resultSet.getString("estado_membresia")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table membresias_club: " + e.getMessage(), e);
        }
        membership.setLeaveDate(toLocalDate(resultSet.getDate("fecha_retiro")));
        membership.setLeaveReason(resultSet.getString("motivo_retiro"));
        return membership;
    }
}