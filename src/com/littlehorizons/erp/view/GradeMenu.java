package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.GradeController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.PerformanceLevel;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Console menu for grades (calificaciones) and averages (promedios). */
public class GradeMenu extends BaseMenu {

    private static final int RANKING_SIZE = 10;

    private final GradeController grades;
    private final AcademicController academic;
    private final StudentController students;
    private final TeacherController teachers;

    public GradeMenu(InputReader input, GradeController grades, AcademicController academic,
                     StudentController students, TeacherController teachers) {
        super(input);
        this.grades = grades;
        this.academic = academic;
        this.students = students;
        this.teachers = teachers;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("CALIFICACIONES Y PROMEDIOS");
            Console.info("1. Registrar la nota de un estudiante");
            Console.info("2. Corregir una nota");
            Console.info("3. Calcular la nota Final de una materia");
            Console.info("4. Boletín de un estudiante (notas y promedios del año)");
            Console.info("5. Historial académico de un estudiante");
            Console.info("6. Notas de una materia por período");
            Console.info("7. Promedio del curso por materia");
            Console.info("8. Ranking de un curso");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 8)) {
                case 1:
                    runSafely(() -> saveGrade(false));
                    break;
                case 2:
                    runSafely(() -> saveGrade(true));
                    break;
                case 3:
                    runSafely(this::calculateFinal);
                    break;
                case 4:
                    runSafely(this::reportCard);
                    break;
                case 5:
                    runSafely(this::history);
                    break;
                case 6:
                    runSafely(this::subjectGrades);
                    break;
                case 7:
                    runSafely(this::courseAverages);
                    break;
                case 8:
                    runSafely(this::ranking);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private Student askStudent() throws ErpException {
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName());
        return student;
    }

    private BigDecimal readScore() throws ErpException {
        return input.readValid("Nota (0.0 a 5.0, use punto decimal): ", text -> {
            try {
                return GradeController.validateScore(new BigDecimal(text));
            } catch (NumberFormatException e) {
                throw new ValidationException("Ingrese un número válido, por ejemplo 4.2.");
            }
        });
    }

    private void saveGrade(boolean correction) throws ErpException {
        Console.title(correction ? "CORREGIR UNA NOTA" : "REGISTRAR UNA NOTA");
        Student student = askStudent();
        Subject subject = Pickers.chooseSubject(input, academic);
        int year = readYear();
        AcademicPeriod period = readPeriod();
        Teacher teacher = teachers.getById(readId("ID del profesor que califica: "));
        Console.info("Profesor: " + teacher.getFullName());
        BigDecimal score = readScore();
        String observations = input.readValid("Observaciones (Enter para omitir): ",
                v -> Validator.optionalText(v, "Observaciones", 500));
        if (correction) {
            grades.correctGrade(student.getStudentId(), subject.getSubjectId(), teacher.getTeacherId(), year, period,
                    score, observations);
            Console.success("Nota corregida: " + subject.getName() + ", período " + period.getLabel() + " = " + score + ".");
        } else {
            grades.registerGrade(student.getStudentId(), subject.getSubjectId(), teacher.getTeacherId(), year, period,
                    score, observations);
            Console.success("Nota registrada: " + subject.getName() + ", período " + period.getLabel() + " = " + score + ".");
        }
    }

    private void calculateFinal() throws ErpException {
        Console.title("NOTA FINAL DE UNA MATERIA");
        Student student = askStudent();
        Subject subject = Pickers.chooseSubject(input, academic);
        int year = readYear();
        BigDecimal result = grades.calculateFinalGrade(student.getStudentId(), subject.getSubjectId(), year);
        Console.success("Nota Final de " + subject.getName() + ": " + result + " ("
                + PerformanceLevel.fromScore(result).getLabel() + ").");
    }

    private void reportCard() throws ErpException {
        Console.title("BOLETÍN DEL ESTUDIANTE");
        Student student = askStudent();
        int year = readYear();
        List<AcademicRecord> records = grades.getStudentGrades(student.getStudentId(), year);
        if (records.isEmpty()) {
            Console.info("El estudiante no tiene notas registradas en " + year + ".");
            return;
        }
        printRecords(records);
        Console.info("");
        Console.info("Promedio general por período:");
        grades.getAveragesByPeriod(student.getStudentId(), year).forEach((period, average) ->
                Console.info("  Período " + period.getLabel() + ": " + average + " - "
                        + PerformanceLevel.fromScore(average).getLabel()));
        Console.info("Promedio por materia:");
        grades.getAveragesBySubject(student.getStudentId(), year).forEach((subject, average) ->
                Console.info("  " + String.format("%-32s", subject) + average + " - "
                        + PerformanceLevel.fromScore(average).getLabel()));
        List<String> failing = grades.getFailingSubjects(student.getStudentId(), year);
        Console.info(failing.isEmpty() ? "Sin materias en nivel Bajo."
                : "Materias en nivel Bajo (promedio menor a 3.0): " + String.join(", ", failing));
    }

    private void history() throws ErpException {
        Console.title("HISTORIAL ACADÉMICO");
        Student student = askStudent();
        Map<Integer, List<AcademicRecord>> history = grades.getAcademicHistory(student.getStudentId());
        if (history.isEmpty()) {
            Console.info("El estudiante no tiene notas registradas.");
            return;
        }
        history.forEach((year, records) -> {
            Console.info("--- Año " + year + " ---");
            printRecords(records);
            BigDecimal average = GradeController.average(records.stream()
                    .filter(r -> r.getPeriod().isRegular()).collect(java.util.stream.Collectors.toList()), 2);
            Console.info("Promedio del año: " + average + " (" + PerformanceLevel.fromScore(average).getLabel() + ")");
        });
    }

    private void subjectGrades() throws ErpException {
        Console.title("NOTAS DE UNA MATERIA");
        Subject subject = Pickers.chooseSubject(input, academic);
        int year = readYear();
        AcademicPeriod period = readPeriod();
        List<AcademicRecord> records = grades.getSubjectGrades(subject.getSubjectId(), year, period);
        if (records.isEmpty()) {
            Console.info("No hay notas registradas para esa materia en el período " + period.getLabel() + " de " + year + ".");
            return;
        }
        records.forEach(r -> System.out.printf("%-5d %-40s %-5s %-9s%n", r.getStudentId(), r.getStudentName(),
                r.getScore(), PerformanceLevel.fromScore(r.getScore()).getLabel()));
        Console.info("Promedio de la materia: " + GradeController.average(records, 2) + " (" + records.size()
                + " estudiante(s)).");
    }

    private void courseAverages() throws ErpException {
        Console.title("PROMEDIO DEL CURSO POR MATERIA");
        Grade grade = Pickers.chooseGrade(input, academic);
        int year = readYear();
        AcademicPeriod period = readPeriod();
        Map<String, BigDecimal> averages = grades.getCourseAveragesBySubject(grade.getGradeId(), year, period);
        if (averages.isEmpty()) {
            Console.info("No hay notas de " + grade.getName() + " en el período " + period.getLabel() + " de " + year + ".");
            return;
        }
        averages.forEach((subject, average) -> System.out.printf("%-34s %-5s %-9s%n", subject, average,
                PerformanceLevel.fromScore(average).getLabel()));
    }

    private void ranking() throws ErpException {
        Console.title("RANKING DE UN CURSO");
        Grade grade = Pickers.chooseGrade(input, academic);
        int year = readYear();
        AcademicPeriod period = readPeriod();
        List<Map.Entry<String, BigDecimal>> ranking = grades.getRanking(grade.getGradeId(), year, period, RANKING_SIZE);
        if (ranking.isEmpty()) {
            Console.info("No hay notas de " + grade.getName() + " en el período " + period.getLabel() + " de " + year + ".");
            return;
        }
        Console.info("Mejores promedios de " + grade.getName() + " - período " + period.getLabel() + " de " + year + ":");
        for (int i = 0; i < ranking.size(); i++) {
            System.out.printf("%2d. %-40s %s%n", i + 1, ranking.get(i).getKey(), ranking.get(i).getValue());
        }
    }

    private void printRecords(List<AcademicRecord> records) {
        System.out.printf("%-7s %-32s %-5s %-9s%n", "Período", "Materia", "Nota", "Nivel");
        records.forEach(r -> System.out.printf("%-7s %-32s %-5s %-9s%n", r.getPeriod().getLabel(), r.getSubjectName(),
                r.getScore(), PerformanceLevel.fromScore(r.getScore()).getLabel()));
    }
}