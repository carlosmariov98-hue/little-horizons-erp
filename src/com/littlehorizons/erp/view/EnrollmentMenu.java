package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.util.DateUtils;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Console menu for grade enrollment (matrícula) and registration in subjects (inscripción). */
public class EnrollmentMenu extends BaseMenu {

    private final AcademicController academic;
    private final StudentController students;

    public EnrollmentMenu(InputReader input, AcademicController academic, StudentController students) {
        super(input);
        this.academic = academic;
        this.students = students;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("MATRÍCULAS E INSCRIPCIONES");
            Console.info("1. Ver las materias de un grado");
            Console.info("2. Matricular un estudiante en un grado");
            Console.info("3. Ver las matrículas de un estudiante");
            Console.info("4. Cambiar el estado de una matrícula");
            Console.info("5. Inscribir un estudiante en todas las materias de su grado");
            Console.info("6. Inscribir un estudiante en una materia");
            Console.info("7. Ver las inscripciones de un estudiante");
            Console.info("8. Ver los estudiantes de un curso");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 8)) {
                case 1:
                    runSafely(this::viewSubjectsOfGrade);
                    break;
                case 2:
                    runSafely(this::enrollInGrade);
                    break;
                case 3:
                    runSafely(this::viewEnrollments);
                    break;
                case 4:
                    runSafely(this::changeEnrollmentStatus);
                    break;
                case 5:
                    runSafely(this::enrollInAllSubjects);
                    break;
                case 6:
                    runSafely(this::enrollInOneSubject);
                    break;
                case 7:
                    runSafely(this::viewSubjectEnrollments);
                    break;
                case 8:
                    runSafely(this::viewCourse);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private Student askStudent() throws ErpException {
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName() + " (" + student.getStatus().getLabel() + ")");
        return student;
    }

    private void viewSubjectsOfGrade() throws ErpException {
        Console.title("MATERIAS DE UN GRADO");
        Grade grade = Pickers.chooseGrade(input, academic);
        List<Subject> subjects = academic.listSubjectsOfGrade(grade.getGradeId());
        System.out.printf("%-4s %-10s %-34s %-6s %-7s%n", "ID", "Código", "Materia", "Horas", "Activa");
        subjects.forEach(s -> System.out.printf("%-4d %-10s %-34s %-6d %-7s%n", s.getSubjectId(), s.getCode(),
                s.getName(), s.getWeeklyHours(), s.isActive() ? "Sí" : "No"));
        Console.info("Total: " + subjects.size() + " materia(s) en " + grade.getName() + ".");
    }

    private void enrollInGrade() throws ErpException {
        Console.title("MATRICULAR UN ESTUDIANTE");
        Student student = askStudent();
        Grade grade = Pickers.chooseGrade(input, academic);
        int year = readYear();
        LocalDate date = input.readDate("Fecha de matrícula (dd/mm/aaaa, Enter = hoy): ", LocalDate.now());
        academic.enrollStudentInGrade(student.getStudentId(), grade.getGradeId(), year, date);
        Console.success(student.getFullName() + " matriculado en " + grade.getName() + " para " + year + ".");
    }

    private void viewEnrollments() throws ErpException {
        Console.title("MATRÍCULAS DE UN ESTUDIANTE");
        Student student = askStudent();
        List<Enrollment> enrollments = academic.getEnrollments(student.getStudentId());
        if (enrollments.isEmpty()) {
            Console.info("El estudiante no tiene matrículas registradas.");
        }
        enrollments.forEach(e -> Console.info("  " + e.getYear() + " | " + e.getGradeName() + " | "
                + e.getStatus().getLabel() + " | matriculado el " + DateUtils.format(e.getEnrollmentDate())
                + (e.getWithdrawalDate() == null ? "" : " | retirado el " + DateUtils.format(e.getWithdrawalDate()))));
    }

    private void changeEnrollmentStatus() throws ErpException {
        Console.title("CAMBIAR ESTADO DE UNA MATRÍCULA");
        Student student = askStudent();
        int year = readYear();
        EnrollmentStatus status = input.readChoice("Nuevo estado:", Arrays.asList(EnrollmentStatus.values()),
                EnrollmentStatus::getLabel);
        if (!input.readYesNo("¿Confirmar el cambio a '" + status.getLabel() + "'? (s/n): ")) {
            Console.info("Cambio cancelado.");
            return;
        }
        academic.changeEnrollmentStatus(student.getStudentId(), year, status);
        Console.success("Matrícula " + year + " actualizada a " + status.getLabel() + ".");
    }

    private void enrollInAllSubjects() throws ErpException {
        Console.title("INSCRIPCIÓN EN LAS MATERIAS DEL GRADO");
        Student student = askStudent();
        int year = readYear();
        AcademicPeriod period = readPeriod();
        String section = input.readValid("Sección (A-Z): ", v -> Validator.requireText(v, "Sección", 1));
        int count = academic.enrollInAllSubjects(student.getStudentId(), year, period, section);
        Console.success("Estudiante inscrito en " + count + " materia(s) para el período " + period.getLabel() + " de "
                + year + ".");
    }

    private void enrollInOneSubject() throws ErpException {
        Console.title("INSCRIPCIÓN EN UNA MATERIA");
        Student student = askStudent();
        Subject subject = Pickers.chooseSubject(input, academic);
        int year = readYear();
        AcademicPeriod period = readPeriod();
        String section = input.readValid("Sección (A-Z): ", v -> Validator.requireText(v, "Sección", 1));
        academic.enrollInSubject(student.getStudentId(), subject.getSubjectId(), year, period, section);
        Console.success("Estudiante inscrito en " + subject.getName() + " (período " + period.getLabel() + " de " + year + ").");
    }

    private void viewSubjectEnrollments() throws ErpException {
        Console.title("INSCRIPCIONES DE UN ESTUDIANTE");
        Student student = askStudent();
        int year = readYear();
        List<SubjectEnrollment> list = academic.getSubjectEnrollments(student.getStudentId(), year);
        if (list.isEmpty()) {
            Console.info("El estudiante no tiene inscripciones en " + year + ".");
        }
        list.forEach(i -> Console.info("  Período " + i.getPeriod().getLabel() + " | " + i.getSubjectName() + " | sección "
                + i.getSection() + " | " + i.getStatus().getLabel()));
    }

    private void viewCourse() throws ErpException {
        Console.title("ESTUDIANTES DE UN CURSO");
        Grade grade = Pickers.chooseGrade(input, academic);
        int year = readYear();
        List<Student> course = academic.getCourse(grade.getGradeId(), year);
        if (course.isEmpty()) {
            Console.info("No hay estudiantes con matrícula activa en " + grade.getName() + " para " + year + ".");
            return;
        }
        course.forEach(s -> System.out.printf("%-5d %-13s %-42s %-3d años%n", s.getStudentId(), s.getDocumentNumber(),
                s.getFullName(), s.getAge()));
        Console.info("Total: " + course.size() + " estudiante(s) en " + grade.getName() + " " + year + ".");
    }
}