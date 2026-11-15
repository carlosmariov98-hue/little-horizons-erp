package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

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
