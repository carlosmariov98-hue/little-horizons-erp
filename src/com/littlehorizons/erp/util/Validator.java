package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ValidationException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Reusable input validations. Every method returns the cleaned value (trimmed) or throws a
 * ValidationException whose message is written for the end user, in Spanish.
 */
public final class Validator {

    private static final List<String> BLOOD_TYPES = Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private Validator() {
    }

    public static <T> T requireNotNull(T value, String label) throws ValidationException {
        if (value == null) {
            throw new ValidationException("El campo '" + label + "' es obligatorio.");
        }
        return value;
    }

    public static String requireText(String value, String label, int maxLength) throws ValidationException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ValidationException("El campo '" + label + "' es obligatorio.");
        }
        if (text.length() > maxLength) {
            throw new ValidationException("El campo '" + label + "' no puede superar los " + maxLength + " caracteres.");
        }
        return text;
    }

    /**
     * Validates optional text.
     *
     * @return the trimmed text, or null when it is empty
     */
    public static String optionalText(String value, String label, int maxLength) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requireText(value, label, maxLength);
    }

    public static String requireName(String value, String label) throws ValidationException {
        String text = requireText(value, label, 100);
        if (!text.matches("^\\p{L}[\\p{L} '\\-]*$")) {
            throw new ValidationException("El campo '" + label + "' solo admite letras, espacios, apóstrofes y guiones.");
        }
        return text;
    }

    public static String optionalName(String value, String label) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requireName(value, label);
    }

    public static String requireDocumentNumber(String value) throws ValidationException {
        String text = requireText(value, "Número de documento", 20);
        if (!text.matches("^[A-Za-z0-9]{5,20}$")) {
            throw new ValidationException("El campo 'Número de documento' debe tener entre 5 y 20 letras o números, "
                    + "sin espacios ni signos.");
        }
        return text.toUpperCase();
    }

    public static String requirePhone(String value, String label) throws ValidationException {
        String text = requireText(value, label, 20);
        if (!text.matches("^\\d{7,10}$")) {
            throw new ValidationException("El campo '" + label + "' debe tener entre 7 y 10 dígitos, sin espacios ni signos.");
        }
        return text;
    }

    public static String optionalPhone(String value, String label) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requirePhone(value, label);
    }

    public static String requireEmail(String value) throws ValidationException {
        String text = optionalEmail(value);
        if (text == null) {
            throw new ValidationException("El campo 'Correo electrónico' es obligatorio.");
        }
        return text;
    }

    public static int requireIntInRange(int value, int min, int max, String label) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException("El campo '" + label + "' debe estar entre " + min + " y " + max + ".");
        }
        return value;
    }

    public static String optionalEmail(String value) throws ValidationException {
        String text = optionalText(value, "Correo electrónico", 100);
        if (text != null && !text.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("El campo 'Correo electrónico' no tiene un formato válido "
                    + "(ejemplo: nombre@correo.com).");
        }
        return text;
    }

    public static String optionalBloodType(String value) throws ValidationException {
        String text = optionalText(value, "Tipo de sangre", 5);
        if (text == null) {
            return null;
        }
        text = text.toUpperCase();
        if (!BLOOD_TYPES.contains(text)) {
            throw new ValidationException("El campo 'Tipo de sangre' debe ser uno de: " + String.join(", ", BLOOD_TYPES) + ".");
        }
        return text;
    }

    public static LocalDate requirePastOrToday(LocalDate date, String label) throws ValidationException {
        requireNotNull(date, label);
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("El campo '" + label + "' no puede ser una fecha futura.");
        }
        return date;
    }
}