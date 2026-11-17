package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** An electronic invoice (simulated) issued for a payment (table "facturas"). */
public class Invoice {

    private int invoiceId;
    private String number;
    private int paymentId;
    private LocalDate issueDate;
    private String cufe;
    private String fileName;
    private String filePath;

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getCufe() { return cufe; }
    public void setCufe(String cufe) { this.cufe = cufe; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
