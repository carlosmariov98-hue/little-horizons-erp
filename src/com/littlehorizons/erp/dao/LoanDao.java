package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the loans of resources. Students and teachers have their own table, so every method
 * receives the {@link BorrowerType}, which knows the table and column names (constants, never user input).
 * Lending and returning change the loan and the status of the resource, so both are done in one transaction.
 */
public class LoanDao extends BaseDao {

    public LoanDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    private static String select(BorrowerType type) {
        String id = "b." + type.getIdColumn();
        return "SELECT l.id_prestamo, l.id_recurso, r.codigo_recurso, r.nombre_recurso, " + id + " AS id_prestatario, "
                + "CONCAT_WS(' ', b.primer_nombre, b.segundo_nombre, b.primer_apellido, b.segundo_apellido) AS prestatario, "
                + "l.fecha_prestamo, l.fecha_devolucion_esperada, l.fecha_devolucion_real, l.estado_prestamo, "
                + "l.observaciones FROM " + type.getLoanTable() + " l JOIN recursos r ON r.id_recurso = l.id_recurso "
                + "JOIN " + type.getBorrowerTable() + " b ON " + id + " = l." + type.getIdColumn();
    }

    /**
     * Lends an available resource. Inside one transaction that locks the resource, it checks that the resource
     * is available, that the borrower has no late loans and has fewer than the allowed open loans, saves the loan
     * and marks the resource as lent.
     *
     * @param loan          the loan to save; it receives its generated id
     * @param today         current date, used to detect late loans
     * @param maxOpenLoans  maximum number of open loans a borrower can have
     * @return the generated id of the loan
     * @throws BusinessRuleException   if a rule is broken (nothing is saved)
     * @throws EntityNotFoundException if the resource or the borrower does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public int lend(Loan loan, LocalDate today, int maxOpenLoans)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        BorrowerType type = loan.getBorrowerType();
        return inTransaction("Could not lend resource id " + loan.getResourceId() + " to " + type + " id "
                        + loan.getBorrowerId() + ".", "No se pudo registrar el préstamo. No se guardó ningún dato.",
                connection -> {
                    requireAvailable(connection, loan.getResourceId());
                    checkBorrower(connection, type, loan.getBorrowerId(), today, maxOpenLoans);
                    int id = insertLoan(connection, loan);
                    setResourceStatus(connection, loan.getResourceId(), "Prestado");
                    loan.setLoanId(id);
                    return id;
                });
    }

    private static void requireAvailable(Connection connection, int resourceId)
            throws SQLException, BusinessRuleException, EntityNotFoundException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT estado_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE")) {
            statement.setInt(1, resourceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new EntityNotFoundException("No se encontró un recurso con el id " + resourceId + ".");
                }
                String status = resultSet.getString(1);
                if (!status.equals("Disponible")) {
                    throw new BusinessRuleException("El recurso no está disponible (estado actual: " + status + ").");
                }
            }
        }
    }

    private static void checkBorrower(Connection connection, BorrowerType type, int borrowerId, LocalDate today,
                                      int maxOpenLoans) throws SQLException, BusinessRuleException {
        String sql = "SELECT COUNT(*), COALESCE(SUM(estado_prestamo = 'Vencido' OR fecha_devolucion_esperada < ?), 0) "
                + "FROM " + type.getLoanTable() + " WHERE " + type.getIdColumn() + " = ? "
                + "AND estado_prestamo IN ('Activo', 'Vencido')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, toSqlDate(today));
            statement.setInt(2, borrowerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(2) > 0) {
                    throw new BusinessRuleException("El " + type.getLabel().toLowerCase()
                            + " tiene préstamos vencidos y no puede solicitar otro recurso.");
                }
                if (resultSet.getInt(1) >= maxOpenLoans) {
                    throw new BusinessRuleException("El " + type.getLabel().toLowerCase() + " ya tiene "
                            + resultSet.getInt(1) + " préstamos abiertos (máximo " + maxOpenLoans + ").");
                }
            }
        }
    }

    private static int insertLoan(Connection connection, Loan loan) throws SQLException {
        String sql = "INSERT INTO " + loan.getBorrowerType().getLoanTable() + " (id_recurso, "
                + loan.getBorrowerType().getIdColumn() + ", fecha_prestamo, fecha_devolucion_esperada, observaciones) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, loan.getResourceId());
            statement.setInt(2, loan.getBorrowerId());
            statement.setDate(3, toSqlDate(loan.getLoanDate()));
            statement.setDate(4, toSqlDate(loan.getExpectedReturnDate()));
            statement.setString(5, blankToNull(loan.getNotes()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the generated id.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void setResourceStatus(Connection connection, int resourceId, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?")) {
            statement.setString(1, status);
            statement.setInt(2, resourceId);
            statement.executeUpdate();
        }
    }

    /**
     * Registers the return of a loan and frees the resource (or sends it to maintenance when it came back damaged).
     *
     * @param type            type of borrower of the loan
     * @param loanId          id of the loan
     * @param returnDate      date the resource was returned
     * @param notes           observations to save with the loan
     * @param toMaintenance   true if the resource must go to maintenance instead of being available
     * @throws BusinessRuleException   if the loan was already returned or the date is before the loan date
     * @throws EntityNotFoundException if the loan does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public void giveBack(BorrowerType type, int loanId, LocalDate returnDate, String notes, boolean toMaintenance)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        inTransaction("Could not register the return of " + type + " loan id " + loanId + ".",
                "No se pudo registrar la devolución. No se guardó ningún dato.", connection -> {
                    int resourceId;
                    try (PreparedStatement statement = connection.prepareStatement("SELECT id_recurso, fecha_prestamo, "
                            + "estado_prestamo FROM " + type.getLoanTable() + " WHERE id_prestamo = ? FOR UPDATE")) {
                        statement.setInt(1, loanId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                throw new EntityNotFoundException("No se encontró un préstamo con el id " + loanId + ".");
                            }
                            if (LoanStatus.fromLabel(resultSet.getString("estado_prestamo")) == LoanStatus.RETURNED) {
                                throw new BusinessRuleException("Ese préstamo ya fue devuelto.");
                            }
                            if (returnDate.isBefore(toLocalDate(resultSet.getDate("fecha_prestamo")))) {
                                throw new BusinessRuleException("La fecha de devolución no puede ser anterior a la "
                                        + "fecha del préstamo.");
                            }
                            resourceId = resultSet.getInt("id_recurso");
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement("UPDATE " + type.getLoanTable()
                            + " SET estado_prestamo = 'Devuelto', fecha_devolucion_real = ?, observaciones = ? "
                            + "WHERE id_prestamo = ?")) {
                        statement.setDate(1, toSqlDate(returnDate));
                        statement.setString(2, blankToNull(notes));
                        statement.setInt(3, loanId);
                        statement.executeUpdate();
                    }
                    setResourceStatus(connection, resourceId, toMaintenance ? "En Mantenimiento" : "Disponible");
                    return null;
                });
    }

    /**
     * Marks as overdue every active loan whose expected return date is before today, for students and teachers.
     *
     * @param today current date
     * @return how many loans were marked
     * @throws DatabaseException if the update fails
     */
    public int markOverdue(LocalDate today) throws DatabaseException {
        int total = 0;
        for (BorrowerType type : BorrowerType.values()) {
            try {
                total += executeUpdate("UPDATE " + type.getLoanTable() + " SET estado_prestamo = 'Vencido' "
                                + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?",
                        statement -> statement.setDate(1, toSqlDate(today)), "",
                        "Could not mark overdue loans of " + type + ".", "No se pudo actualizar los préstamos vencidos.");
            } catch (BusinessRuleException | EntityNotFoundException e) {
                throw new DatabaseException("No se pudo actualizar los préstamos vencidos.", e);
            }
        }
        return total;
    }

    public Loan findById(BorrowerType type, int loanId) throws DatabaseException, EntityNotFoundException {
        List<Loan> found = queryList(select(type) + " WHERE l.id_prestamo = ?",
                statement -> statement.setInt(1, loanId), resultSet -> mapRow(resultSet, type),
                "Could not find " + type + " loan id " + loanId + ".", "No se pudo consultar el préstamo.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException("No se encontró un préstamo con el id " + loanId + ".");
        }
        return found.get(0);
    }

    /** Loans that have not been returned (active and overdue) of students and teachers. */
    public List<Loan> findOpen() throws DatabaseException {
        return findInBothTables(" WHERE l.estado_prestamo IN ('Activo', 'Vencido')", null, "open loans");
    }

    /** Every loan of students and teachers, used for the statistics. */
    public List<Loan> findAll() throws DatabaseException {
        return findInBothTables("", null, "loans");
    }

    public List<Loan> findByResource(int resourceId) throws DatabaseException {
        return findInBothTables(" WHERE l.id_recurso = ?", resourceId, "loans of resource id " + resourceId);
    }

    public List<Loan> findByBorrower(BorrowerType type, int borrowerId) throws DatabaseException {
        return queryList(select(type) + " WHERE l." + type.getIdColumn() + " = ? ORDER BY l.fecha_prestamo DESC",
                statement -> statement.setInt(1, borrowerId), resultSet -> mapRow(resultSet, type),
                "Could not list loans of " + type + " id " + borrowerId + ".", "No se pudo consultar los préstamos.");
    }

    private List<Loan> findInBothTables(String where, Integer parameter, String description) throws DatabaseException {
        List<Loan> result = new ArrayList<>();
        for (BorrowerType type : BorrowerType.values()) {
            result.addAll(queryList(select(type) + where, statement -> {
                        if (parameter != null) {
                            statement.setInt(1, parameter);
                        }
                    }, resultSet -> mapRow(resultSet, type), "Could not list " + description + ".",
                    "No se pudo consultar los préstamos."));
        }
        return result;
    }

    private Loan mapRow(ResultSet resultSet, BorrowerType type) throws SQLException {
        Loan loan = new Loan();
        loan.setLoanId(resultSet.getInt("id_prestamo"));
        loan.setBorrowerType(type);
        loan.setResourceId(resultSet.getInt("id_recurso"));
        loan.setResourceCode(resultSet.getString("codigo_recurso"));
        loan.setResourceName(resultSet.getString("nombre_recurso"));
        loan.setBorrowerId(resultSet.getInt("id_prestatario"));
        loan.setBorrowerName(resultSet.getString("prestatario"));
        loan.setLoanDate(toLocalDate(resultSet.getDate("fecha_prestamo")));
        loan.setExpectedReturnDate(toLocalDate(resultSet.getDate("fecha_devolucion_esperada")));
        loan.setActualReturnDate(toLocalDate(resultSet.getDate("fecha_devolucion_real")));
        try {
            loan.setStatus(LoanStatus.fromLabel(resultSet.getString("estado_prestamo")));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in " + type.getLoanTable() + ": " + e.getMessage(), e);
        }
        loan.setNotes(resultSet.getString("observaciones"));
        return loan;
    }
}