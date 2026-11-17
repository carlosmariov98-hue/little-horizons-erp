package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A payment made for a student (table "pagos"). A paymentId of 0 means it is not saved yet. */
public class Payment {

    private int paymentId;
    private int studentId;
    private String studentName;
    private int guardianId;
    private String guardianName;
    private int conceptId;
    private String conceptCode;
    private String conceptName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod method;
    private String receiptNumber;
    private int year;
    private PaymentPeriod period;
    private String notes;
    private String invoiceNumber;

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getGuardianId() { return guardianId; }
    public void setGuardianId(int guardianId) { this.guardianId = guardianId; }
    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public int getConceptId() { return conceptId; }
    public void setConceptId(int conceptId) { this.conceptId = conceptId; }
    public String getConceptCode() { return conceptCode; }
    public void setConceptCode(String conceptCode) { this.conceptCode = conceptCode; }
    public String getConceptName() { return conceptName; }
    public void setConceptName(String conceptName) { this.conceptName = conceptName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public PaymentPeriod getPeriod() { return period; }
    public void setPeriod(PaymentPeriod period) { this.period = period; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
}