package com.littlehorizons.erp;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.view.Console;
import com.littlehorizons.erp.view.InputReader;
import com.littlehorizons.erp.view.MainMenu;
import com.littlehorizons.erp.view.StudentMenu;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Entry point of the ERP. It builds the objects of every layer (DAOs, controllers, views)
 * and connects them, then opens the main menu.
 */
public class Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            verifyConnection(database);

            StudentDao studentDao = new StudentDao(database);
            GuardianDao guardianDao = new GuardianDao(database);
            StudentGuardianDao studentGuardianDao = new StudentGuardianDao(database, studentDao, guardianDao);

            StudentController studentController = new StudentController(studentDao, studentGuardianDao);
            GuardianController guardianController = new GuardianController(guardianDao, studentGuardianDao);

            InputReader input = new InputReader(new Scanner(System.in, StandardCharsets.UTF_8));
            StudentMenu studentMenu = new StudentMenu(input, studentController, guardianController);
            new MainMenu(input, studentMenu).run();
        } catch (ConfigurationException | DatabaseException e) {
            Console.error(e.getMessage());
        } catch (NoSuchElementException e) {
            Console.info("La entrada de datos terminó. Hasta pronto.");
        } catch (RuntimeException e) {
            AppLogger.getInstance().error("Unexpected error stopped the application.", e);
            Console.error("Ocurrió un error inesperado y el programa debe cerrarse. Consulte error_log.txt.");
        }
    }

    // fails fast, with a clear message, when MySQL cannot be reached
    private static void verifyConnection(DatabaseConnection database) throws DatabaseException {
        try {
            database.getConnection().close();
        } catch (SQLException e) {
            AppLogger.getInstance().error("Could not close the verification connection.", e);
        }
    }
}
