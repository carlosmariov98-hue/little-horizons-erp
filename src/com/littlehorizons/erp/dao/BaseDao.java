package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Common behaviour shared by all DAOs: obtaining connections, reporting SQL errors
 * and converting between Java and SQL values.
 */
public abstract class BaseDao {

    private final ConnectionProvider connectionProvider;

    protected BaseDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Work that must be executed inside one database transaction.
     *
     * @param <T> type of the value returned by the work
     */
    @FunctionalInterface
    protected interface TransactionalAction<T> {
        T execute(Connection connection) throws SQLException, BusinessRuleException, EntityNotFoundException;
    }

    /**
     * Sets the parameters of a PreparedStatement.
     */
    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /**
     * Converts the current row of a ResultSet into an object.
     *
     * @param <T> type of the object
     */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    /**
     * Runs a SELECT and converts every row into an object. Connection, statement and result set are closed here.
     *
     * @param sql              the query, with ? placeholders
     * @param binder           sets the placeholders
     * @param mapper           converts each row
     * @param technicalMessage message written in the log if the query fails
     * @param userMessage      message shown to the user if the query fails
     * @param <T>              type of the objects
     * @return the list of objects (empty when there are no rows)
     * @throws DatabaseException if the query fails
     */
    protected <T> List<T> queryList(String sql, Binder binder, RowMapper<T> mapper, String technicalMessage,
                                    String userMessage) throws DatabaseException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(mapper.map(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw databaseError(technicalMessage, userMessage, e);
        }
    }

    /**
     * Runs a SELECT COUNT(*) style query and returns the first number.
     */
    protected int queryCount(String sql, Binder binder, String technicalMessage, String userMessage)
            throws DatabaseException {
        List<Integer> result = queryList(sql, binder, resultSet -> resultSet.getInt(1), technicalMessage, userMessage);
        return result.isEmpty() ? 0 : result.get(0);
    }

    /**
     * Runs an INSERT, UPDATE or DELETE and returns the number of rows changed.
     * A duplicate key becomes a BusinessRuleException and a missing reference an EntityNotFoundException.
     *
     * @param sql              the statement, with ? placeholders
     * @param binder           sets the placeholders
     * @param duplicateMessage message shown to the user when a unique key is violated
     * @param technicalMessage message written in the log if the statement fails
     * @param userMessage      message shown to the user if the statement fails
     * @return the number of rows changed
     * @throws BusinessRuleException   if a unique key is violated
     * @throws EntityNotFoundException if a referenced record does not exist
     * @throws DatabaseException       if a technical error happens
     */
    protected int executeUpdate(String sql, Binder binder, String duplicateMessage, String technicalMessage,
                                String userMessage)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw translate(e, duplicateMessage, technicalMessage, userMessage);
        }
    }

    /**
     * Checks that a record exists. The table and column names are constants written in the DAOs, never user input.
     *
     * @param table           table name
     * @param idColumn        primary key column
     * @param id              value of the key
     * @param notFoundMessage message shown to the user when there is no such record
     * @throws EntityNotFoundException if the record does not exist
     * @throws DatabaseException       if the query fails
     */
    protected void requireExists(String table, String idColumn, int id, String notFoundMessage)
            throws DatabaseException, EntityNotFoundException {
        int count = queryCount("SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = ?",
                statement -> statement.setInt(1, id), "Could not check " + table + " id " + id + ".",
                "No se pudo verificar el registro.");
        if (count == 0) {
            throw new EntityNotFoundException(notFoundMessage);
        }
    }

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_FOREIGN_KEY_FAILS = 1452;

    private DatabaseException translate(SQLException e, String duplicateMessage, String technicalMessage,
                                        String userMessage) throws BusinessRuleException, EntityNotFoundException {
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            throw new BusinessRuleException(duplicateMessage, e);
        }
        if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAILS) {
            throw new EntityNotFoundException("Alguno de los datos referenciados no existe.");
        }
        return databaseError(technicalMessage, userMessage, e);
    }

    protected Connection getConnection() throws DatabaseException {
        return connectionProvider.getConnection();
    }

    /**
     * Runs the given work as an atomic transaction: if anything fails, every change is rolled back,
     * so the database never keeps half of an operation.
     *
     * @param technicalMessage message written in the log when a SQL error happens
     * @param userMessage      message shown to the user when a SQL error happens
     * @param action           the work to execute with the transaction connection
     * @param <T>              type of the value returned by the work
     * @return the value returned by the work
     * @throws BusinessRuleException   if the work violates a business rule (everything is rolled back)
     * @throws EntityNotFoundException if the work refers to a record that does not exist
     * @throws DatabaseException       if a technical database error happens
     */
    protected <T> T inTransaction(String technicalMessage, String userMessage, TransactionalAction<T> action)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = action.execute(connection);
                connection.commit();
                return result;
            } catch (BusinessRuleException | EntityNotFoundException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError(technicalMessage, userMessage, e);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            AppLogger.getInstance().error("Rollback failed.", e);
        }
    }

    /**
     * Logs a technical SQL error and builds the exception that will be shown to the user.
     * The error is logged here, where it is detected, so upper layers do not need to log it again.
     *
     * @param technicalMessage message written in the log (developer audience)
     * @param userMessage      message shown in the console (user audience)
     * @param cause            the original SQL exception
     * @return the exception that the caller must throw
     */
    protected DatabaseException databaseError(String technicalMessage, String userMessage, SQLException cause) {
        AppLogger.getInstance().error(technicalMessage, cause);
        return new DatabaseException(userMessage, cause);
    }

    protected static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    protected static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    /**
     * Converts empty or blank text to null so optional columns are stored as NULL, not as ''.
     *
     * @param text the text to normalize
     * @return the trimmed text, or null when it is null or blank
     */
    protected static String blankToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}