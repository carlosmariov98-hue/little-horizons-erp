package com.littlehorizons.erp;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        AppLogger logger = AppLogger.getInstance();
        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM estudiantes");
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println("Connected! Students in database: " + resultSet.getInt(1));
                }
            } catch (SQLException e) {
                logger.error("Test query failed.", e);
                System.out.println("The test query failed. See error_log.txt");
            }
        } catch (ErpException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}