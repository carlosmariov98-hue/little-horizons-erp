package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.DateUtils;

import java.time.LocalDate;
import java.util.List;

/** Console menu for the extracurricular clubs. */
public class ClubMenu extends BaseMenu {

    private final ClubController clubs;
    private final StudentController students;
    private final TeacherController teachers;

    public ClubMenu(InputReader input, ClubController clubs, StudentController students, TeacherController teachers) {
        super(input);
        this.clubs = clubs;
        this.students = students;
        this.teachers = teachers;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("CLUBES EXTRACURRICULARES");
            Console.info("1. Listar clubes");
            Console.info("2. Crear un club");
            Console.info("3. Editar un club");
            Console.info("4. Activar o desactivar un club");
            Console.info("5. Inscribir un estudiante en un club");
            Console.info("6. Retirar un estudiante de un club");
            Console.info("7. Ver los miembros de un club");
            Console.info("8. Ver los clubes de un estudiante");
            Console.info("9. Reporte de ocupación de los clubes");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 9)) {
                case 1:
                    runSafely(this::listClubs);
                    break;
                case 2:
                    runSafely(this::createClub);
                    break;
                case 3:
                    runSafely(this::editClub);
                    break;
                case 4:
                    runSafely(this::toggleActive);
                    break;
                case 5:
                    runSafely(this::joinClub);
                    break;
                case 6:
                    runSafely(this::withdraw);
                    break;
                case 7:
                    runSafely(this::showMembers);
                    break;
                case 8:
                    runSafely(this::studentClubs);
                    break;
                case 9:
                    runSafely(this::occupancyReport);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private static String clubLine(Club club) {
        return String.format("%3d | %-36s | %-30s | %2d/%-2d | %-24s | %s", club.getClubId(), club.getName(),
                dash(club.getSupervisorName()), club.getActiveMembers(), club.getMaxCapacity(),
                dash(club.getSchedule()), club.isActive() ? "Activo" : "Inactivo");
    }

    private void listClubs() throws ErpException {
        List<Club> all = clubs.listClubs();
        Console.title("CLUBES (" + all.size() + ")");
        Console.info(String.format("%3s | %-36s | %-30s | %-5s | %-24s | %s", "ID", "Club", "Supervisor", "Cupos",
                "Horario", "Estado"));
        all.forEach(club -> Console.info(clubLine(club)));
    }

    private Club chooseClub() throws ErpException {
        return clubs.getClub(readId("ID del club: "));
    }

    private Student askStudent() throws ErpException {
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName());
        return student;
    }

    // asks for a teacher id; Enter = no teacher (0). The teacher found is shown so the user can confirm it
    private int readOptionalTeacher(String label, int current) throws ErpException {
        String hint = current == 0 ? "Enter = ninguno" : "Enter = conservar, '-' = quitar";
        return input.readValid(label + " (" + hint + "): ", text -> {
            if (text.isEmpty()) {
                return current;
            }
            if (text.equals("-")) {
                return 0;
            }
            try {
                int id = Integer.parseInt(text);
                Teacher teacher = teachers.getById(id);
                Console.info("Profesor: " + teacher.getFullName());
                return id;
            } catch (NumberFormatException e) {
                throw new ValidationException("Ingrese el número de identificación del profesor.");
            } catch (EntityNotFoundException e) {
                throw new ValidationException(e.getMessage());
            }
        });
    }

    private int readCapacity(int current) throws ErpException {
        return input.readValid("Capacidad máxima" + (current > 0 ? " [" + current + "]" : "") + ": ", text -> {
            if (text.isEmpty() && current > 0) {
                return current;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new ValidationException("Ingrese un número entero, por ejemplo 20.");
            }
        });
    }

    private void createClub() throws ErpException {
        Console.title("CREAR UN CLUB");
        Club club = new Club();
        club.setName(input.readValid("Nombre del club: ", text -> text));
        club.setDescription(input.readLine("Descripción (Enter = ninguna): "));
        club.setSupervisorId(readOptionalTeacher("ID del profesor supervisor", 0));
        club.setMaxCapacity(readCapacity(0));
        club.setSchedule(input.readLine("Horario de reunión, ejemplo 'Lunes 3:00 - 5:00 PM' (Enter = ninguno): "));
        int id = clubs.createClub(club);
        Console.success("Club creado con ID " + id + ".");
    }

    private void editClub() throws ErpException {
        Club club = chooseClub();
        Console.title("EDITAR: " + club.getName() + " (Enter conserva el valor actual, '-' borra un dato opcional)");
        club.setName(readKeepText("Nombre", club.getName(), text -> text, false));
        club.setDescription(readKeepText("Descripción", club.getDescription(), text -> text, true));
        club.setSupervisorId(readOptionalTeacher("ID del profesor supervisor", club.getSupervisorId()));
        club.setMaxCapacity(readCapacity(club.getMaxCapacity()));
        club.setSchedule(readKeepText("Horario", club.getSchedule(), text -> text, true));
        clubs.updateClub(club);
        Console.success("Club actualizado.");
    }

    private void toggleActive() throws ErpException {
        Club club = chooseClub();
        boolean target = !club.isActive();
        if (input.readYesNo((target ? "¿Activar" : "¿Desactivar") + " el club " + club.getName() + "? (s/n): ")) {
            clubs.changeActive(club.getClubId(), target);
            Console.success("Club " + (target ? "activado" : "desactivado") + ".");
        } else {
            Console.info("No se realizó ningún cambio.");
        }
    }

    private void joinClub() throws ErpException {
        Console.title("INSCRIBIR UN ESTUDIANTE EN UN CLUB");
        Student student = askStudent();
        Club club = input.readChoice("Club:", clubs.listActiveClubs(),
                c -> c.getName() + " (cupos libres: " + c.getAvailableSlots() + ")");
        LocalDate date = input.readDate("Fecha de ingreso (dd/MM/yyyy, Enter = hoy): ", LocalDate.now());
        clubs.joinClub(student.getStudentId(), club.getClubId(), date);
        Console.success(student.getFullName() + " quedó inscrito en " + club.getName() + ".");
    }

    private void withdraw() throws ErpException {
        Console.title("RETIRAR UN ESTUDIANTE DE UN CLUB");
        Student student = askStudent();
        Club club = chooseClub();
        ClubMembership membership = clubs.getActiveMembership(student.getStudentId(), club.getClubId());
        String reason = input.readValid("Motivo del retiro: ", text -> text);
        LocalDate date = input.readDate("Fecha de retiro (dd/MM/yyyy, Enter = hoy): ", LocalDate.now());
        if (input.readYesNo("¿Retirar a " + student.getFullName() + " de " + club.getName() + "? (s/n): ")) {
            clubs.withdraw(membership.getMembershipId(), date, reason);
            Console.success("Estudiante retirado del club.");
        } else {
            Console.info("No se realizó ningún cambio.");
        }
    }

    private void showMembers() throws ErpException {
        Club club = chooseClub();
        List<ClubMembership> members = clubs.getMembers(club.getClubId());
        Console.title("MIEMBROS DE " + club.getName().toUpperCase() + " (" + club.getActiveMembers() + "/"
                + club.getMaxCapacity() + " activos)");
        if (members.isEmpty()) {
            Console.info("El club no tiene miembros.");
            return;
        }
        for (ClubMembership m : members) {
            Console.info(String.format("%4d | %-36s | %-8s | ingreso %s%s", m.getStudentId(), m.getStudentName(),
                    m.getStatus().getLabel(), DateUtils.format(m.getJoinDate()),
                    m.getLeaveDate() == null ? "" : " | retiro " + DateUtils.format(m.getLeaveDate()) + " ("
                            + dash(m.getLeaveReason()) + ")"));
        }
    }

    private void studentClubs() throws ErpException {
        Student student = askStudent();
        List<ClubMembership> list = clubs.getClubsOfStudent(student.getStudentId());
        if (list.isEmpty()) {
            Console.info("El estudiante no ha participado en ningún club.");
            return;
        }
        for (ClubMembership m : list) {
            Console.info(String.format("%-36s | %-8s | ingreso %s%s", m.getClubName(), m.getStatus().getLabel(),
                    DateUtils.format(m.getJoinDate()),
                    m.getLeaveDate() == null ? "" : " | retiro " + DateUtils.format(m.getLeaveDate())));
        }
    }

    private void occupancyReport() throws ErpException {
        List<Club> report = clubs.getOccupancyReport();
        Console.title("OCUPACIÓN DE LOS CLUBES ACTIVOS");
        for (Club club : report) {
            int percent = ClubController.occupancyPercent(club);
            Console.info(String.format("%-36s %2d/%-2d  %3d%%  %s", club.getName(), club.getActiveMembers(),
                    club.getMaxCapacity(), percent, club.getAvailableSlots() == 0 ? "LLENO" : ""));
        }
        Console.info("Total de estudiantes inscritos: " + ClubController.totalMembers(report));
    }
}