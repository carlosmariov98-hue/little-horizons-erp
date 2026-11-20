package test.controlador;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.FileOperationException;
import test.modelo.AssignedSubject;
import test.modelo.TeacherInfo;
import test.modelo.TeacherSubjectsReport;

import java.nio.file.Path;
import java.util.List;

/**
 * Logic of the exporter: checks that the teacher exists and has subjects in the academic year,
 * and asks the exporter to write the file. It does not read or print anything on the console.
 */
public class TeacherSubjectsController {

    /** Academic year of the report. It is the only place where the year is defined. */
    public static final int ACADEMIC_YEAR = 2025;

    private final TeacherSubjectsQuery query;
    private final TeacherSubjectsExporter exporter;

    public TeacherSubjectsController(TeacherSubjectsQuery query, TeacherSubjectsExporter exporter) {
        this.query = query;
        this.exporter = exporter;
    }

    /**
     * Collects the information of the report of a teacher.
     *
     * @param teacherId id of the teacher
     * @return the teacher with the subjects assigned in the academic year
     * @throws EntityNotFoundException if there is no teacher with that id
     * @throws BusinessRuleException   if the teacher has no subjects assigned in the academic year
     * @throws DatabaseException       if a query fails
     */
    public TeacherSubjectsReport loadReport(int teacherId)
            throws EntityNotFoundException, BusinessRuleException, DatabaseException {
        TeacherInfo teacher = query.findTeacher(teacherId).orElseThrow(
                () -> new EntityNotFoundException("No se encontró ningún profesor con el ID " + teacherId + "."));
        List<AssignedSubject> subjects = query.findAssignedSubjects(teacherId, ACADEMIC_YEAR);
        if (subjects.isEmpty()) {
            throw new BusinessRuleException("El profesor " + teacher.getFullName()
                    + " no tiene materias asignadas" + System.lineSeparator()
                    + "para el año académico " + ACADEMIC_YEAR + ".");
        }
        return new TeacherSubjectsReport(teacher, ACADEMIC_YEAR, subjects);
    }

    /**
     * Writes the file of a report.
     *
     * @param report the report to write
     * @return the full path of the written file
     * @throws FileOperationException if the file cannot be written
     */
    public Path exportToFile(TeacherSubjectsReport report) throws FileOperationException {
        return exporter.export(report);
    }
}