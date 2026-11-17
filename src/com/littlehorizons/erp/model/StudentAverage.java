package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.PerformanceLevel;

import java.math.BigDecimal;

/** The average score of one student in a group of grades, with its performance level (used by the reports). */
public class StudentAverage {

    private final String studentName;
    private final BigDecimal average;
    private final PerformanceLevel level;

    public StudentAverage(String studentName, BigDecimal average) {
        this.studentName = studentName;
        this.average = average;
        this.level = PerformanceLevel.fromScore(average);
    }

    public String getStudentName() { return studentName; }
    public BigDecimal getAverage() { return average; }
    public PerformanceLevel getLevel() { return level; }
}