package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for the loans of resources to students and teachers: lending, returning,
 * overdue loans and the loan statistics.
 */
public class LoanController {

    /** A borrower can have at most this number of open loans at the same time. */
    public static final int MAX_OPEN_LOANS = 3;
    /** A loan can last at most this number of days. */
    public static final int MAX_LOAN_DAYS = 60;

    private static final int MAX_NOTES = 300;

    private final LoanDao loanDao;
    private final ResourceDao resourceDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public LoanController(LoanDao loanDao, ResourceDao resourceDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.loanDao = loanDao;
        this.resourceDao = resourceDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // returns the name of the borrower after checking that it exists and is active
    private String requireActiveBorrower(BorrowerType type, int borrowerId) throws ErpException {
        if (type == BorrowerType.STUDENT) {
            Student student = studentDao.findById(borrowerId);
            if (student.getStatus() != StudentStatus.ACTIVE) {
                throw new BusinessRuleException("Solo los estudiantes activos pueden pedir recursos prestados "
                        + "(estado actual: " + student.getStatus().getLabel() + ").");
            }
            return student.getFullName();
        }
        Teacher teacher = teacherDao.findById(borrowerId);
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Solo los profesores activos pueden pedir recursos prestados "
                    + "(estado actual: " + teacher.getStatus().getLabel() + ").");
        }
        return teacher.getFullName();
    }

    /**
     * Lends a resource. The resource must be available; the borrower must be active, have no late loans and have
     * fewer than {@value #MAX_OPEN_LOANS} open loans. The loan and the status of the resource are saved together.
     *
     * @param type           student or teacher
     * @param borrowerId     id of the borrower
     * @param resourceId     id of the resource
     * @param loanDate       date of the loan (today or earlier)
     * @param expectedReturn date the resource must be returned
     * @param notes          optional observations
     * @return the saved loan
     * @throws ErpException if a rule is broken, a record does not exist, or the database fails
     */
    public Loan lendResource(BorrowerType type, int borrowerId, int resourceId, LocalDate loanDate,
                             LocalDate expectedReturn, String notes) throws ErpException {
        Validator.requireNotNull(type, "Tipo de prestatario");
        Validator.requirePastOrToday(loanDate, "Fecha del préstamo");
        Validator.requireNotNull(expectedReturn, "Fecha de devolución esperada");
        if (!expectedReturn.isAfter(loanDate)) {
            throw new ValidationException("La fecha de devolución debe ser posterior a la fecha del préstamo.");
        }
        if (ChronoUnit.DAYS.between(loanDate, expectedReturn) > MAX_LOAN_DAYS) {
            throw new ValidationException("Un préstamo no puede durar más de " + MAX_LOAN_DAYS + " días.");
        }
        String cleanNotes = Validator.optionalText(notes, "Observaciones", MAX_NOTES);
        String borrowerName = requireActiveBorrower(type, borrowerId);
        Loan loan = new Loan();
        loan.setBorrowerType(type);
        loan.setBorrowerId(borrowerId);
        loan.setBorrowerName(borrowerName);
        loan.setResourceId(resourceId);
        loan.setLoanDate(loanDate);
        loan.setExpectedReturnDate(expectedReturn);
        loan.setNotes(cleanNotes);
        loanDao.lend(loan, LocalDate.now(), MAX_OPEN_LOANS);
        return loanDao.findById(type, loan.getLoanId());
    }

    /**
     * Registers the return of a loan. The resource becomes available again, or goes to maintenance if it came
     * back damaged. The observations of the loan record the days of delay automatically.
     *
     * @param damaged true if the resource came back damaged
     * @return the loan after the return
     * @throws ErpException if the loan does not exist, was already returned, the date is invalid, or the database fails
     */
    public Loan returnResource(BorrowerType type, int loanId, LocalDate returnDate, boolean damaged, String notes)
            throws ErpException {
        Validator.requireNotNull(type, "Tipo de prestatario");
        Validator.requirePastOrToday(returnDate, "Fecha de devolución");
        String cleanNotes = Validator.optionalText(notes, "Observaciones", MAX_NOTES);
        Loan loan = loanDao.findById(type, loanId);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessRuleException("Ese préstamo ya fue devuelto.");
        }
        loanDao.giveBack(type, loanId, returnDate, buildReturnNotes(loan, returnDate, damaged, cleanNotes), damaged);
        return loanDao.findById(type, loanId);
    }

    // the text saved with a returned loan: delay, damage and the notes typed by the user
    private static String buildReturnNotes(Loan loan, LocalDate returnDate, boolean damaged, String notes) {
        StringBuilder text = new StringBuilder();
        long late = ChronoUnit.DAYS.between(loan.getExpectedReturnDate(), returnDate);
        text.append(late > 0 ? "Devuelto con " + late + (late == 1 ? " día" : " días") + " de retraso"
                : "Devuelto a tiempo");
        if (damaged) {
            text.append(". Devuelto con daños: enviado a mantenimiento");
        }
        if (notes != null) {
            text.append(". ").append(notes);
        }
        return text.toString();
    }

    /**
     * Marks as overdue the active loans whose expected return date has passed.
     *
     * @return how many loans changed to overdue
     * @throws DatabaseException if the update fails
     */
    public int refreshOverdue() throws DatabaseException {
        return loanDao.markOverdue(LocalDate.now());
    }

    /** Overdue loans, with the one that is the most late first. */
    public List<Loan> listOverdue() throws DatabaseException {
        refreshOverdue();
        LocalDate today = LocalDate.now();
        return loanDao.findOpen().stream().filter(l -> l.getStatus() == LoanStatus.OVERDUE)
                .sorted(Comparator.comparingLong((Loan l) -> l.getDaysLate(today)).reversed())
                .collect(Collectors.toList());
    }

    /** Loans that have not been returned, ordered by expected return date. */
    public List<Loan> listOpen() throws DatabaseException {
        refreshOverdue();
        return loanDao.findOpen().stream().sorted(Comparator.comparing(Loan::getExpectedReturnDate))
                .collect(Collectors.toList());
    }

    public List<Loan> getLoansOfBorrower(BorrowerType type, int borrowerId) throws ErpException {
        if (type == BorrowerType.STUDENT) {
            studentDao.findById(borrowerId);
        } else {
            teacherDao.findById(borrowerId);
        }
        return loanDao.findByBorrower(type, borrowerId);
    }

    public List<Loan> getLoansOfResource(int resourceId) throws ErpException {
        resourceDao.findById(resourceId);
        return loanDao.findByResource(resourceId).stream()
                .sorted(Comparator.comparing(Loan::getLoanDate).reversed()).collect(Collectors.toList());
    }

    // ---------- statistics (Streams) ----------

    /** The most borrowed resources of all time, with the number of loans, from the most to the fewest. */
    public Map<String, Long> getMostBorrowed(int limit) throws DatabaseException {
        return loanDao.findAll().stream()
                .collect(Collectors.groupingBy(l -> l.getResourceCode() + " - " + l.getResourceName(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Number of loans of students and of teachers. */
    public Map<String, Long> countByBorrowerType() throws DatabaseException {
        return loanDao.findAll().stream().collect(Collectors.groupingBy(l -> l.getBorrowerType().getLabel(),
                TreeMap::new, Collectors.counting()));
    }

    /** Number of returned loans that came back after the expected date. */
    public long countLateReturns() throws DatabaseException {
        return loanDao.findAll().stream()
                .filter(l -> l.getStatus() == LoanStatus.RETURNED && l.getDaysLate(LocalDate.now()) > 0).count();
    }

    /** Average days of delay of the returned loans that were late (zero if none), with two decimals. */
    public BigDecimal averageDaysLate() throws DatabaseException {
        List<Long> delays = loanDao.findAll().stream().filter(l -> l.getStatus() == LoanStatus.RETURNED)
                .map(l -> l.getDaysLate(LocalDate.now())).filter(days -> days > 0).collect(Collectors.toList());
        if (delays.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal total = delays.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(delays.size()), 2, RoundingMode.HALF_UP);
    }
}