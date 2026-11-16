package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.DateUtils;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Console menu for the teachers module. */
public class TeacherMenu extends BaseMenu {

    private final TeacherController teacherController;
    private final AcademicController academicController;

    public TeacherMenu(InputReader input, TeacherController teacherController, AcademicController academicController) {
        super(input);
        this.teacherController = teacherController;
        this.academicController = academicController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("GESTIÓN DE PROFESORES");
            Console.info("1. Registrar profesor");
            Console.info("2. Listar profesores");
            Console.info("3. Buscar profesor");
            Console.info("4. Ver ficha y carga académica de un profesor");
            Console.info("5. Actualizar datos de un profesor");
            Console.info("6. Cambiar estado de un profesor");
            Console.info("7. Asignar una materia a un profesor");
            Console.info("8. Ver los profesores de una materia");
            Console.info("0. Volver al menú principal");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 8)) {
                case 1:
                    runSafely(this::registerTeacher);
                    break;
                case 2:
                    runSafely(this::listTeachers);
                    break;
                case 3:
                    runSafely(this::searchTeachers);
                    break;
                case 4:
                    runSafely(this::viewTeacher);
                    break;
                case 5:
                    runSafely(this::updateTeacher);
                    break;
                case 6:
                    runSafely(this::changeStatus);
                    break;
                case 7:
                    runSafely(this::assignSubject);
                    break;
                case 8:
                    runSafely(this::viewTeachersOfSubject);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private void registerTeacher() throws ErpException {
        Console.title("REGISTRO DE PROFESOR");
        Console.info("(Escriba 'cancelar' en cualquier momento para volver al menú)");
        Teacher teacher = new Teacher();
        teacher.setDocumentType(input.readChoice("Tipo de documento:", GuardianController.DOCUMENT_TYPES,
                Labels::documentName));
        teacher.setDocumentNumber(input.readValid("Número de documento: ", v -> {
            String number = Validator.requireDocumentNumber(v);
            teacherController.ensureDocumentAvailable(number, 0);
            return number;
        }));
        teacher.setFirstName(input.readValid("Primer nombre: ", v -> Validator.requireName(v, "Primer nombre")));
        teacher.setMiddleName(input.readValid("Segundo nombre (Enter para omitir): ",
                v -> Validator.optionalName(v, "Segundo nombre")));
        teacher.setFirstLastName(input.readValid("Primer apellido: ", v -> Validator.requireName(v, "Primer apellido")));
        teacher.setSecondLastName(input.readValid("Segundo apellido (Enter para omitir): ",
                v -> Validator.optionalName(v, "Segundo apellido")));
        teacher.setEmail(input.readValid("Correo electrónico: ", v -> {
            String email = Validator.requireEmail(v);
            teacherController.ensureEmailAvailable(email, 0);
            return email;
        }));
        teacher.setPhone(input.readValid("Teléfono (Enter para omitir): ", v -> Validator.optionalPhone(v, "Teléfono")));
        teacher.setSpecialization(input.readValid("Especialización (ej. Matemáticas): ",
                v -> Validator.requireText(v, "Especialización", 100)));
        teacher.setHireDate(input.readValid("Fecha de contratación (dd/mm/aaaa, Enter = hoy): ", v ->
                v.isEmpty() ? LocalDate.now() : Validator.requirePastOrToday(DateUtils.parse(v), "Fecha de contratación")));
        teacher.setStatus(TeacherStatus.ACTIVE);
        int id = teacherController.registerTeacher(teacher);
        Console.success("Profesor registrado correctamente con el id " + id + ".");
    }

    private void listTeachers() throws ErpException {
        Console.title("LISTADO DE PROFESORES");
        Console.info("1. Todos");
        Console.info("2. Por estado");
        if (input.readMenuOption("Seleccione una opción: ", 1, 2) == 1) {
            printTable(teacherController.listAll());
        } else {
            printTable(teacherController.listByStatus(input.readChoice("Estado:",
                    Arrays.asList(TeacherStatus.values()), TeacherStatus::getLabel)));
        }
    }

    private void searchTeachers() throws ErpException {
        Console.title("BUSCAR PROFESOR");
        String text = input.readValid("Nombre, documento, correo o especialización: ",
                v -> Validator.requireText(v, "Texto de búsqueda", 100));
        printTable(teacherController.search(text));
    }

    private void viewTeacher() throws ErpException {
        Console.title("FICHA DEL PROFESOR");
        Teacher t = teacherController.getById(readId("ID del profesor: "));
        Console.info("Profesor: " + t.getFullName() + " (id " + t.getTeacherId() + ")");
        Console.info("Documento: " + t.getDocumentType().getLabel() + " " + t.getDocumentNumber());
        Console.info("Correo: " + t.getEmail() + " | Teléfono: " + dash(t.getPhone()));
        Console.info("Especialización: " + t.getSpecialization() + " | Contratado: " + DateUtils.format(t.getHireDate())
                + " | Estado: " + t.getStatus().getLabel());
        int year = readYear();
        List<TeachingAssignment> assignments = teacherController.getAssignments(t.getTeacherId(), year);
        Console.info("Carga académica " + year + ":");
        if (assignments.isEmpty()) {
            Console.info("  (sin materias asignadas)");
        }
        assignments.forEach(a -> Console.info("  - " + a.getSubjectName() + " (" + a.getGradeName() + ", sección "
                + a.getSection() + ")"));
    }

    private void updateTeacher() throws ErpException {
        Console.title("ACTUALIZAR PROFESOR");
        Console.info("(Enter mantiene el valor actual; '-' borra un dato opcional; 'cancelar' vuelve al menú)");
        Teacher t = teacherController.getById(readId("ID del profesor: "));
        Console.info("Editando a: " + t.getFullName());
        t.setFirstName(readKeepText("Primer nombre", t.getFirstName(), v -> Validator.requireName(v, "Primer nombre"), false));
        t.setMiddleName(readKeepText("Segundo nombre", t.getMiddleName(), v -> Validator.optionalName(v, "Segundo nombre"), true));
        t.setFirstLastName(readKeepText("Primer apellido", t.getFirstLastName(), v -> Validator.requireName(v, "Primer apellido"), false));
        t.setSecondLastName(readKeepText("Segundo apellido", t.getSecondLastName(), v -> Validator.optionalName(v, "Segundo apellido"), true));
        t.setEmail(readKeepText("Correo electrónico", t.getEmail(), v -> {
            String email = Validator.requireEmail(v);
            teacherController.ensureEmailAvailable(email, t.getTeacherId());
            return email;
        }, false));
        t.setPhone(readKeepText("Teléfono", t.getPhone(), v -> Validator.optionalPhone(v, "Teléfono"), true));
        t.setSpecialization(readKeepText("Especialización", t.getSpecialization(),
                v -> Validator.requireText(v, "Especialización", 100), false));
        teacherController.updateTeacher(t);
        Console.success("Datos del profesor actualizados correctamente.");
    }

    private void changeStatus() throws ErpException {
        Console.title("CAMBIAR ESTADO DE UN PROFESOR");
        Teacher t = teacherController.getById(readId("ID del profesor: "));
        Console.info(t.getFullName() + " - estado actual: " + t.getStatus().getLabel());
        TeacherStatus status = input.readChoice("Nuevo estado:", Arrays.asList(TeacherStatus.values()),
                TeacherStatus::getLabel);
        if (!input.readYesNo("¿Confirmar el cambio a '" + status.getLabel() + "'? (s/n): ")) {
            Console.info("Cambio cancelado.");
            return;
        }
        teacherController.changeStatus(t.getTeacherId(), status);
        Console.success("Estado actualizado a " + status.getLabel() + ".");
    }

    private void assignSubject() throws ErpException {
        Console.title("ASIGNAR MATERIA A UN PROFESOR");
        Teacher t = teacherController.getById(readId("ID del profesor: "));
        Console.info("Profesor: " + t.getFullName() + " (" + t.getStatus().getLabel() + ")");
        Subject subject = Pickers.chooseSubject(input, academicController);
        int year = readYear();
        String section = input.readValid("Sección (A-Z): ", v -> Validator.requireText(v, "Sección", 1));
        teacherController.assignSubject(t.getTeacherId(), subject.getSubjectId(), year, section);
        Console.success(subject.getName() + " asignada a " + t.getFullName() + " (" + year + ", sección "
                + section.toUpperCase() + ").");
    }

    private void viewTeachersOfSubject() throws ErpException {
        Console.title("PROFESORES DE UNA MATERIA");
        Subject subject = Pickers.chooseSubject(input, academicController);
        int year = readYear();
        List<TeachingAssignment> assignments = teacherController.getTeachersOfSubject(subject.getSubjectId(), year);
        if (assignments.isEmpty()) {
            Console.info("Ningún profesor tiene asignada esa materia en " + year + ".");
        }
        assignments.forEach(a -> Console.info("  - Sección " + a.getSection() + ": " + a.getTeacherName()));
    }

    private void printTable(List<Teacher> teachers) {
        if (teachers.isEmpty()) {
            Console.info("No se encontraron profesores.");
            return;
        }
        System.out.printf("%-5s %-5s %-13s %-38s %-24s %-9s%n", "ID", "Tipo", "Documento", "Nombre completo",
                "Especialización", "Estado");
        teachers.forEach(t -> System.out.printf("%-5d %-5s %-13s %-38s %-24s %-9s%n", t.getTeacherId(),
                t.getDocumentType().getLabel(), t.getDocumentNumber(), t.getFullName(), t.getSpecialization(),
                t.getStatus().getLabel()));
        Console.info("Total: " + teachers.size() + " profesor(es).");
    }
}