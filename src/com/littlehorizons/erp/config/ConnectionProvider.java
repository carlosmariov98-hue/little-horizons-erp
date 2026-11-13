package com.littlehorizons.erp.config;

import com.littlehorizons.erp.exception.DatabaseException;

import java.sql.Connection;

/**
 * Abstraction for "something that can give me a database connection".
 * DAOs depend on this interface instead of a concrete class (Dependency Inversion Principle).
 */
public interface ConnectionProvider {

    /**
     * Opens a new connection. The caller is responsible for closing it.
     *
     * @return an open connection
     * @throws DatabaseException if the connection cannot be established
     */
    Connection getConnection() throws DatabaseException;
}
