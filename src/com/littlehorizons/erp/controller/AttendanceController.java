package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for daily attendance: taking attendance, corrections and the attendance reports.
 * Only the status "Ausente" counts as an absence; a justified medical excuse or a late arrival does not.
 */
public class AttendanceController {

    /** A student with more absences than this in one week appears in the weekly report. */
    public static final int MAX_ABSENCES_PER_WEEK = 3;

    private static final int MAX_OBSERVATIONS = 500;
    private static final int FIRST_YEAR = 2000;

    private final AttendanceDao attendanceDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final TeachingRules teachingRules;

    public AttendanceController(AttendanceDao attendanceDao, StudentDao studentDao, SubjectDao subjectDao,
                                SubjectEnrollmentDao subjectEnrollmentDao, TeachingRules teachingRules) {
        this.attendanceDao = attendanceDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.teachingRules = teachingRules;
    }

    /**
     * Validates the date of a class: not in the future, not a weekend, not before the year 2000.
     *
     * @param date the date to check
     * @throws ValidationException with a user-friendly message when the date is not valid
     */
    public static void validateClassDate(LocalDate date) throws ValidationException {
        Validator.requirePastOrToday(date, "Fecha de la clase");
        if (date.getYear() < FIRST_YEAR) {
            throw new ValidationException("La fecha de la clase es demasiado antigua.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ValidationException("No hay clases los fines de semana; elija un día de lunes a viernes.");
        }
    }

    private Subject requireActiveSubject(int subjectId) throws ErpException {
        Subject subject = subjectDao.findById(subjectId);
        if (!subject.isActive()) {
            throw new BusinessRuleException("La materia " + subject.getName() + " no está activa.");
        }
        return subject;
    }

    private Attendance build(int studentId, int subjectId, int teacherId, LocalDate date, AttendanceStatus status,
                             String observations) throws ErpException {
        Attendance attendance = new Attendance();
        attendance.setStudentId(studentId);
        attendance.setSubjectId(subjectId);
        attendance.setDate(date);
        attendance.setStatus(Validator.requireNotNull(status, "Estado de asistencia"));
        attendance.setObservations(Validator.optionalText(observations, "Observaciones", MAX_OBSERVATIONS));
        attendance.setRegisteredBy(teacherId);
        return attendance;
    }

    /**
     * Registers the attendance of one student in one subject on one day.
     *
     * @param studentId    id of the student
     * @param subjectId    id of the subject
     * @param teacherId    id of the teacher who takes attendance
     * @param date         day of the class
     * @param status       attendance status
     * @param observations optional comments
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public void registerAttendance(int studentId, int subjectId, int teacherId, LocalDate date,
                                   AttendanceStatus status, String observations) throws ErpException {
        validateClassDate(date);
        Attendance attendance = build(studentId, subjectId, teacherId, date, status, observations);
        studentDao.findById(studentId);
        requireActiveSubject(subjectId);
        teachingRules.requireTeacherOfSubject(teacherId, subjectId, date.getYear());
        boolean registered = subjectEnrollmentDao.findByStudent(studentId, date.getYear()).stream()
                .anyMatch(i -> i.getSubjectId() == subjectId && i.getStatus() == EnrollmentStatus.ACTIVE);
        if (!registered) {
            throw new BusinessRuleException("El estudiante no tiene una inscripción activa en esa materia en "
                    + date.getYear() + ".");
        }
        attendanceDao.insert(attendance);
    }

    /**
     * Changes the status of an attendance already registered.
     *
     * @throws ErpException if the attendance does not exist, the status is the same, or the database fails
     */
    public void changeAttendance(int studentId, int subjectId, int teacherId, LocalDate date, AttendanceStatus status,
                                 String observations) throws ErpException {
        validateClassDate(date);
        Attendance newData = build(studentId, subjectId, teacherId, date, status, observations);
        teachingRules.requireTeacherOfSubject(teacherId, subjectId, date.getYear());
        Attendance current = attendanceDao.find(studentId, subjectId, date).orElseThrow(
                () -> new EntityNotFoundException("No hay asistencia registrada para ese estudiante, materia y fecha."));
        if (current.getStatus() == status) {
            throw new BusinessRuleException("La asistencia ya está registrada como " + status.getLabel() + ".");
        }
        attendanceDao.updateStatus(current.getAttendanceId(), status, newData.getObservations(), teacherId);
    }

    /**
     * Students that still need attendance in a subject on a day (to take attendance for the whole class).
     *
     * @throws ErpException if the date is invalid, the subject is not active, everyone already has attendance,
     *                      or the database fails
     */
    public List<Attendance> getPendingRoster(int subjectId, LocalDate date) throws ErpException {
        validateClassDate(date);
        requireActiveSubject(subjectId);
        List<Attendance> roster = attendanceDao.findPendingRoster(subjectId, date.getYear(), date);
        if (roster.isEmpty()) {
            throw new BusinessRuleException("No hay estudiantes pendientes: no hay inscritos activos o ya se tomó "
                    + "la asistencia de esa materia en esa fecha.");
        }
        return roster;
    }

    /**
     * Saves the attendance of a whole class in one transaction (all the students or none).
     *
     * @param statuses attendance chosen for each student of the roster
     * @return the number of students saved
     * @throws ErpException if a rule is broken or the database fails (nothing is saved)
     */
    public int registerRoll(int subjectId, int teacherId, LocalDate date, Map<Attendance, AttendanceStatus> statuses)
            throws ErpException {
        validateClassDate(date);
        requireActiveSubject(subjectId);
        teachingRules.requireTeacherOfSubject(teacherId, subjectId, date.getYear());
        if (statuses.isEmpty()) {
            throw new ValidationException("No hay estudiantes para registrar.");
        }
        List<Attendance> toSave = new java.util.ArrayList<>();
        for (Map.Entry<Attendance, AttendanceStatus> entry : statuses.entrySet()) {
            toSave.add(build(entry.getKey().getStudentId(), subjectId, teacherId, date, entry.getValue(), null));
        }
        return attendanceDao.insertAll(toSave);
    }

    public List<Attendance> getSubjectAttendance(int subjectId, LocalDate date) throws ErpException {
        validateClassDate(date);
        return attendanceDao.findBySubjectAndDate(subjectId, date);
    }

    public List<Attendance> getStudentAttendance(int studentId, LocalDate from, LocalDate to) throws ErpException {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ValidationException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        studentDao.findById(studentId);
        return attendanceDao.findByStudent(studentId, from, to);
    }

    // ---------- statistics (Streams) ----------

    /**
     * Counts how many times each status appears in a list of attendance records.
     *
     * @param records the attendance records
     * @return a count for every status (zero when it does not appear), in the order of the enum
     */
    public static Map<AttendanceStatus, Long> countByStatus(List<Attendance> records) {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status, 0L);
        }
        counts.putAll(records.stream().collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting())));
        return counts;
    }

    /**
     * Percentage of classes attended: every record that is not an absence counts as attended.
     *
     * @param records the attendance records
     * @return a value from 0 to 100 with one decimal, or 100 when there are no records
     */
    public static double attendancePercentage(List<Attendance> records) {
        if (records.isEmpty()) {
            return 100.0;
        }
        long attended = records.stream().filter(r -> r.getStatus() != AttendanceStatus.ABSENT).count();
        return Math.round(attended * 1000.0 / records.size()) / 10.0;
    }

    /** Monday of the week that contains the given date. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Report of the students with more than {@value #MAX_ABSENCES_PER_WEEK} absences in the week (Monday to Sunday)
     * that contains the given date, counting all their subjects. Ordered from the most absences to the fewest.
     *
     * @param dateInWeek any date of the week
     * @return pairs (student name, number of absences)
     * @throws ErpException if the database fails
     */
    public List<Map.Entry<String, Long>> getWeeklyAbsenceReport(LocalDate dateInWeek) throws ErpException {
        Validator.requireNotNull(dateInWeek, "Fecha");
        LocalDate monday = weekStart(dateInWeek);
        return attendanceDao.findByDateRange(monday, monday.plusDays(6)).stream()
                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                .collect(Collectors.groupingBy(r -> r.getStudentName() + " (id " + r.getStudentId() + ")",
                        Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > MAX_ABSENCES_PER_WEEK)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toList());
    }
}