package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianAssignment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.DateUtils;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Console menu for the students and guardians module. It only talks to the user and to the controllers.
 */
public class StudentMenu extends BaseMenu {

    private final StudentController studentController;
    private final GuardianController guardianController;

    public StudentMenu(InputReader input, StudentController studentController, GuardianController guardianController) {
        super(input);
        this.studentController = studentController;
        this.guardianController = guardianController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("GESTIÓN DE ESTUDIANTES");
            Console.info("1. Registrar estudiante");
            Console.info("2. Listar estudiantes");
            Console.info("3. Buscar estudiante");
            Console.info("4. Ver ficha de un estudiante");
            Console.info("5. Actualizar datos de un estudiante");
            Console.info("6. Cambiar estado de un estudiante");
            Console.info("7. Agregar acudiente a un estudiante");
            Console.info("8. Consultar un acudiente y sus estudiantes");
            Console.info("0. Volver al menú principal");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 8)) {
                case 1:
                    runSafely(this::registerStudent);
                    break;
                case 2:
                    runSafely(this::listStudents);
                    break;
                case 3:
                    runSafely(this::searchStudents);
                    break;
                case 4:
                    runSafely(this::viewStudent);
                    break;
                case 5:
                    runSafely(this::updateStudent);
                    break;
                case 6:
                    runSafely(this::changeStudentStatus);
                    break;
                case 7:
                    runSafely(this::addGuardianToStudent);
                    break;
                case 8:
                    runSafely(this::queryGuardian);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    // ------------------------------------------------------------------ registration

    private void registerStudent() throws ErpException {
        Console.title("REGISTRO DE ESTUDIANTE");
        Console.info("(Escriba 'cancelar' en cualquier momento para volver al menú)");
        Student student = new Student();
        student.setFirstName(input.readValid("Primer nombre: ", v -> Validator.requireName(v, "Primer nombre")));
        student.setMiddleName(input.readValid("Segundo nombre (Enter para omitir): ",
                v -> Validator.optionalName(v, "Segundo nombre")));
        student.setFirstLastName(input.readValid("Primer apellido: ", v -> Validator.requireName(v, "Primer apellido")));
        student.setSecondLastName(input.readValid("Segundo apellido (Enter para omitir): ",
                v -> Validator.optionalName(v, "Segundo apellido")));
        student.setBirthDate(input.readValid("Fecha de nacimiento (dd/mm/aaaa): ", v -> {
            LocalDate date = DateUtils.parse(v);
            StudentController.validateBirthDate(date);
            return date;
        }));
        Console.info("Según su edad, el documento que corresponde es: "
                + Labels.documentName(StudentController.suggestedDocumentType(student.getBirthDate())));
        student.setDocumentType(askStudentDocumentType(student.getBirthDate()));
        student.setDocumentNumber(input.readValid("Número de documento: ", v -> {
            String number = Validator.requireDocumentNumber(v);
            studentController.ensureDocumentAvailable(number);
            return number;
        }));
        student.setGender(input.readChoice("Género:", Arrays.asList(Gender.values()), Labels::genderName));
        student.setAddress(input.readValid("Dirección (Enter para omitir): ",
                v -> Validator.optionalText(v, "Dirección", 255)));
        student.setPhone(input.readValid("Teléfono (Enter para omitir): ", v -> Validator.optionalPhone(v, "Teléfono")));
        student.setEmail(input.readValid("Correo electrónico (Enter para omitir): ", Validator::optionalEmail));
        student.setBloodType(input.readValid("Tipo de sangre, ej. O+ (Enter para omitir): ",
                Validator::optionalBloodType));
        student.setAllergies(input.readValid("Alergias (Enter para omitir): ",
                v -> Validator.optionalText(v, "Alergias", 1000)));
        student.setMedicalConditions(input.readValid("Condiciones médicas (Enter para omitir): ",
                v -> Validator.optionalText(v, "Condiciones médicas", 1000)));
        LocalDate birthDate = student.getBirthDate();
        student.setAdmissionDate(input.readValid("Fecha de ingreso (dd/mm/aaaa, Enter = hoy): ", v -> {
            if (v.isEmpty()) {
                return LocalDate.now();
            }
            LocalDate date = Validator.requirePastOrToday(DateUtils.parse(v), "Fecha de ingreso");
            if (date.isBefore(birthDate)) {
                throw new ValidationException("La fecha de ingreso no puede ser anterior a la fecha de nacimiento.");
            }
            return date;
        }));
        student.setStatus(StudentStatus.ACTIVE);

        Console.info("\nAcudientes: el estudiante debe tener al menos uno.");
        List<GuardianAssignment> assignments = new ArrayList<>();
        do {
            assignments.add(askGuardianAssignment(assignments.isEmpty(), assignments));
        } while (input.readYesNo("¿Desea agregar otro acudiente? (s/n): "));

        Console.title("RESUMEN DEL REGISTRO");
        Console.info("Estudiante: " + student.getFullName() + " (" + student.getDocumentType().getLabel() + " "
                + student.getDocumentNumber() + ", " + student.getAge() + " años)");
        assignments.forEach(a -> Console.info("Acudiente: " + a.getGuardian().getFullName() + " ("
                + a.getGuardian().getRelationship() + ")" + (a.isPrimary() ? " - principal" : "")));
        if (!input.readYesNo("¿Confirmar el registro? (s/n): ")) {
            Console.info("Registro cancelado. No se guardó nada.");
            return;
        }
        int id = studentController.registerStudent(student, assignments);
        Console.success("Estudiante registrado correctamente con el id " + id + ".");
    }

    private DocumentType askStudentDocumentType(LocalDate birthDate) throws ErpException {
        return input.readChoice("Tipo de documento:", Arrays.asList(DocumentType.values()), Labels::documentName,
                type -> StudentController.validateDocumentTypeForAge(type, birthDate));
    }

    // asks for a guardian by document: an existing one is reused, otherwise a new one is registered
    private GuardianAssignment askGuardianAssignment(boolean first, List<GuardianAssignment> alreadyLinked)
            throws ErpException {
        Guardian guardian = null;
        while (guardian == null) {
            String document = input.readValid("Documento del acudiente: ", v -> {
                String number = Validator.requireDocumentNumber(v);
                boolean repeated = alreadyLinked.stream()
                        .anyMatch(a -> number.equals(a.getGuardian().getDocumentNumber()));
                if (repeated) {
                    throw new BusinessRuleException("Ese acudiente ya está vinculado a este estudiante.");
                }
                return number;
            });
            Optional<Guardian> existing = guardianController.findByDocument(document);
            if (!existing.isPresent()) {
                Console.info("No existe un acudiente con ese documento. Se registrarán sus datos.");
                guardian = askNewGuardian(document);
            } else if (!existing.get().isActive()) {
                Console.error("Ese acudiente se encuentra inactivo y no se puede vincular.");
            } else {
                Console.info("Acudiente encontrado: " + existing.get().getFullName() + " ("
                        + existing.get().getRelationship() + ")");
                if (input.readYesNo("¿Es el acudiente correcto? (s/n): ")) {
                    guardian = existing.get();
                }
            }
        }
        boolean primary = first || input.readYesNo("¿Es el acudiente principal? (s/n): ");
        boolean emergency = input.readYesNo("¿Es contacto de emergencia? (s/n): ");
        return new GuardianAssignment(guardian, primary, emergency);
    }

    private Guardian askNewGuardian(String document) throws ErpException {
        Guardian guardian = new Guardian();
        guardian.setDocumentNumber(document);
        guardian.setDocumentType(input.readChoice("Tipo de documento del acudiente:", GuardianController.DOCUMENT_TYPES,
                Labels::documentName));
        guardian.setFirstName(input.readValid("Primer nombre: ", v -> Validator.requireName(v, "Primer nombre")));
        guardian.setMiddleName(input.readValid("Segundo nombre (Enter para omitir): ",
                v -> Validator.optionalName(v, "Segundo nombre")));
        guardian.setFirstLastName(input.readValid("Primer apellido: ", v -> Validator.requireName(v, "Primer apellido")));
        guardian.setSecondLastName(input.readValid("Segundo apellido (Enter para omitir): ",
                v -> Validator.optionalName(v, "Segundo apellido")));
        guardian.setRelationship(input.readChoice("Parentesco:", GuardianController.RELATIONSHIPS, r -> r));
        guardian.setPrimaryPhone(input.readValid("Teléfono principal: ", v -> Validator.requirePhone(v, "Teléfono principal")));
        guardian.setSecondaryPhone(input.readValid("Teléfono secundario (Enter para omitir): ",
                v -> Validator.optionalPhone(v, "Teléfono secundario")));
        guardian.setEmail(input.readValid("Correo electrónico (Enter para omitir): ", Validator::optionalEmail));
        guardian.setAddress(input.readValid("Dirección (Enter para omitir): ",
                v -> Validator.optionalText(v, "Dirección", 255)));
        guardian.setOccupation(input.readValid("Ocupación (Enter para omitir): ",
                v -> Validator.optionalText(v, "Ocupación", 100)));
        guardian.setFinanciallyResponsible(input.readYesNo("¿Es responsable financiero (paga la pensión)? (s/n): "));
        return guardian;
    }

    // ------------------------------------------------------------------ queries

    private void listStudents() throws ErpException {
        Console.title("LISTADO DE ESTUDIANTES");
        Console.info("1. Todos");
        Console.info("2. Por estado");
        if (input.readMenuOption("Seleccione una opción: ", 1, 2) == 1) {
            printStudentTable(studentController.listAll());
        } else {
            StudentStatus status = input.readChoice("Estado:", Arrays.asList(StudentStatus.values()),
                    StudentStatus::getLabel);
            printStudentTable(studentController.listByStatus(status));
        }
    }

    private void searchStudents() throws ErpException {
        Console.title("BUSCAR ESTUDIANTE");
        String text = input.readValid("Nombre o documento a buscar: ", v -> Validator.requireText(v, "Texto de búsqueda", 100));
        printStudentTable(studentController.search(text));
    }

    private void viewStudent() throws ErpException {
        Console.title("FICHA DEL ESTUDIANTE");
        Student student = studentController.getById(askId("ID del estudiante: "));
        printStudentDetail(student);
    }

    private void queryGuardian() throws ErpException {
        Console.title("CONSULTA DE ACUDIENTE");
        String document = input.readValid("Documento del acudiente: ", Validator::requireDocumentNumber);
        Optional<Guardian> found = guardianController.findByDocument(document);
        if (!found.isPresent()) {
            Console.info("No existe un acudiente con el documento " + document + ".");
            return;
        }
        Guardian g = found.get();
        Console.info("Acudiente: " + g.getFullName() + " (id " + g.getGuardianId() + ")");
        Console.info("Documento: " + g.getDocumentType().getLabel() + " " + g.getDocumentNumber());
        Console.info("Parentesco: " + g.getRelationship() + (g.isActive() ? "" : " - INACTIVO"));
        Console.info("Teléfonos: " + g.getPrimaryPhone() + (g.getSecondaryPhone() == null ? "" : " / " + g.getSecondaryPhone()));
        Console.info("Correo: " + orDash(g.getEmail()) + " | Ocupación: " + orDash(g.getOccupation()));
        Console.info("Responsable financiero: " + (g.isFinanciallyResponsible() ? "Sí" : "No"));
        Console.info("Estudiantes a su cargo:");
        printStudentTable(guardianController.getStudentsOf(g.getGuardianId()));
    }

    // ------------------------------------------------------------------ changes

    private void updateStudent() throws ErpException {
        Console.title("ACTUALIZAR ESTUDIANTE");
        Console.info("(Enter mantiene el valor actual; '-' borra un dato opcional; 'cancelar' vuelve al menú)");
        Student student = studentController.getById(askId("ID del estudiante: "));
        Console.info("Editando a: " + student.getFullName());
        student.setFirstName(askKeep("Primer nombre", student.getFirstName(), v -> Validator.requireName(v, "Primer nombre"), false));
        student.setMiddleName(askKeep("Segundo nombre", student.getMiddleName(), v -> Validator.optionalName(v, "Segundo nombre"), true));
        student.setFirstLastName(askKeep("Primer apellido", student.getFirstLastName(), v -> Validator.requireName(v, "Primer apellido"), false));
        student.setSecondLastName(askKeep("Segundo apellido", student.getSecondLastName(), v -> Validator.optionalName(v, "Segundo apellido"), true));
        LocalDate newBirthDate = input.readValid("Fecha de nacimiento [" + DateUtils.format(student.getBirthDate()) + "]: ", v -> {
            if (v.isEmpty()) {
                return student.getBirthDate();
            }
            LocalDate date = DateUtils.parse(v);
            StudentController.validateBirthDate(date);
            return date;
        });
        student.setBirthDate(newBirthDate);
        if (input.readYesNo("¿Desea corregir el tipo y número de documento? (s/n): ")) {
            student.setDocumentType(askStudentDocumentType(student.getBirthDate()));
            student.setDocumentNumber(input.readValid("Número de documento: ", v -> {
                String number = Validator.requireDocumentNumber(v);
                studentController.ensureDocumentAvailable(number, student.getStudentId());
                return number;
            }));
        }
        student.setAddress(askKeep("Dirección", student.getAddress(), v -> Validator.optionalText(v, "Dirección", 255), true));
        student.setPhone(askKeep("Teléfono", student.getPhone(), v -> Validator.optionalPhone(v, "Teléfono"), true));
        student.setEmail(askKeep("Correo electrónico", student.getEmail(), Validator::optionalEmail, true));
        student.setBloodType(askKeep("Tipo de sangre", student.getBloodType(), Validator::optionalBloodType, true));
        student.setAllergies(askKeep("Alergias", student.getAllergies(), v -> Validator.optionalText(v, "Alergias", 1000), true));
        student.setMedicalConditions(askKeep("Condiciones médicas", student.getMedicalConditions(),
                v -> Validator.optionalText(v, "Condiciones médicas", 1000), true));
        studentController.updateStudent(student);
        Console.success("Datos del estudiante actualizados correctamente.");
    }

    private void changeStudentStatus() throws ErpException {
        Console.title("CAMBIAR ESTADO DE UN ESTUDIANTE");
        Student student = studentController.getById(askId("ID del estudiante: "));
        Console.info(student.getFullName() + " - estado actual: " + student.getStatus().getLabel());
        StudentStatus newStatus = input.readChoice("Nuevo estado:", Arrays.asList(StudentStatus.values()),
                StudentStatus::getLabel);
        if (!input.readYesNo("¿Confirmar el cambio a '" + newStatus.getLabel() + "'? (s/n): ")) {
            Console.info("Cambio cancelado.");
            return;
        }
        studentController.changeStatus(student.getStudentId(), newStatus);
        Console.success("Estado actualizado a " + newStatus.getLabel() + ".");
    }

    private void addGuardianToStudent() throws ErpException {
        Console.title("AGREGAR ACUDIENTE A UN ESTUDIANTE");
        Student student = studentController.getById(askId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName());
        List<GuardianAssignment> current = studentController.getGuardiansOf(student.getStudentId());
        GuardianAssignment assignment = askGuardianAssignment(current.isEmpty(), current);
        guardianController.linkToStudent(student.getStudentId(), assignment);
        Console.success("Acudiente " + assignment.getGuardian().getFullName() + " vinculado correctamente.");
    }

    // ------------------------------------------------------------------ helpers

    private int askId(String prompt) throws ErpException {
        return input.readValid(prompt, v -> {
            try {
                int id = Integer.parseInt(v);
                if (id >= 1) {
                    return id;
                }
            } catch (NumberFormatException e) {
                // handled below: the same message is shown for any invalid entry
            }
            throw new ValidationException("Ingrese un número entero positivo.");
        });
    }

    // asks for a text value showing the current one; Enter keeps it and '-' clears an optional value
    private String askKeep(String label, String current, InputReader.Parser<String> parser, boolean optional)
            throws ErpException {
        return input.readValid(label + " [" + orDash(current) + "]: ", v -> {
            if (v.isEmpty()) {
                return current;
            }
            if (optional && v.equals("-")) {
                return null;
            }
            return parser.parse(v);
        });
    }

    private static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private void printStudentTable(List<Student> students) {
        if (students.isEmpty()) {
            Console.info("No se encontraron estudiantes.");
            return;
        }
        System.out.printf("%-5s %-5s %-13s %-42s %-5s %-10s%n", "ID", "Tipo", "Documento", "Nombre completo", "Edad", "Estado");
        students.forEach(s -> System.out.printf("%-5d %-5s %-13s %-42s %-5d %-10s%n", s.getStudentId(),
                s.getDocumentType().getLabel(), s.getDocumentNumber(), s.getFullName(), s.getAge(),
                s.getStatus().getLabel()));
        Console.info("Total: " + students.size() + " estudiante(s).");
    }

    private void printStudentDetail(Student s) throws ErpException {
        Console.info("Estudiante: " + s.getFullName() + " (id " + s.getStudentId() + ")");
        Console.info("Documento: " + s.getDocumentType().getLabel() + " " + s.getDocumentNumber());
        Console.info("Nacimiento: " + DateUtils.format(s.getBirthDate()) + " (" + s.getAge() + " años) | Género: "
                + Labels.genderName(s.getGender()));
        Console.info("Estado: " + s.getStatus().getLabel() + " | Ingreso: " + DateUtils.format(s.getAdmissionDate()));
        Console.info("Dirección: " + orDash(s.getAddress()) + " | Teléfono: " + orDash(s.getPhone()));
        Console.info("Correo: " + orDash(s.getEmail()) + " | Tipo de sangre: " + orDash(s.getBloodType()));
        Console.info("Alergias: " + orDash(s.getAllergies()));
        Console.info("Condiciones médicas: " + orDash(s.getMedicalConditions()));
        Console.info("Acudientes:");
        List<GuardianAssignment> guardians = studentController.getGuardiansOf(s.getStudentId());
        if (guardians.isEmpty()) {
            Console.info("  (sin acudientes registrados)");
        }
        guardians.forEach(a -> Console.info("  - " + a.getGuardian().getFullName() + " (" + a.getGuardian().getRelationship()
                + ") Tel: " + a.getGuardian().getPrimaryPhone() + (a.isPrimary() ? " | PRINCIPAL" : "")
                + (a.isEmergencyContact() ? " | Emergencia" : "")));
    }
}
