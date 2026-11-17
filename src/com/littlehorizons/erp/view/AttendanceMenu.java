package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.util.DateUtils;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Console menu for attendance control (control de asistencia). */
public class AttendanceMenu extends BaseMenu {

    private static final List<AttendanceStatus> STATUSES = Arrays.asList(AttendanceStatus.values());

    private final AttendanceController attendance;
    private final AcademicController academic;
    private final StudentController students;
    private final TeacherController teachers;

    public AttendanceMenu(InputReader input, AttendanceController attendance, AcademicController academic,
                          StudentController students, TeacherController teachers) {
        super(input);
        this.attendance = attendance;
        this.academic = academic;
        this.students = students;
        this.teachers = teachers;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("CONTROL DE ASISTENCIA");
            Console.info("1. Pasar lista de una materia (todo el curso)");
            Console.info("2. Registrar la asistencia de un estudiante");
            Console.info("3. Corregir una asistencia");
            Console.info("4. Ver la asistencia de un estudiante");
            Console.info("5. Ver la asistencia de una materia en una fecha");
            Console.info("6. Reporte semanal: estudiantes con más de 3 ausencias");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 6)) {
                case 1:
                    runSafely(this::takeRoll);
                    break;
                case 2:
                    runSafely(() -> saveAttendance(false));
                    break;
                case 3:
                    runSafely(() -> saveAttendance(true));
                    break;
                case 4:
                    runSafely(this::studentAttendance);
                    break;
                case 5:
                    runSafely(this::subjectAttendance);
                    break;
                case 6:
                    runSafely(this::weeklyReport);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private LocalDate readClassDate() throws ErpException {
        return input.readValid("Fecha de la clase (dd/mm/aaaa, Enter = hoy): ", text -> {
            LocalDate date = text.isEmpty() ? LocalDate.now() : DateUtils.parse(text);
            AttendanceController.validateClassDate(date);
            return date;
        });
    }

    private Teacher askTeacher() throws ErpException {
        Teacher teacher = teachers.getById(readId("ID del profesor que registra: "));
        Console.info("Profesor: " + teacher.getFullName());
        return teacher;
    }

    private AttendanceStatus readStatus() throws ErpException {
        return input.readChoice("Estado de asistencia:", STATUSES, AttendanceStatus::getLabel);
    }

    private void takeRoll() throws ErpException {
        Console.title("PASAR LISTA");
        Subject subject = Pickers.chooseSubject(input, academic);
        LocalDate date = readClassDate();
        Teacher teacher = askTeacher();
        List<Attendance> roster = attendance.getPendingRoster(subject.getSubjectId(), date);
        Console.info(roster.size() + " estudiante(s). Enter = Presente | 1 Presente, 2 Ausente, 3 Tardanza, "
                + "4 Excusa Médica | 'cancelar' no guarda nada.");
        Map<Attendance, AttendanceStatus> chosen = new LinkedHashMap<>();
        for (Attendance entry : roster) {
            chosen.put(entry, input.readValid(String.format("  %-40s [Presente]: ", entry.getStudentName()), text -> {
                if (text.isEmpty()) {
                    return AttendanceStatus.PRESENT;
                }
                try {
                    int option = Integer.parseInt(text);
                    if (option >= 1 && option <= STATUSES.size()) {
                        return STATUSES.get(option - 1);
                    }
                } catch (NumberFormatException e) {
                    // handled below: the same message is shown for any invalid entry
                }
                throw new ValidationException("Escriba 1, 2, 3, 4 o presione Enter para 'Presente'.");
            }));
        }
        int saved = attendance.registerRoll(subject.getSubjectId(), teacher.getTeacherId(), date, chosen);
        long absent = chosen.values().stream().filter(s -> s == AttendanceStatus.ABSENT).count();
        Console.success("Lista guardada: " + saved + " estudiante(s), " + absent + " ausente(s).");
    }

    private void saveAttendance(boolean correction) throws ErpException {
        Console.title(correction ? "CORREGIR UNA ASISTENCIA" : "REGISTRAR ASISTENCIA");
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName());
        Subject subject = Pickers.chooseSubject(input, academic);
        LocalDate date = readClassDate();
        Teacher teacher = askTeacher();
        AttendanceStatus status = readStatus();
        String observations = input.readValid("Observaciones (Enter para omitir): ",
                v -> Validator.optionalText(v, "Observaciones", 500));
        if (correction) {
            attendance.changeAttendance(student.getStudentId(), subject.getSubjectId(), teacher.getTeacherId(), date,
                    status, observations);
            Console.success("Asistencia corregida: " + status.getLabel() + ".");
        } else {
            attendance.registerAttendance(student.getStudentId(), subject.getSubjectId(), teacher.getTeacherId(), date,
                    status, observations);
            Console.success("Asistencia registrada: " + status.getLabel() + ".");
        }
    }

    private void studentAttendance() throws ErpException {
        Console.title("ASISTENCIA DE UN ESTUDIANTE");
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName());
        LocalDate to = input.readDate("Fecha final (dd/mm/aaaa, Enter = hoy): ", LocalDate.now());
        LocalDate from = input.readDate("Fecha inicial (dd/mm/aaaa, Enter = 30 días antes): ", to.minusDays(30));
        List<Attendance> records = attendance.getStudentAttendance(student.getStudentId(), from, to);
        if (records.isEmpty()) {
            Console.info("No hay asistencia registrada entre " + DateUtils.format(from) + " y " + DateUtils.format(to) + ".");
            return;
        }
        records.forEach(r -> System.out.printf("%-12s %-30s %-14s %s%n", DateUtils.format(r.getDate()), r.getSubjectName(),
                r.getStatus().getLabel(), r.getObservations() == null ? "" : r.getObservations()));
        Console.info("Resumen:");
        AttendanceController.countByStatus(records).forEach((status, count) ->
                Console.info("  " + String.format("%-14s", status.getLabel()) + count));
        Console.info("Asistencia efectiva: " + AttendanceController.attendancePercentage(records) + "% de "
                + records.size() + " clase(s).");
    }

    private void subjectAttendance() throws ErpException {
        Console.title("ASISTENCIA DE UNA MATERIA");
        Subject subject = Pickers.chooseSubject(input, academic);
        LocalDate date = readClassDate();
        List<Attendance> records = attendance.getSubjectAttendance(subject.getSubjectId(), date);
        if (records.isEmpty()) {
            Console.info("No hay asistencia registrada para esa materia el " + DateUtils.format(date) + ".");
            return;
        }
        records.forEach(r -> System.out.printf("%-5d %-40s %s%n", r.getStudentId(), r.getStudentName(),
                r.getStatus().getLabel()));
        Console.info("Resumen:");
        AttendanceController.countByStatus(records).forEach((status, count) ->
                Console.info("  " + String.format("%-14s", status.getLabel()) + count));
    }

    private void weeklyReport() throws ErpException {
        Console.title("REPORTE SEMANAL DE AUSENCIAS");
        LocalDate date = input.readDate("Cualquier fecha de la semana (dd/mm/aaaa, Enter = hoy): ", LocalDate.now());
        LocalDate monday = AttendanceController.weekStart(date);
        List<Map.Entry<String, Long>> report = attendance.getWeeklyAbsenceReport(date);
        Console.info("Semana del " + DateUtils.format(monday) + " al " + DateUtils.format(monday.plusDays(6))
                + " - estudiantes con más de " + AttendanceController.MAX_ABSENCES_PER_WEEK + " ausencias:");
        if (report.isEmpty()) {
            Console.info("  Ningún estudiante supera el límite de ausencias esa semana.");
        }
        report.forEach(entry -> System.out.printf("  %-50s %d ausencias%n", entry.getKey(), entry.getValue()));
    }
}