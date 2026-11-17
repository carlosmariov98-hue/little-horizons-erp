package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The loan of a resource to a student or a teacher (tables "prestamos_estudiante" and "prestamos_profesor").
 * The id of a loan is only unique inside its borrower type, so a loan is identified by type and id together.
 */
public class Loan {

    private int loanId;
    private BorrowerType borrowerType;
    private int resourceId;
    private String resourceCode;
    private String resourceName;
    private int borrowerId;
    private String borrowerName;
    private LocalDate loanDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private LoanStatus status = LoanStatus.ACTIVE;
    private String notes;

    public int getLoanId() { return loanId; }
    public void setLoanId(int loanId) { this.loanId = loanId; }
    public BorrowerType getBorrowerType() { return borrowerType; }
    public void setBorrowerType(BorrowerType borrowerType) { this.borrowerType = borrowerType; }
    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public int getBorrowerId() { return borrowerId; }
    public void setBorrowerId(int borrowerId) { this.borrowerId = borrowerId; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }
    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    public LocalDate getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /** Days after the expected date that the loan has been open (or was returned); 0 when it is not late. */
    public long getDaysLate(LocalDate today) {
        LocalDate end = actualReturnDate != null ? actualReturnDate : today;
        return Math.max(0, ChronoUnit.DAYS.between(expectedReturnDate, end));
    }
}