package test.vista;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.util.AppLogger;
import test.controlador.TeacherSubjectsController;
import test.controlador.TeacherSubjectsExporter;
import test.controlador.TeacherSubjectsQuery;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Independent entry point of the teacher subjects exporter. It does not use the login or the main menu of the ERP:
 * it only reuses the database connection of the project and runs the exporter directly.
 */
public class ExportadorMateriasProfesorMain {

    public static void main(String[] args) {
        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            TeacherSubjectsController controller = new TeacherSubjectsController(
                    new TeacherSubjectsQuery(database), new TeacherSubjectsExporter(Paths.get("")));
            new TeacherSubjectsView(new Scanner(System.in, StandardCharsets.UTF_8), controller).run();
        } catch (ConfigurationException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("La entrada de datos terminó. Hasta pronto.");
        } catch (RuntimeException e) {
            AppLogger.getInstance().error("Unexpected error stopped the exporter.", e);
            System.out.println("Error: Ocurrió un error inesperado. Consulte error_log.txt para más detalles.");
        }
    }
}