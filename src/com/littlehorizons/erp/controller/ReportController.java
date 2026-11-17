package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.PerformanceLevel;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.MoneyFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Reports and statistics of the whole school. It only reads data: every number is calculated with Streams
 * from the lists that the DAOs and the other controllers already provide.
 */
public class ReportController {

    /** Minimum score to pass a subject (Colombian scale 0.0 - 5.0, basic level). */
    public static final BigDecimal PASSING_SCORE = new BigDecimal("3.0");

    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final GradeDao gradeDao;
    private final EnrollmentDao enrollmentDao;
    private final AcademicRecordDao academicRecordDao;
    private final AttendanceDao attendanceDao;
    private final ClubDao clubDao;
    private final PaymentController paymentController;
    private final ResourceController resourceController;
    private final LoanController loanController;

    public ReportController(StudentDao studentDao, TeacherDao teacherDao, GradeDao gradeDao,
                            EnrollmentDao enrollmentDao, AcademicRecordDao academicRecordDao,
                            AttendanceDao attendanceDao, ClubDao clubDao, PaymentController paymentController,
                            ResourceController resourceController, LoanController loanController) {
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.gradeDao = gradeDao;
        this.enrollmentDao = enrollmentDao;
        this.academicRecordDao = academicRecordDao;
        this.attendanceDao = attendanceDao;
        this.clubDao = clubDao;
        this.paymentController = paymentController;
        this.resourceController = resourceController;
        this.loanController = loanController;
    }

    public List<Grade> listGrades() throws DatabaseException {
        return gradeDao.findAll().stream().sorted(Comparator.comparingInt(Grade::getLevel))
                .collect(Collectors.toList());
    }

    // ---------- students ----------

    /** Number of students in each status (all the statuses appear, even with zero). */
    public Map<StudentStatus, Long> studentsByStatus() throws DatabaseException {
        Map<StudentStatus, Long> counts = new EnumMap<>(StudentStatus.class);
        for (StudentStatus status : StudentStatus.values()) {
            counts.put(status, 0L);
        }
        studentDao.findAll().stream().collect(Collectors.groupingBy(Student::getStatus, Collectors.counting()))
                .forEach(counts::put);
        return counts;
    }

    /** Number of active students of each gender. */
    public Map<Gender, Long> activeStudentsByGender() throws DatabaseException {
        return studentDao.findByStatus(StudentStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(Student::getGender, () -> new EnumMap<>(Gender.class),
                        Collectors.counting()));
    }

    /** Number of students enrolled in each grade in a year, from the lowest grade to the highest. */
    public Map<String, Long> studentsPerGrade(int year) throws DatabaseException {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Grade grade : listGrades()) {
            result.put(grade.getName(), (long) enrollmentDao.findStudentsByGradeAndYear(grade.getGradeId(), year).size());
        }
        return result;
    }

    // ---------- academic performance ----------

    private static BigDecimal averageOf(List<AcademicRecord> records) {
        BigDecimal total = records.stream().map(AcademicRecord::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
    }

    /** Average score of each subject of a grade in a period, ordered by subject name. */
    public Map<String, BigDecimal> subjectAverages(int gradeId, int year, AcademicPeriod period)
            throws DatabaseException {
        return academicRecordDao.findByGrade(gradeId, year, period).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getSubjectName, TreeMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), ReportController::averageOf)));
    }

    /** Average score of every student of a grade in a period, from the best to the lowest. */
    public List<StudentAverage> studentAverages(int gradeId, int year, AcademicPeriod period)
            throws DatabaseException {
        return academicRecordDao.findByGrade(gradeId, year, period).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getStudentName,
                        Collectors.collectingAndThen(Collectors.toList(), ReportController::averageOf)))
                .entrySet().stream().map(entry -> new StudentAverage(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(StudentAverage::getAverage).reversed()
                        .thenComparing(StudentAverage::getStudentName))
                .collect(Collectors.toList());
    }

    /** The first students of an already sorted list of averages. */
    public static List<StudentAverage> top(List<StudentAverage> averages, int limit) {
        return averages.stream().limit(limit).collect(Collectors.toList());
    }

    /** Students whose average is below the passing score. */
    public static List<StudentAverage> atRisk(List<StudentAverage> averages) {
        return averages.stream().filter(a -> a.getAverage().compareTo(PASSING_SCORE) < 0)
                .collect(Collectors.toList());
    }

    /** Number of students in each performance level (all the levels appear, even with zero). */
    public static Map<PerformanceLevel, Long> levelDistribution(List<StudentAverage> averages) {
        Map<PerformanceLevel, Long> counts = new EnumMap<>(PerformanceLevel.class);
        for (PerformanceLevel level : PerformanceLevel.values()) {
            counts.put(level, 0L);
        }
        averages.stream().collect(Collectors.groupingBy(StudentAverage::getLevel, Collectors.counting()))
                .forEach(counts::put);
        return counts;
    }

    /** Average of all the students of the list (zero if the list is empty). */
    public static BigDecimal overallAverage(List<StudentAverage> averages) {
        if (averages.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal total = averages.stream().map(StudentAverage::getAverage).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(averages.size()), 2, RoundingMode.HALF_UP);
    }

    // ---------- attendance ----------

    private List<Attendance> attendanceBetween(LocalDate from, LocalDate to) throws ErpException {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ValidationException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        return attendanceDao.findByDateRange(from, to);
    }

    /** Number of attendance records of each status in a date range (all the statuses appear, even with zero). */
    public Map<AttendanceStatus, Long> attendanceSummary(LocalDate from, LocalDate to) throws ErpException {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status, 0L);
        }
        attendanceBetween(from, to).stream()
                .collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting())).forEach(counts::put);
        return counts;
    }

    /** The students with the most absences in a date range, with the number of absences. */
    public Map<String, Long> topAbsences(LocalDate from, LocalDate to, int limit) throws ErpException {
        return attendanceBetween(from, to).stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .collect(Collectors.groupingBy(Attendance::getStudentName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Part as a percentage of the total, with one decimal (zero if the total is zero). */
    public static BigDecimal percentage(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    // ---------- finance ----------

    /** Income of each month of a year (only the months with payments), as month number and total. */
    public Map<Integer, BigDecimal> incomeByMonth(int year) throws DatabaseException {
        return paymentController.getPaymentsBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentDate().getMonthValue(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
    }

    /** Income of each payment concept in a year, ordered by concept name. */
    public Map<String, BigDecimal> incomeByConcept(int year) throws ErpException {
        return paymentController.getIncomeByConcept(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public int delinquentCount() throws ErpException {
        return paymentController.getDelinquentStudents().size();
    }

    // ---------- clubs ----------

    /** Active clubs from the fullest to the emptiest. */
    public List<Club> clubOccupancy() throws DatabaseException {
        return clubDao.findActive().stream()
                .sorted(Comparator.comparing(ReportController::occupancyPercent).reversed()
                        .thenComparing(Club::getName))
                .collect(Collectors.toList());
    }

    /** Percentage of the capacity of a club that is used by active members. */
    public static BigDecimal occupancyPercent(Club club) {
        return percentage(club.getActiveMembers(), club.getMaxCapacity());
    }

    // ---------- general summary ----------

    /**
     * The main numbers of the school for a year, one line per number, in the order they must be shown.
     *
     * @throws ErpException if any of the data cannot be read
     */
    public Map<String, String> generalSummary(int year) throws ErpException {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Estudiantes activos", String.valueOf(studentsByStatus().get(StudentStatus.ACTIVE)));
        summary.put("Profesores activos", String.valueOf(teacherDao.findByStatus(TeacherStatus.ACTIVE).size()));
        summary.put("Estudiantes matriculados en " + year,
                String.valueOf(studentsPerGrade(year).values().stream().mapToLong(Long::longValue).sum()));
        summary.put("Recaudo de " + year, MoneyFormat.format(incomeByConcept(year).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        summary.put("Estudiantes morosos", String.valueOf(delinquentCount()));
        List<Club> clubs = clubDao.findActive();
        summary.put("Clubes activos", String.valueOf(clubs.size()));
        summary.put("Miembros activos en clubes", String.valueOf(clubs.stream().mapToInt(Club::getActiveMembers).sum()));
        Map<ResourceStatus, Long> resources = resourceController.countByStatus();
        summary.put("Recursos disponibles", String.valueOf(resources.get(ResourceStatus.AVAILABLE)));
        summary.put("Recursos prestados", String.valueOf(resources.get(ResourceStatus.LOANED)));
        summary.put("Préstamos vencidos", String.valueOf(loanController.listOverdue().size()));
        return summary;
    }
}
