package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.EventPosition;
import com.littlehorizons.erp.model.enums.ParticipationRole;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for the school events: the events, the participation of the students, the positions of
 * the competitions and the statistics (podium ranking and events by type).
 */
public class EventController {

    private static final int FIRST_YEAR = 2000;

    private final EventDao eventDao;
    private final EventParticipantDao participantDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public EventController(EventDao eventDao, EventParticipantDao participantDao, StudentDao studentDao,
                           TeacherDao teacherDao) {
        this.eventDao = eventDao;
        this.participantDao = participantDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    private void validateEvent(SchoolEvent event) throws ErpException {
        event.setName(Validator.requireText(event.getName(), "Nombre del evento", 150));
        event.setDescription(Validator.optionalText(event.getDescription(), "Descripción", 500));
        event.setPlace(Validator.optionalText(event.getPlace(), "Lugar", 150));
        Validator.requireNotNull(event.getType(), "Tipo de evento");
        Validator.requireNotNull(event.getDate(), "Fecha del evento");
        Validator.requireIntInRange(event.getDate().getYear(), FIRST_YEAR, LocalDate.now().getYear() + 5,
                "Año del evento");
        if (event.getOrganizerId() != 0) {
            Teacher organizer = teacherDao.findById(event.getOrganizerId());
            if (organizer.getStatus() != TeacherStatus.ACTIVE) {
                throw new BusinessRuleException("El profesor " + organizer.getFullName()
                        + " no está activo y no puede organizar un evento.");
            }
        }
    }

    /**
     * Creates an event. An event without organizer has organizerId 0.
     *
     * @return the generated id
     * @throws ErpException if the data is invalid or the database fails
     */
    public int createEvent(SchoolEvent event) throws ErpException {
        validateEvent(event);
        event.setActive(true);
        return eventDao.insert(event);
    }

    /**
     * Updates an event. The competition flag cannot be removed while competitors have a position.
     *
     * @throws ErpException if the data is invalid, the event does not exist, or the database fails
     */
    public void updateEvent(SchoolEvent event) throws ErpException {
        validateEvent(event);
        SchoolEvent current = eventDao.findById(event.getEventId());
        if (current.hasCompetition() && !event.hasCompetition()
                && participantDao.findByEvent(event.getEventId()).stream().anyMatch(p -> p.getPosition() != null)) {
            throw new BusinessRuleException("El evento ya tiene posiciones asignadas: no se puede quitar la competencia.");
        }
        eventDao.update(event);
    }

    /**
     * Activates or deactivates an event. An inactive event does not accept new participants.
     *
     * @throws ErpException if the event does not exist, already has that status, or the database fails
     */
    public void changeActive(int eventId, boolean active) throws ErpException {
        SchoolEvent event = eventDao.findById(eventId);
        if (event.isActive() == active) {
            throw new BusinessRuleException("El evento ya se encuentra " + (active ? "activo" : "inactivo") + ".");
        }
        eventDao.changeActive(eventId, active);
    }

    /**
     * Registers a student in an event. Only events with competition accept competitors.
     *
     * @return the id of the participation
     * @throws ErpException if a rule is broken or the database fails
     */
    public int registerParticipant(int eventId, int studentId, ParticipationRole role, String notes)
            throws ErpException {
        Validator.requireNotNull(role, "Rol de participación");
        String cleanNotes = Validator.optionalText(notes, "Observaciones", 300);
        SchoolEvent event = eventDao.findById(eventId);
        if (!event.isActive()) {
            throw new BusinessRuleException("El evento está inactivo y no acepta participantes.");
        }
        if (role == ParticipationRole.COMPETITOR && !event.hasCompetition()) {
            throw new BusinessRuleException("Este evento no tiene competencia: no puede haber competidores.");
        }
        Student student = studentDao.findById(studentId);
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Solo los estudiantes activos pueden participar en un evento "
                    + "(estado actual: " + student.getStatus().getLabel() + ").");
        }
        EventParticipant participant = new EventParticipant();
        participant.setEventId(eventId);
        participant.setStudentId(studentId);
        participant.setRole(role);
        participant.setNotes(cleanNotes);
        return participantDao.insert(participant);
    }

    /**
     * Removes a participation. A competitor who already has a position cannot be removed.
     *
     * @throws ErpException if the participation does not exist, already has a result, or the database fails
     */
    public void removeParticipant(int participationId) throws ErpException {
        EventParticipant participant = participantDao.findById(participationId);
        if (participant.getPosition() != null) {
            throw new BusinessRuleException("El estudiante ya tiene un resultado en el evento y no se puede retirar.");
        }
        participantDao.delete(participationId);
    }

    /**
     * Gives a position to a competitor. It needs an event with competition that has already taken place, and each
     * podium place (1st, 2nd, 3rd) can be given to only one competitor.
     *
     * @throws ErpException if a rule is broken or the database fails
     */
    public void assignPosition(int participationId, EventPosition position) throws ErpException {
        Validator.requireNotNull(position, "Posición");
        EventParticipant participant = participantDao.findById(participationId);
        SchoolEvent event = eventDao.findById(participant.getEventId());
        if (!event.hasCompetition()) {
            throw new BusinessRuleException("Solo los eventos con competencia tienen posiciones.");
        }
        if (participant.getRole() != ParticipationRole.COMPETITOR) {
            throw new BusinessRuleException("Solo un competidor puede recibir una posición "
                    + "(rol actual: " + participant.getRole().getLabel() + ").");
        }
        if (event.getDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("El evento es el " + event.getDate()
                    + ": las posiciones se asignan cuando el evento ya se realizó.");
        }
        participantDao.assignPosition(participationId, position);
    }

    public SchoolEvent getEvent(int eventId) throws DatabaseException, EntityNotFoundException {
        return eventDao.findById(eventId);
    }

    public List<SchoolEvent> listEvents() throws DatabaseException {
        return eventDao.findAll();
    }

    public List<SchoolEvent> listActiveEvents() throws DatabaseException {
        return eventDao.findActive();
    }

    public List<EventParticipant> getParticipants(int eventId) throws ErpException {
        eventDao.findById(eventId);
        return participantDao.findByEvent(eventId);
    }

    /** Competitors of an event that have a podium place, from the 1st to the 3rd. */
    public List<EventParticipant> getPodium(int eventId) throws ErpException {
        SchoolEvent event = eventDao.findById(eventId);
        if (!event.hasCompetition()) {
            throw new BusinessRuleException("Este evento no tiene competencia.");
        }
        return participantDao.findByEvent(eventId).stream()
                .filter(p -> p.getPosition() != null && p.getPosition().isPodium())
                .sorted(Comparator.comparing(EventParticipant::getPosition))
                .collect(Collectors.toList());
    }

    public List<EventParticipant> getEventsOfStudent(int studentId) throws ErpException {
        studentDao.findById(studentId);
        return participantDao.findByStudent(studentId);
    }

    // ---------- statistics (Streams) ----------

    /** Number of podium places (1st, 2nd and 3rd) won by each student, from the most to the fewest. */
    public Map<String, Long> getPodiumRanking() throws DatabaseException {
        Map<String, Long> counts = participantDao.findAllWithPosition().stream()
                .filter(p -> p.getPosition().isPodium())
                .collect(Collectors.groupingBy(EventParticipant::getStudentName, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                        java.util.LinkedHashMap::new));
    }

    /** Number of events of each type, ordered by the name of the type. */
    public Map<String, Long> getEventsByType() throws DatabaseException {
        return eventDao.findAll().stream().collect(Collectors.groupingBy(e -> e.getType().getLabel(), TreeMap::new,
                Collectors.counting()));
    }

    /** Total number of participations in a list of events. */
    public static int totalParticipants(List<SchoolEvent> events) {
        return events.stream().mapToInt(SchoolEvent::getParticipantCount).sum();
    }
}