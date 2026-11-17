package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.FileOperationException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianAssignment;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentResult;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.InvoiceFileWriter;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for payments: registering them with their invoice, the payment history,
 * the income summary and the list of students who are late with the tuition.
 */
public class PaymentController {

    /** Code of the tuition concept, used to detect late students. */
    public static final String TUITION_CODE = "PENSION";
    /** A student with no tuition payment in this number of days is considered late. */
    public static final int DELINQUENCY_DAYS = 30;

    private static final int FIRST_YEAR = 2000;
    private static final int MAX_NOTES = 500;

    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final PaymentConceptDao conceptDao;
    private final StudentDao studentDao;
    private final StudentGuardianDao studentGuardianDao;
    private final GuardianDao guardianDao;
    private final InvoiceFileWriter invoiceWriter;

    public PaymentController(PaymentDao paymentDao, InvoiceDao invoiceDao, PaymentConceptDao conceptDao,
                             StudentDao studentDao, StudentGuardianDao studentGuardianDao, GuardianDao guardianDao,
                             InvoiceFileWriter invoiceWriter) {
        this.paymentDao = paymentDao;
        this.invoiceDao = invoiceDao;
        this.conceptDao = conceptDao;
        this.studentDao = studentDao;
        this.studentGuardianDao = studentGuardianDao;
        this.guardianDao = guardianDao;
        this.invoiceWriter = invoiceWriter;
    }

    // the guardian chosen by the user, or by default the financially responsible one, then the primary one
    private Guardian resolveGuardian(int studentId, Integer requestedGuardianId) throws ErpException {
        List<GuardianAssignment> links = studentGuardianDao.findByStudent(studentId).stream()
                .filter(link -> link.getGuardian().isActive()).collect(Collectors.toList());
        if (links.isEmpty()) {
            throw new BusinessRuleException("El estudiante no tiene acudientes activos para asociar el pago.");
        }
        if (requestedGuardianId != null) {
            return links.stream().map(GuardianAssignment::getGuardian)
                    .filter(g -> g.getGuardianId() == requestedGuardianId).findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "El acudiente indicado no está asociado a este estudiante."));
        }
        return links.stream().map(GuardianAssignment::getGuardian).filter(Guardian::isFinanciallyResponsible).findFirst()
                .orElse(links.get(0).getGuardian());
    }

    /**
     * Registers a payment, creates its invoice and writes the TXT file of the invoice.
     * The payment and the invoice are saved together; if only the TXT file fails, the payment stays saved
     * and the result carries a warning so the file can be generated again.
     *
     * @param studentId  id of the student
     * @param guardianId id of the guardian who paid, or null to use the financially responsible one
     * @param conceptId  id of the concept
     * @param amount     amount paid, or null to charge the base amount of the concept
     * @param date       date of the payment
     * @param method     payment method
     * @param year       academic year the payment applies to
     * @param period     period the payment applies to (required for recurring concepts)
     * @param notes      optional notes
     * @return the saved payment, its invoice and the file (or a warning)
     * @throws ErpException if a rule is broken, a record is missing, or the database fails
     */
    public PaymentResult registerPayment(int studentId, Integer guardianId, int conceptId, BigDecimal amount,
                                         LocalDate date, PaymentMethod method, int year, PaymentPeriod period,
                                         String notes) throws ErpException {
        Validator.requirePastOrToday(date, "Fecha del pago");
        Validator.requireNotNull(method, "Método de pago");
        Validator.requireIntInRange(year, FIRST_YEAR, LocalDate.now().getYear() + 1, "Año académico");
        String cleanNotes = Validator.optionalText(notes, "Notas", MAX_NOTES);
        Student student = studentDao.findById(studentId);
        if (student.getStatus() != StudentStatus.ACTIVE && student.getStatus() != StudentStatus.SUSPENDED) {
            throw new BusinessRuleException("Solo se registran pagos de estudiantes activos o suspendidos "
                    + "(estado actual: " + student.getStatus().getLabel() + ").");
        }
        PaymentConcept concept = conceptDao.findById(conceptId);
        if (!concept.isActive()) {
            throw new BusinessRuleException("El concepto " + concept.getName() + " no está activo.");
        }
        BigDecimal paid = PaymentConceptController.validateAmount(amount == null ? concept.getBaseAmount() : amount,
                "Monto pagado");
        if (paid.compareTo(concept.getBaseAmount()) > 0) {
            throw new BusinessRuleException("El monto no puede superar el valor base del concepto ("
                    + com.littlehorizons.erp.util.MoneyFormat.format(concept.getBaseAmount()) + ").");
        }
        if (concept.isRecurring() && period == null) {
            throw new ValidationException("El concepto " + concept.getName() + " es recurrente: indique el período.");
        }
        Guardian guardian = resolveGuardian(studentId, guardianId);
        if (paymentDao.existsIdentical(studentId, conceptId, date, paid)) {
            throw new BusinessRuleException("Ya existe un pago idéntico (mismo estudiante, concepto, fecha y monto). "
                    + "Parece un registro duplicado.");
        }
        Payment payment = new Payment();
        payment.setStudentId(studentId);
        payment.setStudentName(student.getFullName());
        payment.setGuardianId(guardian.getGuardianId());
        payment.setGuardianName(guardian.getFullName());
        payment.setConceptId(conceptId);
        payment.setConceptCode(concept.getCode());
        payment.setConceptName(concept.getName());
        payment.setAmount(paid);
        payment.setPaymentDate(date);
        payment.setMethod(method);
        payment.setYear(year);
        payment.setPeriod(period);
        payment.setNotes(cleanNotes);
        Invoice invoice = paymentDao.registerWithInvoice(payment, LocalDate.now());
        try {
            Path file = writeInvoiceFile(invoice, payment, guardian, student.getDocumentNumber());
            return new PaymentResult(payment, invoice, file, null);
        } catch (FileOperationException | DatabaseException e) {
            return new PaymentResult(payment, invoice, null, "El pago y la factura quedaron guardados, pero no se pudo "
                    + "generar el archivo TXT (" + e.getMessage() + ") Use la opción de generar el archivo de la factura "
                    + "para intentarlo de nuevo.");
        }
    }

    private Path writeInvoiceFile(Invoice invoice, Payment payment, Guardian guardian, String studentDocument)
            throws FileOperationException, DatabaseException, EntityNotFoundException {
        Path file = invoiceWriter.write(invoice, payment, guardian, studentDocument);
        invoiceDao.updateFile(invoice.getInvoiceId(), file.getFileName().toString(), file.toString());
        invoice.setFileName(file.getFileName().toString());
        invoice.setFilePath(file.toString());
        return file;
    }

    /**
     * Writes again the TXT file of the invoice of a payment (for old invoices that have no file, or after an error).
     *
     * @param paymentId id of the payment
     * @return the path of the file
     * @throws ErpException if the payment or its invoice does not exist, the file cannot be written, or the database fails
     */
    public Path generateInvoiceFile(int paymentId) throws ErpException {
        Payment payment = paymentDao.findById(paymentId);
        Invoice invoice = invoiceDao.findByPaymentId(paymentId).orElseThrow(
                () -> new EntityNotFoundException("El pago " + paymentId + " no tiene una factura asociada."));
        if (payment.getGuardianId() == 0) {
            throw new BusinessRuleException("El pago no tiene un acudiente asociado, necesario para la factura.");
        }
        Guardian guardian = guardianDao.findById(payment.getGuardianId());
        Student student = studentDao.findById(payment.getStudentId());
        return writeInvoiceFile(invoice, payment, guardian, student.getDocumentNumber());
    }

    public Invoice getInvoice(int paymentId) throws ErpException {
        paymentDao.findById(paymentId);
        return invoiceDao.findByPaymentId(paymentId).orElseThrow(
                () -> new EntityNotFoundException("El pago " + paymentId + " no tiene una factura asociada."));
    }

    public Payment getPayment(int paymentId) throws DatabaseException, EntityNotFoundException {
        return paymentDao.findById(paymentId);
    }

    public List<Payment> getStudentPayments(int studentId) throws ErpException {
        studentDao.findById(studentId);
        return paymentDao.findByStudent(studentId);
    }

    public List<Payment> getGuardianPayments(int guardianId) throws ErpException {
        guardianDao.findById(guardianId);
        return paymentDao.findByGuardian(guardianId);
    }

    // ---------- statistics (Streams) ----------

    /** Sum of the amounts of a list of payments. */
    public static BigDecimal total(List<Payment> payments) {
        return payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total paid for each concept, ordered by concept name. */
    public static Map<String, BigDecimal> totalsByConcept(List<Payment> payments) {
        return payments.stream().collect(Collectors.groupingBy(Payment::getConceptName, TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
    }

    /**
     * Income of the school between two dates, grouped by concept.
     *
     * @throws ErpException if the dates are not valid or the database fails
     */
    public Map<String, BigDecimal> getIncomeByConcept(LocalDate from, LocalDate to) throws ErpException {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ValidationException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        return totalsByConcept(paymentDao.findByDateRange(from, to));
    }

    public List<Payment> getPaymentsBetween(LocalDate from, LocalDate to) throws DatabaseException {
        return paymentDao.findByDateRange(from, to);
    }

    /**
     * Active students with no tuition payment in the last {@value #DELINQUENCY_DAYS} days, ordered from the
     * ones that never paid and then from the oldest last payment.
     *
     * @throws ErpException if the tuition concept does not exist or the database fails
     */
    public List<DelinquentStudent> getDelinquentStudents() throws ErpException {
        PaymentConcept tuition = conceptDao.findByCode(TUITION_CODE).orElseThrow(
                () -> new EntityNotFoundException("No existe el concepto de pensión (" + TUITION_CODE + ")."));
        Map<Integer, LocalDate> lastPayments = paymentDao.findLastPaymentDates(tuition.getConceptId());
        LocalDate limit = LocalDate.now().minusDays(DELINQUENCY_DAYS);
        return studentDao.findByStatus(StudentStatus.ACTIVE).stream()
                .map(student -> new DelinquentStudent(student, lastPayments.get(student.getStudentId())))
                .filter(item -> item.getLastPaymentDate() == null || item.getLastPaymentDate().isBefore(limit))
                .sorted(Comparator.comparing(DelinquentStudent::getLastPaymentDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}