package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.FileOperationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Writes a report as a plain text (TXT) file inside the reports directory. */
public class ReportFileWriter {

    private static final String SEPARATOR = "=".repeat(60);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path directory;

    public ReportFileWriter(Path directory) {
        this.directory = directory;
    }

    /**
     * Writes (or overwrites) the general summary of the given day.
     *
     * @param title the title of the report
     * @param data  label and value of every line, in the order they must appear
     * @param day   day of the report; it is part of the file name
     * @return the path of the written file
     * @throws FileOperationException if the directory or the file cannot be written
     */
    public Path write(String title, Map<String, String> data, LocalDate day) throws FileOperationException {
        Path file = directory.resolve("resumen_general_" + day.format(FILE_DATE) + ".txt");
        List<String> lines = new ArrayList<>();
        lines.add(SEPARATOR);
        lines.add(title);
        lines.add("Fecha: " + DateUtils.format(day));
        lines.add(SEPARATOR);
        data.forEach((label, value) -> lines.add(label + ": " + value));
        try {
            Files.createDirectories(directory);
            Files.write(file, lines, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            AppLogger.getInstance().error("Could not write report file " + file + ".", e);
            throw new FileOperationException("No se pudo escribir el archivo del reporte en " + file + ".", e);
        }
    }
}