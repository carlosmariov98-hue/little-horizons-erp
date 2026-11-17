package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.ErpException;
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
import com.littlehorizons.erp.util.DateUtils;
import com.littlehorizons.erp.util.MoneyFormat;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Console menu for the financial module: payments, invoices, history, late students and income. */
public class FinanceMenu extends BaseMenu {

    private final PaymentController payments;
    private final PaymentConceptController concepts;
    private final StudentController students;
    private final GuardianController guardians;
    private final PaymentConceptMenu conceptMenu;

    public FinanceMenu(InputReader input, PaymentController payments, PaymentConceptController concepts,
                       StudentController students, GuardianController guardians, PaymentConceptMenu conceptMenu) {
        super(input);
        this.payments = payments;
        this.concepts = concepts;
        this.students = students;
        this.guardians = guardians;
        this.conceptMenu = conceptMenu;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("GESTIÓN FINANCIERA");
            Console.info("1. Registrar un pago (genera factura)");
            Console.info("2. Historial de pagos de un estudiante");
            Console.info("3. Historial de pagos de un acudiente");
            Console.info("4. Estudiantes morosos");
            Console.info("5. Ingresos por concepto en un rango de fechas");
            Console.info("6. Consultar o regenerar la factura de un pago");
            Console.info("7. Conceptos de pago");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 7)) {
                case 1:
                    runSafely(this::registerPayment);
                    break;
                case 2:
                    runSafely(this::studentHistory);
                    break;
                case 3:
                    runSafely(this::guardianHistory);
                    break;
                case 4:
                    runSafely(this::delinquents);
                    break;
                case 5:
                    runSafely(this::incomeReport);
                    break;
                case 6:
                    runSafely(this::invoiceOptions);
                    break;
                case 7:
                    conceptMenu.run();
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private Student askStudent() throws ErpException {
        Student student = students.getById(readId("ID del estudiante: "));
        Console.info("Estudiante: " + student.getFullName() + " (" + student.getStatus().getLabel() + ")");
        return student;
    }

    private void registerPayment() throws ErpException {
        Console.title("REGISTRAR UN PAGO");
        Student student = askStudent();
        List<GuardianAssignment> links = students.getGuardiansOf(student.getStudentId());
        Integer guardianId = null;
        if (!links.isEmpty()) {
            Console.info("Acudientes del estudiante:");
            links.forEach(l -> Console.info("  - " + l.getGuardian().getFullName()
                    + (l.getGuardian().isFinanciallyResponsible() ? " (responsable financiero)" : "")));
            if (input.readYesNo("¿Factura a nombre del responsable financiero? (s = sí, n = elegir otro): ")) {
                guardianId = null;
            } else {
                guardianId = input.readChoice("Acudiente que paga:", links, l -> l.getGuardian().getFullName())
                        .getGuardian().getGuardianId();
            }
        }
        PaymentConcept concept = input.readChoice("Concepto:", concepts.listActive(),
                c -> c.getName() + " - " + MoneyFormat.format(c.getBaseAmount()));
        BigDecimal amount = readAmountOrDefault(concept);
        LocalDate today = LocalDate.now();
        LocalDate date = input.readDate("Fecha del pago (dd/MM/yyyy, Enter = hoy): ", today);
        PaymentMethod method = input.readChoice("Método de pago:", Arrays.asList(PaymentMethod.values()),
                PaymentMethod::getLabel);
        int year = readYear();
        PaymentPeriod period = readPaymentPeriod(concept);
        String notes = input.readLine("Notas (Enter = ninguna): ");

        Console.info("");
        Console.info("Resumen: " + student.getFullName() + " | " + concept.getName() + " | " + MoneyFormat.format(amount)
                + " | " + DateUtils.format(date) + " | " + method.getLabel());
        if (!input.readYesNo("¿Confirmar el pago? (s/n): ")) {
            Console.info("Pago no registrado.");
            return;
        }
        PaymentResult result = payments.registerPayment(student.getStudentId(), guardianId, concept.getConceptId(),
                amount, date, method, year, period, notes);
        Console.success("Pago registrado. Recibo " + result.getPayment().getReceiptNumber() + " | Factura "
                + result.getInvoice().getNumber());
        Console.info("CUFE: " + result.getInvoice().getCufe());
        if (result.getFile() != null) {
            Console.info("Archivo de la factura: " + result.getFile());
        } else {
            Console.error(result.getFileWarning());
        }
    }

    private BigDecimal readAmountOrDefault(PaymentConcept concept) throws ErpException {
        return input.readValid("Monto pagado (Enter = " + MoneyFormat.format(concept.getBaseAmount()) + "): ", text -> {
            if (text.isEmpty()) {
                return concept.getBaseAmount();
            }
            try {
                return PaymentConceptController.validateAmount(new BigDecimal(text), "Monto pagado");
            } catch (NumberFormatException e) {
                throw new ValidationException("Ingrese un monto válido, sin separador de miles (ejemplo: 350000).");
            }
        });
    }

    private PaymentPeriod readPaymentPeriod(PaymentConcept concept) throws ErpException {
        String hint = concept.isRecurring() ? "obligatorio" : "Enter = ninguno";
        return input.readValid("Período (1 = I, 2 = II, 3 = III, 4 = IV, 5 = Anual; " + hint + "): ", text -> {
            if (text.isEmpty()) {
                if (concept.isRecurring()) {
                    throw new ValidationException("El concepto " + concept.getName() + " requiere un período.");
                }
                return null;
            }
            PaymentPeriod[] all = PaymentPeriod.values();
            try {
                int index = Integer.parseInt(text);
                if (index >= 1 && index <= all.length) {
                    return all[index - 1];
                }
            } catch (NumberFormatException e) {
                // same message below for any invalid entry
            }
            throw new ValidationException("Ingrese un número entre 1 y " + all.length + ".");
        });
    }

    private void printPayments(List<Payment> list) {
        if (list.isEmpty()) {
            Console.info("No hay pagos registrados.");
            return;
        }
        Console.info(String.format("%5s | %-10s | %-10s | %-22s | %-10s | %16s | %s", "ID", "Fecha", "Recibo", "Concepto",
                "Período", "Monto", "Factura"));
        for (Payment p : list) {
            Console.info(String.format("%5d | %-10s | %-10s | %-22s | %-10s | %16s | %s", p.getPaymentId(),
                    DateUtils.format(p.getPaymentDate()), p.getReceiptNumber(), p.getConceptName(),
                    p.getPeriod() == null ? "-" : p.getYear() + " " + p.getPeriod().getLabel(),
                    MoneyFormat.format(p.getAmount()), dash(p.getInvoiceNumber())));
        }
        Console.info("Total pagado: " + MoneyFormat.format(PaymentController.total(list)));
        Console.info("Por concepto:");
        PaymentController.totalsByConcept(list)
                .forEach((name, total) -> Console.info("  - " + name + ": " + MoneyFormat.format(total)));
    }

    private void studentHistory() throws ErpException {
        Console.title("HISTORIAL DE PAGOS DE UN ESTUDIANTE");
        Student student = askStudent();
        printPayments(payments.getStudentPayments(student.getStudentId()));
    }

    private void guardianHistory() throws ErpException {
        Console.title("HISTORIAL DE PAGOS DE UN ACUDIENTE");
        Guardian guardian = guardians.getById(readId("ID del acudiente: "));
        Console.info("Acudiente: " + guardian.getFullName());
        printPayments(payments.getGuardianPayments(guardian.getGuardianId()));
    }

    private void delinquents() throws ErpException {
        List<DelinquentStudent> list = payments.getDelinquentStudents();
        Console.title("ESTUDIANTES MOROSOS (sin pensión en los últimos " + PaymentController.DELINQUENCY_DAYS
                + " días)");
        if (list.isEmpty()) {
            Console.success("Todos los estudiantes activos están al día.");
            return;
        }
        for (DelinquentStudent item : list) {
            Console.info(String.format("%4d | %-34s | %s", item.getStudent().getStudentId(),
                    item.getStudent().getFullName(), item.getLastPaymentDate() == null ? "Nunca ha pagado pensión"
                            : "Último pago de pensión: " + DateUtils.format(item.getLastPaymentDate())));
        }
        Console.info("Total morosos: " + list.size());
    }

    private void incomeReport() throws ErpException {
        Console.title("INGRESOS POR CONCEPTO");
        LocalDate from = input.readDate("Fecha inicial (dd/MM/yyyy): ", null);
        LocalDate to = input.readDate("Fecha final (dd/MM/yyyy, Enter = hoy): ", LocalDate.now());
        Map<String, BigDecimal> income = payments.getIncomeByConcept(from, to);
        if (income.isEmpty()) {
            Console.info("No hay pagos en ese rango de fechas.");
            return;
        }
        income.forEach((name, total) -> Console.info(String.format("  %-30s %18s", name, MoneyFormat.format(total))));
        BigDecimal all = income.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Console.info(String.format("  %-30s %18s", "TOTAL", MoneyFormat.format(all)));
    }

    private void invoiceOptions() throws ErpException {
        int paymentId = readId("ID del pago: ");
        Invoice invoice = payments.getInvoice(paymentId);
        Console.info("Factura: " + invoice.getNumber() + " | Emitida: " + DateUtils.format(invoice.getIssueDate()));
        Console.info("CUFE: " + invoice.getCufe());
        Console.info("Archivo: " + dash(invoice.getFilePath()));
        String question = invoice.getFilePath() == null ? "La factura no tiene archivo TXT. ¿Generarlo ahora? (s/n): "
                : "¿Volver a generar el archivo TXT? (s/n): ";
        if (input.readYesNo(question)) {
            Path file = payments.generateInvoiceFile(paymentId);
            Console.success("Archivo generado: " + file);
        }
    }
}
