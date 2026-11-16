package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import java.time.LocalDate;

/** Registration of a student in a subject for a year and period (table "inscripciones_materia"). */
public class SubjectEnrollment {

    private int id;
    private int studentId;
    private int subjectId;
    private String subjectName;
    private int year;
    private AcademicPeriod period;
    private String section;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public AcademicPeriod getPeriod() { return period; }
    public void setPeriod(AcademicPeriod period) { this.period = period; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
}
