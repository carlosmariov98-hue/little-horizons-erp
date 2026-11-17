package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for grades: registering, correcting, the yearly result and the averages.
 * The averages are calculated with Streams and BigDecimal so no precision is lost.
 */
public class GradeController {

    private static final BigDecimal MIN_SCORE = new BigDecimal("0.0");
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");
    private static final int MAX_OBSERVATIONS = 500;
    private static final int FIRST_YEAR = 2000;

    private final AcademicRecordDao recordDao;
    private final StudentDao studentDao;
    private final TeachingRules teachingRules;
    private final SubjectEnrollmentDao subjectEnrollmentDao;

    public GradeController(AcademicRecordDao recordDao, StudentDao studentDao, TeachingRules teachingRules,
                           SubjectEnrollmentDao subjectEnrollmentDao) {
        this.recordDao = recordDao;
        this.studentDao = studentDao;
        this.teachingRules = teachingRules;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
    }

    /**
     * Validates a score: between 0.0 and 5.0 with at most one decimal (the database column is DECIMAL(2,1)).
     *
     * @param score the score typed by the user
     * @return the score with exactly one decimal
     * @throws ValidationException if the score is out of range or has more than one decimal
     */
    public static BigDecimal validateScore(BigDecimal score) throws ValidationException {
        Validator.requireNotNull(score, "Nota");
        if (score.stripTrailingZeros().scale() > 1 || score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new ValidationException("La nota debe estar entre 0.0 y 5.0 y tener máximo un decimal (ejemplo: 4.2).");
        }
        return score.setScale(1, RoundingMode.UNNECESSARY);
    }

    private static void validateYear(int year) throws ValidationException {
        Validator.requireIntInRange(year, FIRST_YEAR, LocalDate.now().getYear() + 1, "Año académico");
    }

    /**
     * Registers the grade of a student in a subject for a regular period (I to IV).
     *
     * @param studentId    id of the student
     * @param subjectId    id of the subject
     * @param teacherId    id of the teacher who grades
     * @param year         academic year
     * @param period       period I, II, III or IV
     * @param score        score from 0.0 to 5.0
     * @param observations optional comments
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public void registerGrade(int studentId, int subjectId, int teacherId, int year, AcademicPeriod period,
                              BigDecimal score, String observations) throws ErpException {
        validateYear(year);
        if (!period.isRegular()) {
            throw new ValidationException("La nota Final se calcula automáticamente con las notas de los períodos I a IV.");
        }
        BigDecimal cleanScore = validateScore(score);
        String cleanObservations = Validator.optionalText(observations, "Observaciones", MAX_OBSERVATIONS);
        studentDao.findById(studentId);
        teachingRules.requireTeacherOfSubject(teacherId, subjectId, year);
        boolean registered = subjectEnrollmentDao.findByStudent(studentId, year).stream()
                .anyMatch(i -> i.getSubjectId() == subjectId && i.getPeriod() == period
                        && i.getStatus() == EnrollmentStatus.ACTIVE);
        if (!registered) {
            throw new BusinessRuleException("El estudiante no tiene una inscripción activa en esa materia para el período "
                    + period.getLabel() + " de " + year + ".");
        }
        AcademicRecord record = new AcademicRecord();
        record.setStudentId(studentId);
        record.setSubjectId(subjectId);
        record.setTeacherId(teacherId);
        record.setYear(year);
        record.setPeriod(period);
        record.setScore(cleanScore);
        record.setObservations(cleanObservations);
        record.setRecordDate(LocalDate.now());
        recordDao.insert(record);
    }

    /**
     * Corrects a grade already registered. If the correction changes a regular period and the Final grade exists,
     * the Final grade is recalculated so both stay consistent.
     *
     * @param studentId    id of the student
     * @param subjectId    id of the subject
     * @param teacherId    teacher who makes the correction
     * @param year         academic year
     * @param period       period of the grade
     * @param score        the new score
     * @param observations optional comments
     * @throws ErpException if the grade does not exist, a rule is broken, or the database fails
     */
    public void correctGrade(int studentId, int subjectId, int teacherId, int year, AcademicPeriod period,
                             BigDecimal score, String observations) throws ErpException {
        validateYear(year);
        BigDecimal cleanScore = validateScore(score);
        String cleanObservations = Validator.optionalText(observations, "Observaciones", MAX_OBSERVATIONS);
        teachingRules.requireTeacherOfSubject(teacherId, subjectId, year);
        AcademicRecord record = recordDao.find(studentId, subjectId, year, period).orElseThrow(
                () -> new EntityNotFoundException("El estudiante no tiene nota en esa materia para el período "
                        + period.getLabel() + " de " + year + "."));
        recordDao.updateScore(record.getRecordId(), cleanScore, cleanObservations, teacherId);
        if (period.isRegular()) {
            refreshFinalIfExists(studentId, subjectId, teacherId, year);
        }
    }

    // keeps the Final grade consistent when a period grade is corrected
    private void refreshFinalIfExists(int studentId, int subjectId, int teacherId, int year) throws ErpException {
        java.util.Optional<AcademicRecord> finalRecord = recordDao.find(studentId, subjectId, year, AcademicPeriod.FINAL);
        if (finalRecord.isPresent()) {
            List<AcademicRecord> regular = regularRecords(studentId, subjectId, year);
            if (regular.size() == REGULAR_COUNT) {
                recordDao.updateScore(finalRecord.get().getRecordId(), average(regular, 1),
                        "Recalculada automáticamente", teacherId);
            }
        }
    }

    private static final int REGULAR_COUNT = 4;

    private List<AcademicRecord> regularRecords(int studentId, int subjectId, int year) throws DatabaseException {
        return recordDao.findByStudentAndYear(studentId, year).stream()
                .filter(r -> r.getSubjectId() == subjectId && r.getPeriod().isRegular())
                .collect(Collectors.toList());
    }

    /**
     * Calculates and saves the Final grade of a subject: the average of periods I to IV, with one decimal.
     *
     * @param studentId id of the student
     * @param subjectId id of the subject
     * @param year      academic year
     * @return the Final score
     * @throws ErpException if the four period grades are not complete, or the Final grade already exists
     */
    public BigDecimal calculateFinalGrade(int studentId, int subjectId, int year) throws ErpException {
        validateYear(year);
        List<AcademicRecord> regular = regularRecords(studentId, subjectId, year);
        if (regular.size() < REGULAR_COUNT) {
            throw new BusinessRuleException("No se puede calcular la nota Final: el estudiante tiene "
                    + regular.size() + " de 4 períodos calificados en esa materia.");
        }
        if (recordDao.find(studentId, subjectId, year, AcademicPeriod.FINAL).isPresent()) {
            throw new BusinessRuleException("La nota Final de esa materia ya fue calculada.");
        }
        AcademicRecord last = regular.stream().max(Comparator.comparing(r -> r.getPeriod().ordinal())).get();
        AcademicRecord finalRecord = new AcademicRecord();
        finalRecord.setStudentId(studentId);
        finalRecord.setSubjectId(subjectId);
        finalRecord.setTeacherId(last.getTeacherId());
        finalRecord.setYear(year);
        finalRecord.setPeriod(AcademicPeriod.FINAL);
        finalRecord.setScore(average(regular, 1));
        finalRecord.setObservations("Promedio de los períodos I a IV");
        finalRecord.setRecordDate(LocalDate.now());
        recordDao.insert(finalRecord);
        return finalRecord.getScore();
    }

    // ---------- queries and statistics (Streams) ----------

    /**
     * Average of the scores of a list of grades.
     *
     * @param records the grades (may be empty)
     * @param scale   number of decimals of the result
     * @return the average rounded half up, or 0 when the list is empty
     */
    public static BigDecimal average(List<AcademicRecord> records, int scale) {
        if (records.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        BigDecimal sum = records.stream().map(AcademicRecord::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(records.size()), scale, RoundingMode.HALF_UP);
    }

    public List<AcademicRecord> getStudentGrades(int studentId, int year) throws ErpException {
        validateYear(year);
        studentDao.findById(studentId);
        return recordDao.findByStudentAndYear(studentId, year);
    }

    /**
     * General average of a student in one period: the average of all the subject grades of that period.
     */
    public BigDecimal getPeriodAverage(int studentId, int year, AcademicPeriod period) throws ErpException {
        return average(getStudentGrades(studentId, year).stream()
                .filter(r -> r.getPeriod() == period).collect(Collectors.toList()), 2);
    }

    /**
     * Average of each period of a student in one year (I, II, III, IV), keeping the order of the periods.
     */
    public Map<AcademicPeriod, BigDecimal> getAveragesByPeriod(int studentId, int year) throws ErpException {
        Map<AcademicPeriod, List<AcademicRecord>> byPeriod = getStudentGrades(studentId, year).stream()
                .filter(r -> r.getPeriod().isRegular())
                .collect(Collectors.groupingBy(AcademicRecord::getPeriod, TreeMap::new, Collectors.toList()));
        Map<AcademicPeriod, BigDecimal> result = new TreeMap<>();
        byPeriod.forEach((period, list) -> result.put(period, average(list, 2)));
        return result;
    }

    /**
     * Average of each subject of a student in one year (periods I to IV only), ordered by subject name.
     */
    public Map<String, BigDecimal> getAveragesBySubject(int studentId, int year) throws ErpException {
        Map<String, List<AcademicRecord>> bySubject = getStudentGrades(studentId, year).stream()
                .filter(r -> r.getPeriod().isRegular())
                .collect(Collectors.groupingBy(AcademicRecord::getSubjectName, TreeMap::new, Collectors.toList()));
        Map<String, BigDecimal> result = new TreeMap<>();
        bySubject.forEach((subject, list) -> result.put(subject, average(list, 2)));
        return result;
    }

    /**
     * Full academic history of a student: every year with its grades, from the oldest to the newest.
     */
    public Map<Integer, List<AcademicRecord>> getAcademicHistory(int studentId) throws ErpException {
        studentDao.findById(studentId);
        return recordDao.findByStudent(studentId).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getYear, TreeMap::new, Collectors.toList()));
    }

    public List<AcademicRecord> getSubjectGrades(int subjectId, int year, AcademicPeriod period) throws ErpException {
        validateYear(year);
        return recordDao.findBySubject(subjectId, year, period);
    }

    /**
     * Average of the course in each subject of a grade, for one year and period.
     */
    public Map<String, BigDecimal> getCourseAveragesBySubject(int gradeId, int year, AcademicPeriod period)
            throws ErpException {
        validateYear(year);
        Map<String, BigDecimal> result = new TreeMap<>();
        recordDao.findByGrade(gradeId, year, period).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getSubjectName, TreeMap::new, Collectors.toList()))
                .forEach((subject, list) -> result.put(subject, average(list, 2)));
        return result;
    }

    /**
     * Ranking of the students of a grade by their general average in one year and period.
     *
     * @param limit maximum number of students in the ranking
     * @return pairs (student name, average) from the best to the worst
     */
    public List<Map.Entry<String, BigDecimal>> getRanking(int gradeId, int year, AcademicPeriod period, int limit)
            throws ErpException {
        validateYear(year);
        Map<String, BigDecimal> averages = recordDao.findByGrade(gradeId, year, period).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getStudentName,
                        Collectors.collectingAndThen(Collectors.toList(), list -> average(list, 2))));
        return averages.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Subjects where the student's average in a year is below the passing score (3.0).
     */
    public List<String> getFailingSubjects(int studentId, int year) throws ErpException {
        BigDecimal passing = new BigDecimal("3.0");
        return getAveragesBySubject(studentId, year).entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(passing) < 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}