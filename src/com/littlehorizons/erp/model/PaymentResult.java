package com.littlehorizons.erp.model;

import java.nio.file.Path;

/**
 * Result of registering a payment: the saved payment and invoice, and the TXT file when it could be written.
 * If the file failed, the payment and the invoice are still saved and the warning explains how to retry.
 */
public class PaymentResult {

    private final Payment payment;
    private final Invoice invoice;
    private final Path file;
    private final String fileWarning;

    public PaymentResult(Payment payment, Invoice invoice, Path file, String fileWarning) {
        this.payment = payment;
        this.invoice = invoice;
        this.file = file;
        this.fileWarning = fileWarning;
    }

    public Payment getPayment() { return payment; }
    public Invoice getInvoice() { return invoice; }
    public Path getFile() { return file; }
    public String getFileWarning() { return fileWarning; }
}
