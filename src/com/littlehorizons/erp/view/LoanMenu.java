package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Console menu for the loans of resources to students and teachers. */
public class LoanMenu extends BaseMenu {

    private static final int DEFAULT_LOAN_DAYS = 14;
    private static final int TOP_LIMIT = 5;

    private final LoanController loans;
    private final ResourceController resources;
    private final StudentController students;
    private final TeacherController teachers;

    public LoanMenu(InputReader input, LoanController loans, ResourceController resources, StudentController students,
                    TeacherController teachers) {
        super(input);
        this.loans = loans;
        this.resources = resources;
        this.students = students;
        this.teachers = teachers;
    }

    public void run() {
        runSafely(() -> {
            int marked = loans.refreshOverdue();
            if (marked > 0) {
                Console.info("Se marcaron " + marked + " préstamos como vencidos.");
            }
        });
        boolean back = false;
        while (!back) {
            Console.title("PRÉSTAMOS DE RECURSOS");
            Console.info("1. Prestar un recurso");
            Console.info("2. Registrar la devolución de un recurso");
            Console.info("3. Ver los préstamos abiertos");
            Console.info("4. Ver los préstamos vencidos");
            Console.info("5. Ver los préstamos de un estudiante o profesor");
            Console.info("6. Ver el historial de préstamos de un recurso");
            Console.info("7. Estadísticas de préstamos");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 7)) {
                case 1:
                    runSafely(this::lend);
                    break;
                case 2:
                    runSafely(this::giveBack);
                    break;
                case 3:
                    runSafely(() -> printLoans("PRÉSTAMOS ABIERTOS", loans.listOpen()));
                    break;
                case 4:
                    runSafely(() -> printLoans("PRÉSTAMOS VENCIDOS", loans.listOverdue()));
                    break;
                case 5:
                    runSafely(this::borrowerLoans);
                    break;
                case 6:
                    runSafely(this::resourceHistory);
                    break;
                case 7:
                    runSafely(this::statistics);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private void printLoans(String title, List<Loan> list) {
        Console.title(title + " (" + list.size() + ")");
        if (list.isEmpty()) {
            Console.info("No hay préstamos para mostrar.");
            return;
        }
        LocalDate today = LocalDate.now();
        for (Loan l : list) {
            Console.info(String.format("%-9s %3d | %-9s %-30s | %-30s | %s a %s | %-8s%s", l.getBorrowerType().getLabel(),
                    l.getLoanId(), l.getResourceCode(), shorten(l.getResourceName(), 30),
                    shorten(l.getBorrowerName(), 30), DateUtils.format(l.getLoanDate()),
                    DateUtils.format(l.getExpectedReturnDate()), l.getStatus().getLabel(),
                    l.getDaysLate(today) > 0 ? " (" + l.getDaysLate(today) + " días de retraso)" : ""));
        }
    }

    private static String shorten(String text, int max) {
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

    private void lend() throws ErpException {
        Console.title("PRESTAR UN RECURSO");
        BorrowerType type = input.readChoice("¿A quién se presta?", Arrays.asList(BorrowerType.values()),
                BorrowerType::getLabel);
        int borrowerId;
        if (type == BorrowerType.STUDENT) {
            borrowerId = students.getById(readId("ID del estudiante: ")).getStudentId();
        } else {
            borrowerId = teachers.getById(readId("ID del profesor: ")).getTeacherId();
        }
        Resource resource = resources.getById(readId("ID del recurso: "));
        Console.info("Recurso: " + resource.getCode() + " - " + resource.getName() + " (" + resource.getStatus().getLabel()
                + ")");
        LocalDate today = LocalDate.now();
        LocalDate loanDate = input.readDate("Fecha del préstamo (dd/MM/yyyy, Enter = hoy): ", today);
        LocalDate expected = input.readDate("Fecha de devolución (dd/MM/yyyy, Enter = " + DateUtils.format(
                loanDate.plusDays(DEFAULT_LOAN_DAYS)) + "): ", loanDate.plusDays(DEFAULT_LOAN_DAYS));
        String notes = input.readLine("Observaciones (Enter = ninguna): ");
        Loan loan = loans.lendResource(type, borrowerId, resource.getResourceId(), loanDate, expected, notes);
        Console.success("Préstamo registrado: " + loan.getResourceName() + " a " + loan.getBorrowerName()
                + ", devolver el " + DateUtils.format(loan.getExpectedReturnDate()) + ".");
    }

    private void giveBack() throws ErpException {
        Console.title("DEVOLVER UN RECURSO");
        List<Loan> open = loans.listOpen();
        if (open.isEmpty()) {
            throw new BusinessRuleException("No hay préstamos abiertos.");
        }
        Loan loan = input.readChoice("Préstamo a devolver:", open, l -> l.getResourceCode() + " " + l.getResourceName()
                + " | " + l.getBorrowerType().getLabel() + ": " + l.getBorrowerName() + " | vence "
                + DateUtils.format(l.getExpectedReturnDate()) + " (" + l.getStatus().getLabel() + ")");
        LocalDate returnDate = input.readDate("Fecha de devolución (dd/MM/yyyy, Enter = hoy): ", LocalDate.now());
        boolean damaged = input.readYesNo("¿El recurso llegó dañado? Si es así pasa a mantenimiento (s/n): ");
        String notes = input.readLine("Observaciones (Enter = ninguna): ");
        Loan result = loans.returnResource(loan.getBorrowerType(), loan.getLoanId(), returnDate, damaged, notes);
        Console.success("Devolución registrada. " + result.getNotes());
    }

    private void borrowerLoans() throws ErpException {
        BorrowerType type = input.readChoice("Tipo:", Arrays.asList(BorrowerType.values()), BorrowerType::getLabel);
        int id = readId("ID del " + type.getLabel().toLowerCase() + ": ");
        printLoans("PRÉSTAMOS DE " + type.getLabel().toUpperCase() + " " + id, loans.getLoansOfBorrower(type, id));
    }

    private void resourceHistory() throws ErpException {
        Resource resource = resources.getById(readId("ID del recurso: "));
        printLoans("HISTORIAL DE " + resource.getCode(), loans.getLoansOfResource(resource.getResourceId()));
    }

    private void statistics() throws ErpException {
        Console.title("ESTADÍSTICAS DE PRÉSTAMOS");
        Console.info("Recursos más prestados:");
        loans.getMostBorrowed(TOP_LIMIT).forEach((name, count) -> Console.info(String.format("  %-60s %d", name, count)));
        Console.info("Préstamos por tipo de prestatario:");
        loans.countByBorrowerType().forEach((type, count) -> Console.info(String.format("  %-12s %d", type, count)));
        BigDecimal average = loans.averageDaysLate();
        Console.info("Devoluciones tardías: " + loans.countLateReturns() + " (promedio de retraso: " + average
                + " días)");
    }
}