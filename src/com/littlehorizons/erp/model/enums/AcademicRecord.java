package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AcademicPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade of a student in a subject, year and period (table "registros_academicos"). */
public class AcademicRecord {

    private int recordId;
    private int studentId;
    private String studentName;
    private int subjectId;
    private String subjectName;
    private int teacherId;
    private String teacherName;
    private int year;
    private AcademicPeriod period;
    private BigDecimal score;
    private String observations;
    private LocalDate recordDate;

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public AcademicPeriod getPeriod() { return period; }
    public void setPeriod(AcademicPeriod period) { this.period = period; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
}
