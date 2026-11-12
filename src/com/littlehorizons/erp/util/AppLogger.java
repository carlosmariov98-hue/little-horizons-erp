package com.littlehorizons.erp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Application logger (Singleton). Appends entries to {@value #LOG_FILE} in the working directory.
 * Logging must never break the application: if the file cannot be written,
 * the problem is reported on the error console and the program continues.
 */
public final class AppLogger {

    public static final String LOG_FILE = "error_log.txt";

    private static final AppLogger INSTANCE = new AppLogger();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private enum Level { INFO, WARN, ERROR }

    private AppLogger() {
    }

    /**
     * Returns the single logger instance.
     *
     * @return the shared logger
     */
    public static AppLogger getInstance() {
        return INSTANCE;
    }

    public void info(String message) {
        write(Level.INFO, message, null);
    }

    public void warn(String message) {
        write(Level.WARN, message, null);
    }

    public void error(String message) {
        write(Level.ERROR, message, null);
    }

    /**
     * Logs an error together with the full stack trace of its cause.
     *
     * @param message description of what failed
     * @param cause   the exception that was caught
     */
    public void error(String message, Throwable cause) {
        write(Level.ERROR, message, cause);
    }

    // synchronized: several threads may log at the same time and entries must not get mixed
    private synchronized void write(Level level, String message, Throwable cause) {
        StringBuilder entry = new StringBuilder();
        entry.append(LocalDateTime.now().format(TIMESTAMP_FORMAT))
                .append(" [").append(level).append("] ")
                .append(message)
                .append(System.lineSeparator());
        if (cause != null) {
            StringWriter stackTrace = new StringWriter();
            try (PrintWriter printWriter = new PrintWriter(stackTrace)) {
                cause.printStackTrace(printWriter);
            }
            entry.append(stackTrace);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(LOG_FILE), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(entry.toString());
        } catch (IOException e) {
            System.err.println("Could not write to " + LOG_FILE + ": " + e.getMessage());
        }
    }
}