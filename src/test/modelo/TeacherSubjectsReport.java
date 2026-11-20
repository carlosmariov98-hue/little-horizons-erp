package test.modelo;

import java.util.List;

/**
 * The information of one report: a teacher, the academic year and the subjects assigned to that teacher
 * in that year. It is what the exporter writes to the file.
 */
public class TeacherSubjectsReport {

    private final TeacherInfo teacher;
    private final int year;
    private final List<AssignedSubject> subjects;

    public TeacherSubjectsReport(TeacherInfo teacher, int year, List<AssignedSubject> subjects) {
        this.teacher = teacher;
        this.year = year;
        this.subjects = List.copyOf(subjects);
    }

    public TeacherInfo getTeacher() {
        return teacher;
    }

    public int getYear() {
        return year;
    }

    public List<AssignedSubject> getSubjects() {
        return subjects;
    }
}