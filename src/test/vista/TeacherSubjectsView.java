package test.vista;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.FileOperationException;
import test.controlador.TeacherSubjectsController;
import test.modelo.TeacherSubjectsReport;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Console screen of the exporter. It asks for the id of the teacher until a valid one is entered,
 * shows the messages and does not contain any query or file logic.
 */
public class TeacherSubjectsView {

    private static final String DOUBLE_LINE = "=".repeat(79);
    private static final String PROMPT = "Ingrese el ID del profesor para exportar sus materias asignadas: ";

    private final Scanner scanner;
    private final TeacherSubjectsController controller;

    public TeacherSubjectsView(Scanner scanner, TeacherSubjectsController controller) {
        this.scanner = scanner;
        this.controller = controller;
    }

    /**
     * Runs the whole flow: title, id request, validations, file generation and result message.
     */
    public void run() {
        printTitle();
        while (true) {
            int teacherId = readTeacherId();
            try {
                TeacherSubjectsReport report = controller.loadReport(teacherId);
                writeAndConfirm(teacherId, report);
                return;
            } catch (EntityNotFoundException e) {
                System.out.println();
                System.out.println("Error: " + e.getMessage());
                System.out.println("Verifique el ID e intente nuevamente.");
                System.out.println();
            } catch (BusinessRuleException | DatabaseException e) {
                System.out.println();
                System.out.println("Error: " + e.getMessage());
                System.out.println();
                waitForEnter();
                return;
            }
        }
    }

    private void printTitle() {
        System.out.println(DOUBLE_LINE);
        System.out.println("     EXPORTADOR DE MATERIAS ASIGNADAS A PROFESOR - AÑO "
                + TeacherSubjectsController.ACADEMIC_YEAR);
        System.out.println(DOUBLE_LINE);
        System.out.println();
    }

    // asks again until the text is a whole number
    private int readTeacherId() {
        while (true) {
            System.out.print(PROMPT);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println();
                System.out.println("Error: Debe ingresar un número válido.");
            }
        }
    }

    private void writeAndConfirm(int teacherId, TeacherSubjectsReport report) {
        System.out.println();
        System.out.println("Generando reporte para profesor con ID " + teacherId + "...");
        System.out.println("Consultando asignaciones del año " + report.getYear() + "...");
        System.out.println();
        try {
            Path file = controller.exportToFile(report);
            System.out.println(DOUBLE_LINE);
            System.out.println("            ARCHIVO GENERADO EXITOSAMENTE");
            System.out.println(DOUBLE_LINE);
            System.out.println();
            System.out.println("Profesor: " + report.getTeacher().getFullName());
            System.out.println("Archivo generado: " + file.getFileName());
            System.out.println("Ubicación: " + file);
            System.out.println();
            System.out.println("El archivo está listo para ser utilizado.");
            System.out.println();
        } catch (FileOperationException e) {
            System.out.println(DOUBLE_LINE);
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("Verifique los permisos de escritura en la carpeta del proyecto.");
            System.out.println(DOUBLE_LINE);
            System.out.println();
        }
        waitForEnter();
    }

    private void waitForEnter() {
        System.out.println(DOUBLE_LINE);
        System.out.println("Presione Enter para salir...");
        System.out.println(DOUBLE_LINE);
        scanner.nextLine();
    }
}