package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.util.CufeGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.sql.Types;

/**
 * Data Access Object for the table "pagos". A payment and its invoice are always saved together
 * in a single transaction, so there is never a payment without invoice or an invoice without payment.
 */
public class PaymentDao extends BaseDao {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final String SELECT = "SELECT p.id_pago, p.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre_estudiante, "
            + "p.id_acudiente, CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido, a.segundo_apellido) "
            + "AS nombre_acudiente, p.id_concepto, c.codigo_concepto, c.nombre_concepto, p.monto_pagado, p.fecha_pago, "
            + "p.metodo_pago, p.numero_recibo, p.anio_academico, p.periodo_academico, p.notas, f.numero_factura "
            + "FROM pagos p JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
            + "LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente "
            + "JOIN conceptos_pago c ON c.id_concepto = p.id_concepto LEFT JOIN facturas f ON f.id_pago = p.id_pago";
    private static final String ORDER_NEWEST_FIRST = " ORDER BY p.fecha_pago DESC, p.id_pago DESC";

    public PaymentDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Saves a payment and its invoice in one transaction. The receipt number (REC-000001) and the invoice
     * number (LH-000001) are the next consecutive numbers, and the CUFE of the invoice is calculated here.
     *
     * @param payment   the payment to save; it receives its id and receipt number
     * @param issueDate issue date of the invoice
     * @return the saved invoice
     * @throws BusinessRuleException   if another payment took the same consecutive number
     * @throws EntityNotFoundException if the student, guardian or concept does not exist
     * @throws DatabaseException       if the data cannot be saved (nothing is saved)
     */
    public Invoice registerWithInvoice(Payment payment, LocalDate issueDate)
            throws DatabaseException, BusinessRuleException, EntityNotFoundException {
        return inTransaction("Could not register payment of student id " + payment.getStudentId() + ".",
                "No se pudo registrar el pago. No se guardó ningún dato.", connection -> {
                    try {
                        int nextReceipt = lastNumber(connection, "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_recibo, 5) "
                                + "AS UNSIGNED)), 0) FROM pagos FOR UPDATE") + 1;
                        payment.setReceiptNumber(String.format("REC-%06d", nextReceipt));
                        payment.setPaymentId(insertPayment(connection, payment));
                        int nextInvoice = lastNumber(connection, "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_factura, 4) "
                                + "AS UNSIGNED)), 0) FROM facturas FOR UPDATE") + 1;
                        Invoice invoice = new Invoice();
                        invoice.setNumber(String.format("LH-%06d", nextInvoice));
                        invoice.setPaymentId(payment.getPaymentId());
                        invoice.setIssueDate(issueDate);
                        invoice.setCufe(CufeGenerator.generate(invoice.getNumber(), issueDate));
                        invoice.setInvoiceId(insertInvoice(connection, invoice));
                        payment.setInvoiceNumber(invoice.getNumber());
                        return invoice;
                    } catch (SQLException e) {
                        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                            throw new BusinessRuleException("Otro pago tomó el mismo consecutivo. Intente de nuevo.", e);
                        }
                        throw e;
                    }
                });
    }

    private static int lastNumber(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private int insertPayment(Connection connection, Payment payment) throws SQLException {
        String sql = "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, "
                + "metodo_pago, numero_recibo, anio_academico, periodo_academico, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, payment.getStudentId());
            statement.setInt(2, payment.getGuardianId());
            statement.setInt(3, payment.getConceptId());
            statement.setBigDecimal(4, payment.getAmount());
            statement.setDate(5, toSqlDate(payment.getPaymentDate()));
            statement.setString(6, payment.getMethod().getLabel());
            statement.setString(7, payment.getReceiptNumber());
            statement.setInt(8, payment.getYear());
            if (payment.getPeriod() == null) {
                statement.setNull(9, Types.VARCHAR);
            } else {
                statement.setString(9, payment.getPeriod().getLabel());
            }
            statement.setString(10, blankToNull(payment.getNotes()));
            statement.executeUpdate();
            return generatedKey(statement);
        }
    }

    private int insertInvoice(Connection connection, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, invoice.getNumber());
            statement.setInt(2, invoice.getPaymentId());
            statement.setDate(3, toSqlDate(invoice.getIssueDate()));
            statement.setString(4, invoice.getCufe());
            statement.executeUpdate();
            return generatedKey(statement);
        }
    }

    private static int generatedKey(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("The database did not return the generated id.");
            }
            return keys.getInt(1);
        }
    }

    public Payment findById(int paymentId) throws DatabaseException, EntityNotFoundException {
        List<Payment> found = queryList(SELECT + " WHERE p.id_pago = ?", statement -> statement.setInt(1, paymentId),
                this::mapRow, "Could not find payment id " + paymentId + ".", "No se pudo consultar el pago.");
        if (found.isEmpty()) {
            throw new EntityNotFoundException("No se encontró un pago con el id " + paymentId + ".");
        }
        return found.get(0);
    }

    public List<Payment> findByStudent(int studentId) throws DatabaseException {
        return queryList(SELECT + " WHERE p.id_estudiante = ?" + ORDER_NEWEST_FIRST,
                statement -> statement.setInt(1, studentId), this::mapRow,
                "Could not list payments of student id " + studentId + ".", "No se pudo consultar los pagos del estudiante.");
    }

    public List<Payment> findByGuardian(int guardianId) throws DatabaseException {
        return queryList(SELECT + " WHERE p.id_acudiente = ?" + ORDER_NEWEST_FIRST,
                statement -> statement.setInt(1, guardianId), this::mapRow,
                "Could not list payments of guardian id " + guardianId + ".", "No se pudo consultar los pagos del acudiente.");
    }

    public List<Payment> findByDateRange(LocalDate from, LocalDate to) throws DatabaseException {
        return queryList(SELECT + " WHERE p.fecha_pago BETWEEN ? AND ?" + ORDER_NEWEST_FIRST, statement -> {
                    statement.setDate(1, toSqlDate(from));
                    statement.setDate(2, toSqlDate(to));
                }, this::mapRow, "Could not list payments between " + from + " and " + to + ".",
                "No se pudo consultar los pagos del período.");
    }

    /**
     * Tells whether an identical payment (same student, concept, date and amount) is already saved,
     * which usually means the payment was typed twice by mistake.
     */
    public boolean existsIdentical(int studentId, int conceptId, LocalDate date, BigDecimal amount)
            throws DatabaseException {
        return queryCount("SELECT COUNT(*) FROM pagos WHERE id_estudiante = ? AND id_concepto = ? AND fecha_pago = ? "
                + "AND monto_pagado = ?", statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, conceptId);
            statement.setDate(3, toSqlDate(date));
            statement.setBigDecimal(4, amount);
        }, "Could not check duplicated payment.", "No se pudo verificar el pago.") > 0;
    }

    /**
     * Date of the last payment of a concept for each student that has paid it.
     *
     * @param conceptId id of the concept
     * @return map from student id to the date of the latest payment
     * @throws DatabaseException if the query fails
     */
    public Map<Integer, LocalDate> findLastPaymentDates(int conceptId) throws DatabaseException {
        return queryList("SELECT id_estudiante, MAX(fecha_pago) FROM pagos WHERE id_concepto = ? GROUP BY id_estudiante",
                statement -> statement.setInt(1, conceptId),
                resultSet -> new AbstractMap.SimpleEntry<>(resultSet.getInt(1), toLocalDate(resultSet.getDate(2))),
                "Could not read the last payments of concept id " + conceptId + ".",
                "No se pudo consultar los últimos pagos.").stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Payment mapRow(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(resultSet.getInt("id_pago"));
        payment.setStudentId(resultSet.getInt("id_estudiante"));
        payment.setStudentName(resultSet.getString("nombre_estudiante"));
        payment.setGuardianId(resultSet.getInt("id_acudiente"));
        payment.setGuardianName(resultSet.getString("nombre_acudiente"));
        payment.setConceptId(resultSet.getInt("id_concepto"));
        payment.setConceptCode(resultSet.getString("codigo_concepto"));
        payment.setConceptName(resultSet.getString("nombre_concepto"));
        payment.setAmount(resultSet.getBigDecimal("monto_pagado"));
        payment.setPaymentDate(toLocalDate(resultSet.getDate("fecha_pago")));
        try {
            payment.setMethod(PaymentMethod.fromLabel(resultSet.getString("metodo_pago")));
            String period = resultSet.getString("periodo_academico");
            payment.setPeriod(period == null ? null : PaymentPeriod.fromLabel(period));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid value stored in table pagos: " + e.getMessage(), e);
        }
        payment.setReceiptNumber(resultSet.getString("numero_recibo"));
        payment.setYear(resultSet.getInt("anio_academico"));
        payment.setNotes(resultSet.getString("notas"));
        payment.setInvoiceNumber(resultSet.getString("numero_factura"));
        return payment;
    }
}