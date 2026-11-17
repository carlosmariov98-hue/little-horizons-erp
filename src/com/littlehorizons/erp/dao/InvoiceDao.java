package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.ConnectionProvider;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.model.Invoice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data Access Object for the table "facturas". Invoices are created together with their payment
 * (see {@link PaymentDao}); this class reads them and stores the location of the TXT file.
 */
public class InvoiceDao extends BaseDao {

    private static final String SELECT = "SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe, "
            + "nombre_archivo, ruta_archivo FROM facturas";

    public InvoiceDao(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public Optional<Invoice> findByPaymentId(int paymentId) throws DatabaseException {
        return queryList(SELECT + " WHERE id_pago = ?", statement -> statement.setInt(1, paymentId), this::mapRow,
                "Could not find the invoice of payment id " + paymentId + ".", "No se pudo consultar la factura.")
                .stream().findFirst();
    }

    /**
     * Stores the name and path of the TXT file generated for an invoice.
     *
     * @throws EntityNotFoundException if the invoice does not exist
     * @throws DatabaseException       if the data cannot be saved
     */
    public void updateFile(int invoiceId, String fileName, String filePath)
            throws DatabaseException, EntityNotFoundException {
        try {
            int rows = executeUpdate("UPDATE facturas SET nombre_archivo = ?, ruta_archivo = ? WHERE id_factura = ?",
                    statement -> {
                        statement.setString(1, fileName);
                        statement.setString(2, filePath);
                        statement.setInt(3, invoiceId);
                    }, "", "Could not save the file of invoice id " + invoiceId + ".",
                    "No se pudo guardar la ubicación del archivo de la factura.");
            if (rows == 0) {
                requireExists("facturas", "id_factura", invoiceId, "No se encontró la factura con el id " + invoiceId + ".");
            }
        } catch (BusinessRuleException e) {
            throw new DatabaseException("No se pudo guardar la ubicación del archivo de la factura.", e);
        }
    }

    private Invoice mapRow(ResultSet resultSet) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(resultSet.getInt("id_factura"));
        invoice.setNumber(resultSet.getString("numero_factura"));
        invoice.setPaymentId(resultSet.getInt("id_pago"));
        invoice.setIssueDate(toLocalDate(resultSet.getDate("fecha_emision")));
        invoice.setCufe(resultSet.getString("cufe"));
        invoice.setFileName(resultSet.getString("nombre_archivo"));
        invoice.setFilePath(resultSet.getString("ruta_archivo"));
        return invoice;
    }
}