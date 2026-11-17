package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.FileOperationException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the invoice as a plain text (TXT) file. The invoice is simulated: the DIAN resolution,
 * the CUFE and the QR text are examples that follow the structure of a real electronic invoice.
 */
public class InvoiceFileWriter {

    private static final String SEPARATOR = "=".repeat(64);
    private static final String LINE = "-".repeat(64);

    private final Path directory;

    public InvoiceFileWriter(Path directory) {
        this.directory = directory;
    }

    /**
     * Builds the name of the file of an invoice, for example LH-000123 becomes factura_000123.txt.
     *
     * @param invoiceNumber invoice number
     * @return the file name
     */
    public static String fileNameOf(String invoiceNumber) {
        return "factura_" + invoiceNumber.substring(invoiceNumber.indexOf('-') + 1) + ".txt";
    }

    /**
     * Writes (or overwrites) the TXT file of an invoice inside the invoices directory.
     *
     * @param invoice  the invoice
     * @param payment  the paid payment
     * @param guardian the client of the invoice
     * @param studentDocument document of the student, shown in the detail
     * @return the path of the written file
     * @throws FileOperationException if the directory or the file cannot be written
     */
    public Path write(Invoice invoice, Payment payment, Guardian guardian, String studentDocument)
            throws FileOperationException {
        Path file = directory.resolve(fileNameOf(invoice.getNumber()));
        try {
            Files.createDirectories(directory);
            Files.write(file, buildContent(invoice, payment, guardian, studentDocument), StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            AppLogger.getInstance().error("Could not write invoice file " + file + ".", e);
            throw new FileOperationException("No se pudo escribir el archivo de la factura " + invoice.getNumber()
                    + ". Verifique que la carpeta 'invoices' exista y tenga permisos de escritura.", e);
        }
    }

    private List<String> buildContent(Invoice invoice, Payment payment, Guardian guardian, String studentDocument) {
        BigDecimal total = payment.getAmount();
        BigDecimal base = TaxPolicy.taxableBase(payment.getConceptCode(), total);
        BigDecimal vat = TaxPolicy.vatAmount(payment.getConceptCode(), total);
        String period = payment.getPeriod() == null ? "-" : payment.getPeriod().getLabel();
        List<String> lines = new ArrayList<>();
        lines.add(SEPARATOR);
        lines.add("            COLEGIO LITTLE HORIZONS");
        lines.add("      FACTURA ELECTRÓNICA DE VENTA (SIMULADA)");
        lines.add(SEPARATOR);
        lines.add("NIT: " + CufeGenerator.SCHOOL_NIT + "-8        Régimen: Responsable de IVA");
        lines.add("Resolución DIAN No. 18764000000001 del 2026-01-01 (simulada)");
        lines.add("Rango autorizado: LH-000001 a LH-999999 - Vigencia: 12 meses");
        lines.add(LINE);
        lines.add("Factura No.:      " + invoice.getNumber());
        lines.add("Fecha de emisión: " + DateUtils.format(invoice.getIssueDate()));
        lines.add("Recibo de caja:   " + payment.getReceiptNumber());
        lines.add("Método de pago:   " + payment.getMethod().getLabel());
        lines.add(LINE);
        lines.add("CLIENTE (ACUDIENTE)");
        lines.add("Nombre:     " + guardian.getFullName());
        lines.add("Documento:  " + guardian.getDocumentType().getLabel() + " " + guardian.getDocumentNumber());
        lines.add("Dirección:  " + (guardian.getAddress() == null ? "-" : guardian.getAddress()));
        lines.add("Teléfono:   " + (guardian.getPrimaryPhone() == null ? "-" : guardian.getPrimaryPhone()));
        lines.add("Correo:     " + (guardian.getEmail() == null ? "-" : guardian.getEmail()));
        lines.add(LINE);
        lines.add("ESTUDIANTE");
        lines.add("Nombre:     " + payment.getStudentName());
        lines.add("Documento:  " + studentDocument);
        lines.add(LINE);
        lines.add("DETALLE");
        lines.add("Concepto:   " + payment.getConceptName() + " (" + payment.getConceptCode() + ")");
        lines.add("Año: " + payment.getYear() + "     Período: " + period);
        if (payment.getNotes() != null) {
            lines.add("Notas:      " + payment.getNotes());
        }
        lines.add(LINE);
        lines.add(String.format("%-40s %22s", "Valor antes de IVA", MoneyFormat.format(base)));
        lines.add(String.format("%-40s %22s", TaxPolicy.appliesVat(payment.getConceptCode())
                        ? "IVA " + TaxPolicy.VAT_RATE.movePointRight(2).intValue() + "%" : "IVA (servicio educativo excluido)",
                MoneyFormat.format(vat)));
        lines.add(String.format("%-40s %22s", "TOTAL PAGADO", MoneyFormat.format(total)));
        lines.add(SEPARATOR);
        lines.add("CUFE (SHA-384):");
        lines.add(invoice.getCufe().substring(0, 48));
        lines.add(invoice.getCufe().substring(48));
        lines.add("QR (simulado): https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey="
                + invoice.getCufe().substring(0, 16) + "...");
        lines.add(SEPARATOR);
        lines.add("Documento generado con fines académicos. No tiene validez fiscal.");
        return lines;
    }
}