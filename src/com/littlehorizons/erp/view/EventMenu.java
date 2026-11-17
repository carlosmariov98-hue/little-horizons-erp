package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.EventPosition;
import com.littlehorizons.erp.model.enums.EventType;
import com.littlehorizons.erp.model.enums.ParticipationRole;
import com.littlehorizons.erp.util.DateUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Console menu for the school events, their participants and the results of the competitions. */
public class EventMenu extends BaseMenu {

    private final EventController events;
    private final StudentController students;
    private final TeacherController teachers;

    public EventMenu(InputReader input, EventController events, StudentController students, TeacherController teachers) {
        super(input);
        this.events = events;
        this.students = students;
        this.teachers = teachers;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("EVENTOS");
            Console.info("1. Listar eventos");
            Console.info("2. Crear un evento");
            Console.info("3. Editar un evento");
            Console.info("4. Activar o desactivar un evento");
            Console.info("5. Registrar un participante");
            Console.info("6. Retirar un participante");
            Console.info("7. Asignar la posición de un competidor");
            Console.info("8. Ver los participantes de un evento");
            Console.info("9. Ver el podio de un evento");
            Console.info("10. Ver los eventos de un estudiante");
            Console.info("11. Ranking de estudiantes por podios");
            Console.info("12. Cantidad de eventos por tipo");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 12)) {
                case 1:
                    runSafely(this::listEvents);
                    break;
                case 2:
                    runSafely(this::createEvent);
                    break;
                case 3:
                    runSafely(this::editEvent);
                    break;
                case 4:
                    runSafely(this::toggleActive);
                    break;
                case 5:
                    runSafely(this::registerParticipant);
                    break;
                case 6:
                    runSafely(this::removeParticipant);
                    break;
                case 7:
                    runSafely(this::assignPosition);
                    break;
                case 8:
                    runSafely(this::showParticipants);
                    break;
                case 9:
                    runSafely(this::showPodium);
                    break;
                case 10:
                    runSafely(this::studentEvents);
                    break;
                case 11:
                    runSafely(this::podiumRanking);
                    break;
                case 12:
                    runSafely(this::eventsByType);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private void listEvents() throws ErpException {
        List<SchoolEvent> all = events.listEvents();
        Console.title("EVENTOS (" + all.size() + ")");
        Console.info(String.format("%3s | %-10s | %-40s | %-11s | %-5s | %-5s | %s", "ID", "Fecha", "Evento", "Tipo",
                "Comp.", "Part.", "Estado"));
        for (SchoolEvent e : all) {
            Console.info(String.format("%3d | %-10s | %-40s | %-11s | %-5s | %5d | %s", e.getEventId(),
                    DateUtils.format(e.getDate()), e.getName().length() > 40 ? e.getName().substring(0, 37) + "..."
                            : e.getName(), e.getType().getLabel(), e.hasCompetition() ? "Sí" : "No",
                    e.getParticipantCount(), e.isActive() ? "Activo" : "Inactivo"));
        }
        Console.info("Total de participaciones: " + EventController.totalParticipants(all));
    }

    private SchoolEvent chooseEvent() throws ErpException {
        SchoolEvent event = events.getEvent(readId("ID del evento: "));
        Console.info("Evento: " + event.getName() + " (" + DateUtils.format(event.getDate()) + ")");
        return event;
    }

    private Student askStudent() throws ErpException {
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName());
        return student;
    }

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

    private void createEvent() throws ErpException {
        Console.title("CREAR UN EVENTO");
        SchoolEvent event = new SchoolEvent();
        event.setName(input.readValid("Nombre del evento: ", text -> text));
        event.setDescription(input.readLine("Descripción (Enter = ninguna): "));
        event.setType(input.readChoice("Tipo de evento:", Arrays.asList(EventType.values()), EventType::getLabel));
        event.setDate(input.readDate("Fecha del evento (dd/MM/yyyy): ", null));
        event.setPlace(input.readLine("Lugar (Enter = ninguno): "));
        event.setCompetition(input.readYesNo("¿El evento tiene competencia con posiciones? (s/n): "));
        event.setOrganizerId(readOptionalTeacher("ID del profesor organizador", 0));
        int id = events.createEvent(event);
        Console.success("Evento creado con ID " + id + ".");
    }

    private void editEvent() throws ErpException {
        SchoolEvent event = chooseEvent();
        Console.info("Enter conserva el valor actual, '-' borra un dato opcional.");
        event.setName(readKeepText("Nombre", event.getName(), text -> text, false));
        event.setDescription(readKeepText("Descripción", event.getDescription(), text -> text, true));
        event.setPlace(readKeepText("Lugar", event.getPlace(), text -> text, true));
        event.setDate(input.readValid("Fecha [" + DateUtils.format(event.getDate()) + "]: ",
                text -> text.isEmpty() ? event.getDate() : DateUtils.parse(text)));
        event.setCompetition(input.readYesNo("¿Tiene competencia? (actual: " + (event.hasCompetition() ? "sí" : "no")
                + ") (s/n): "));
        event.setOrganizerId(readOptionalTeacher("ID del profesor organizador", event.getOrganizerId()));
        events.updateEvent(event);
        Console.success("Evento actualizado.");
    }

    private void toggleActive() throws ErpException {
        SchoolEvent event = chooseEvent();
        boolean target = !event.isActive();
        if (input.readYesNo((target ? "¿Activar" : "¿Desactivar") + " este evento? (s/n): ")) {
            events.changeActive(event.getEventId(), target);
            Console.success("Evento " + (target ? "activado" : "desactivado") + ".");
        } else {
            Console.info("No se realizó ningún cambio.");
        }
    }

    private void registerParticipant() throws ErpException {
        Console.title("REGISTRAR UN PARTICIPANTE");
        SchoolEvent event = chooseEvent();
        Student student = askStudent();
        ParticipationRole role = input.readChoice("Rol:", Arrays.asList(ParticipationRole.values()),
                ParticipationRole::getLabel);
        String notes = input.readLine("Observaciones (Enter = ninguna): ");
        events.registerParticipant(event.getEventId(), student.getStudentId(), role, notes);
        Console.success(student.getFullName() + " quedó registrado en " + event.getName() + " como "
                + role.getLabel() + ".");
    }

    private EventParticipant chooseParticipant(SchoolEvent event) throws ErpException {
        List<EventParticipant> list = events.getParticipants(event.getEventId());
        if (list.isEmpty()) {
            throw new BusinessRuleException("El evento no tiene participantes.");
        }
        return input.readChoice("Participante:", list, p -> p.getStudentName() + " - " + p.getRole().getLabel()
                + (p.getPosition() == null ? "" : " (" + p.getPosition().getLabel() + ")"));
    }

    private void removeParticipant() throws ErpException {
        SchoolEvent event = chooseEvent();
        EventParticipant participant = chooseParticipant(event);
        if (input.readYesNo("¿Retirar a " + participant.getStudentName() + " del evento? (s/n): ")) {
            events.removeParticipant(participant.getParticipationId());
            Console.success("Participante retirado.");
        } else {
            Console.info("No se realizó ningún cambio.");
        }
    }

    private void assignPosition() throws ErpException {
        Console.title("ASIGNAR LA POSICIÓN DE UN COMPETIDOR");
        SchoolEvent event = chooseEvent();
        EventParticipant participant = chooseParticipant(event);
        EventPosition position = input.readChoice("Posición:", Arrays.asList(EventPosition.values()),
                EventPosition::getLabel);
        events.assignPosition(participant.getParticipationId(), position);
        Console.success(participant.getStudentName() + ": " + position.getLabel() + ".");
    }

    private void showParticipants() throws ErpException {
        SchoolEvent event = chooseEvent();
        List<EventParticipant> list = events.getParticipants(event.getEventId());
        Console.title("PARTICIPANTES (" + list.size() + ")");
        if (list.isEmpty()) {
            Console.info("El evento no tiene participantes.");
            return;
        }
        for (EventParticipant p : list) {
            Console.info(String.format("%4d | %-36s | %-12s | %s", p.getStudentId(), p.getStudentName(),
                    p.getRole().getLabel(), p.getPosition() == null ? "-" : p.getPosition().getLabel()));
        }
    }

    private void showPodium() throws ErpException {
        SchoolEvent event = chooseEvent();
        List<EventParticipant> podium = events.getPodium(event.getEventId());
        Console.title("PODIO");
        if (podium.isEmpty()) {
            Console.info("Aún no hay posiciones asignadas.");
            return;
        }
        podium.forEach(p -> Console.info(String.format("%-10s %s", p.getPosition().getLabel(), p.getStudentName())));
    }

    private void studentEvents() throws ErpException {
        Student student = askStudent();
        List<EventParticipant> list = events.getEventsOfStudent(student.getStudentId());
        if (list.isEmpty()) {
            Console.info("El estudiante no ha participado en ningún evento.");
            return;
        }
        list.forEach(p -> Console.info(String.format("%-44s | %-12s | %s", p.getEventName(), p.getRole().getLabel(),
                p.getPosition() == null ? "-" : p.getPosition().getLabel())));
    }

    private void podiumRanking() throws ErpException {
        Map<String, Long> ranking = events.getPodiumRanking();
        Console.title("RANKING DE PODIOS (1er, 2do y 3er lugar)");
        if (ranking.isEmpty()) {
            Console.info("Todavía no hay podios registrados.");
            return;
        }
        int[] place = {1};
        ranking.forEach((name, count) -> Console.info(String.format("%2d. %-36s %d podio(s)", place[0]++, name, count)));
    }

    private void eventsByType() throws ErpException {
        Console.title("EVENTOS POR TIPO");
        events.getEventsByType().forEach((type, count) -> Console.info(String.format("%-12s %d", type, count)));
    }
}