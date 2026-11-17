package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.util.DateUtils;
import com.littlehorizons.erp.util.MoneyFormat;
import com.littlehorizons.erp.util.ReportFileWriter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Console menu for the reports and statistics of the whole school. */
public class ReportMenu extends BaseMenu {

    private static final int TOP_LIMIT = 5;
    private static final String[] MONTHS = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto",
            "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private final ReportController reports;
    private final ReportFileWriter fileWriter;

    public ReportMenu(InputReader input, ReportController reports, ReportFileWriter fileWriter) {
        super(input);
        this.reports = reports;
        this.fileWriter = fileWriter;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("REPORTES Y ESTADÍSTICAS");
            Console.info("1. Estudiantes (por estado, género y grado)");
            Console.info("2. Rendimiento académico de un grado");
            Console.info("3. Asistencia");
            Console.info("4. Finanzas (recaudo y morosos)");
            Console.info("5. Ocupación de los clubes");
            Console.info("6. Resumen general del colegio (con opción de guardarlo en un archivo)");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 6)) {
                case 1:
                    runSafely(this::studentsReport);
                    break;
                case 2:
                    runSafely(this::academicReport);
                    break;
                case 3:
                    runSafely(this::attendanceReport);
                    break;
                case 4:
                    runSafely(this::financeReport);
                    break;
                case 5:
                    runSafely(this::clubsReport);
                    break;
                case 6:
                    runSafely(this::generalSummary);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private void studentsReport() throws ErpException {
        int year = readYear();
        Console.title("ESTUDIANTES");
        Console.info("Por estado:");
        reports.studentsByStatus().forEach((status, count) ->
                Console.info(String.format("  %-12s %d", status.getLabel(), count)));
        Console.info("Estudiantes activos por género:");
        reports.activeStudentsByGender().forEach((gender, count) ->
                Console.info(String.format("  %-12s %d", gender.getLabel(), count)));
        Console.info("Matriculados por grado en " + year + ":");
        reports.studentsPerGrade(year).forEach((grade, count) ->
                Console.info(String.format("  %-14s %d", grade, count)));
    }

    private void academicReport() throws ErpException {
        Grade grade = input.readChoice("Grado:", reports.listGrades(), Grade::getName);
        int year = readYear();
        AcademicPeriod period = readPeriod();
        List<StudentAverage> averages = reports.studentAverages(grade.getGradeId(), year, period);
        Console.title("RENDIMIENTO DE " + grade.getName().toUpperCase() + " - " + year + " PERÍODO "
                + period.getLabel());
        if (averages.isEmpty()) {
            Console.info("No hay notas registradas para ese grado, año y período.");
            return;
        }
        Console.info("Promedio por materia:");
        reports.subjectAverages(grade.getGradeId(), year, period).forEach((subject, average) ->
                Console.info(String.format("  %-34s %s", subject, average)));
        Console.info("Promedio general del grado: " + ReportController.overallAverage(averages));
        Console.info("Estudiantes por nivel de desempeño:");
        ReportController.levelDistribution(averages).forEach((level, count) ->
                Console.info(String.format("  %-10s %d", level.getLabel(), count)));
        Console.info("Mejores promedios:");
        ReportController.top(averages, TOP_LIMIT).forEach(a ->
                Console.info(String.format("  %-34s %s (%s)", a.getStudentName(), a.getAverage(),
                        a.getLevel().getLabel())));
        List<StudentAverage> risk = ReportController.atRisk(averages);
        Console.info("Estudiantes con promedio menor a " + ReportController.PASSING_SCORE + ": " + risk.size());
        risk.forEach(a -> Console.info(String.format("  %-34s %s", a.getStudentName(), a.getAverage())));
    }

    private void attendanceReport() throws ErpException {
        LocalDate today = LocalDate.now();
        LocalDate from = input.readDate("Desde (dd/MM/yyyy, Enter = 01/01/" + today.getYear() + "): ",
                LocalDate.of(today.getYear(), 1, 1));
        LocalDate to = input.readDate("Hasta (dd/MM/yyyy, Enter = hoy): ", today);
        Map<AttendanceStatus, Long> summary = reports.attendanceSummary(from, to);
        long total = summary.values().stream().mapToLong(Long::longValue).sum();
        Console.title("ASISTENCIA DEL " + DateUtils.format(from) + " AL " + DateUtils.format(to));
        if (total == 0) {
            Console.info("No hay registros de asistencia en ese rango.");
            return;
        }
        summary.forEach((status, count) -> Console.info(String.format("  %-15s %5d  (%s%%)", status.getLabel(), count,
                ReportController.percentage(count, total))));
        Console.info("  Total de registros: " + total);
        Console.info("Estudiantes con más ausencias:");
        reports.topAbsences(from, to, TOP_LIMIT).forEach((name, count) ->
                Console.info(String.format("  %-34s %d", name, count)));
    }

    private void financeReport() throws ErpException {
        int year = readYear();
        Console.title("FINANZAS " + year);
        Map<String, BigDecimal> byConcept = reports.incomeByConcept(year);
        Console.info("Recaudo por concepto:");
        byConcept.forEach((concept, total) -> Console.info(String.format("  %-34s %s", concept,
                MoneyFormat.format(total))));
        Console.info("Recaudo total: " + MoneyFormat.format(byConcept.values().stream().reduce(BigDecimal.ZERO,
                BigDecimal::add)));
        Console.info("Recaudo por mes:");
        reports.incomeByMonth(year).forEach((month, total) -> Console.info(String.format("  %-12s %s",
                MONTHS[month - 1], MoneyFormat.format(total))));
        Console.info("Estudiantes morosos (sin pensión en los últimos 30 días): " + reports.delinquentCount());
    }

    private void clubsReport() throws ErpException {
        Console.title("OCUPACIÓN DE LOS CLUBES");
        List<Club> clubs = reports.clubOccupancy();
        if (clubs.isEmpty()) {
            Console.info("No hay clubes activos.");
            return;
        }
        for (Club club : clubs) {
            Console.info(String.format("  %-36s %3d / %-3d  %s%%", club.getName(), club.getActiveMembers(),
                    club.getMaxCapacity(), ReportController.occupancyPercent(club)));
        }
    }

    private void generalSummary() throws ErpException {
        int year = readYear();
        Map<String, String> summary = reports.generalSummary(year);
        Console.title("RESUMEN GENERAL " + year);
        summary.forEach((label, value) -> Console.info(String.format("  %-36s %s", label, value)));
        if (input.readYesNo("¿Guardar este resumen en un archivo de texto? (s/n): ")) {
            Path file = fileWriter.write("ERP Colegio Little Horizons - Resumen general " + year, summary,
                    LocalDate.now());
            Console.success("Resumen guardado en " + file + ".");
        }
    }
}