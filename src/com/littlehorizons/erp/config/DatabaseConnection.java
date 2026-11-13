package com.littlehorizons.erp.config;

import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides database connections (Singleton).
 * The configuration is read only once. Every call to {@link #getConnection()} returns a NEW connection
 * that the caller must close, normally with try-with-resources.
 */
public final class DatabaseConnection implements ConnectionProvider {

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() throws ConfigurationException {
        AppConfig config = new AppConfig();
        this.url = config.getRequired("db.url");
        this.user = config.getRequired("db.user");
        this.password = config.getOptional("db.password", "");
    }

    /**
     * Returns the single instance, creating it the first time it is requested.
     *
     * @return the shared DatabaseConnection
     * @throws ConfigurationException if db.properties is missing or incomplete
     */
    public static synchronized DatabaseConnection getInstance() throws ConfigurationException {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Opens a new connection to the database.
     *
     * @return an open connection; the caller is responsible for closing it
     * @throws DatabaseException if the connection cannot be established
     */
    @Override
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            AppLogger.getInstance().error("Could not open a database connection.", e);
            throw new DatabaseException("No se pudo conectar con la base de datos. "
                    + "Verifique que MySQL esté en ejecución y que db.properties sea correcto.", e);
        }
    }
}
