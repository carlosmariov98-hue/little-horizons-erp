package test.controlador;

import com.littlehorizons.erp.exception.FileOperationException;
import com.littlehorizons.erp.util.AppLogger;
import test.modelo.AssignedSubject;
import test.modelo.TeacherSubjectsReport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the text of the report and writes it to a plain text (TXT) file. Columns are aligned with spaces
 * (never tabs), so the file looks the same in any text editor.
 */
public class TeacherSubjectsExporter {

    private static final int WIDTH = 79;
    private static final String DOUBLE_LINE = "=".repeat(WIDTH);
    private static final String SINGLE_LINE = "-".repeat(WIDTH);
    private static final String SIGNATURE_LINE = "_".repeat(29);
    // minimum widths of the columns, as in the example of the specification (the grade starts at column 45)
    private static final int MIN_CODE_WIDTH = 10;
    private static final int MIN_SUBJECT_WIDTH = 34;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Path directory;

    /**
     * @param directory folder where the file is written; an empty path means the project root folder
     */
    public TeacherSubjectsExporter(Path directory) {
        this.directory = directory;
    }

    /**
     * Name of the file of a teacher and year, for example materias_profesor_5_2025.txt.
     */
    public static String fileNameOf(int teacherId, int year) {
        return "materias_profesor_" + teacherId + "_" + year + ".txt";
    }

    /**
     * Writes (or overwrites) the report file. The file is completely written and closed before this method returns.
     *
     * @param report the information to write
     * @return the full path of the file
     * @throws FileOperationException if the file cannot be created or written
     */
    public Path export(TeacherSubjectsReport report) throws FileOperationException {
        Path file = directory.resolve(fileNameOf(report.getTeacher().getTeacherId(), report.getYear()))
                .toAbsolutePath().normalize();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String line : buildContent(report, LocalDate.now())) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            AppLogger.getInstance().error("Could not write teacher subjects file " + file + ".", e);
            throw new FileOperationException("No se pudo crear el archivo de exportación.", e);
        }
        AppLogger.getInstance().info("Teacher subjects file written: " + file + ".");
        return file;
    }

    // every line of the file, in order (package-private so it can be checked without writing a file)
    List<String> buildContent(TeacherSubjectsReport report, LocalDate generatedOn) {
        List<String> lines = new ArrayList<>();
        lines.add(DOUBLE_LINE);
        lines.add("           COLEGIO INTERNACIONAL \"LITTLE HORIZONS\"");
        lines.add("              REPORTE DE MATERIAS ASIGNADAS - AÑO " + report.getYear());
        lines.add(DOUBLE_LINE);
        lines.add("");
        lines.add("DATOS DEL PROFESOR");
        lines.add(SINGLE_LINE);
        lines.add(String.format("%-19s: %s", "Nombre completo", report.getTeacher().getFullName()));
        lines.add(String.format("%-19s: %s", "Documento", report.getTeacher().getDocument()));
        lines.add("");
        lines.add(DOUBLE_LINE);
        lines.add("                        MATERIAS ASIGNADAS");
        lines.add(DOUBLE_LINE);
        lines.add("");
        // a column grows only if some text is longer than its minimum width, so no text is ever cut
        int codeWidth = columnWidth(report, MIN_CODE_WIDTH, AssignedSubject::getCode);
        int subjectWidth = columnWidth(report, MIN_SUBJECT_WIDTH, AssignedSubject::getName);
        lines.add(row("Código", "Materia", "Grado", codeWidth, subjectWidth));
        lines.add(SINGLE_LINE);
        report.getSubjects().forEach(subject -> lines.add(row(subject.getCode(), subject.getName(),
                subject.getGradeName(), codeWidth, subjectWidth)));
        lines.add("");
        lines.add(DOUBLE_LINE);
        lines.add("");
        lines.add("Documento generado el: " + generatedOn.format(DATE_FORMAT));
        lines.add("");
        lines.add("");
        lines.add(SIGNATURE_LINE);
        lines.add("Recursos Humanos");
        lines.add("Colegio Internacional \"Little Horizons\"");
        return lines;
    }

    // width of a column: its minimum, or the longest text plus one space so it never touches the next column
    private static int columnWidth(TeacherSubjectsReport report, int minimum, Function<AssignedSubject, String> text) {
        int longest = report.getSubjects().stream().map(text).mapToInt(String::length).max().orElse(0);
        return Math.max(minimum, longest + 1);
    }

    // the grade is the last column, so it is not padded (no trailing spaces)
    private static String row(String code, String subject, String grade, int codeWidth, int subjectWidth) {
        return String.format("%-" + codeWidth + "s%-" + subjectWidth + "s%s", code, subject, grade);
    }
}

