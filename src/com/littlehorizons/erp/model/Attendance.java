package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AttendanceStatus;

import java.time.LocalDate;

/** Attendance of a student in one subject on one day (table "asistencias"). */
public class Attendance {

    private int attendanceId;
    private int studentId;
    private String studentName;
    private int subjectId;
    private String subjectName;
    private LocalDate date;
    private AttendanceStatus status;
    private String observations;
    private int registeredBy;

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public int getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(int registeredBy) { this.registeredBy = registeredBy; }
}