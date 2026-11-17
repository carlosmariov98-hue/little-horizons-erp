package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** An active student who has not paid tuition recently. The last payment date is null if there is none. */
public class DelinquentStudent {

    private final Student student;
    private final LocalDate lastPaymentDate;

    public DelinquentStudent(Student student, LocalDate lastPaymentDate) {
        this.student = student;
        this.lastPaymentDate = lastPaymentDate;
    }

    public Student getStudent() { return student; }
    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
}