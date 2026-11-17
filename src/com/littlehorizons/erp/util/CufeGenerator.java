package com.littlehorizons.erp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Generates the CUFE (Código Único de Factura Electrónica) of an invoice.
 * In this academic project it is simulated: the SHA-384 hash of invoice number + school NIT + issue date.
 */
public final class CufeGenerator {

    /** NIT of the school (simulated). */
    public static final String SCHOOL_NIT = "900123456";

    private static final String ALGORITHM = "SHA-384";

    private CufeGenerator() {
    }

    /**
     * Calculates the CUFE of an invoice.
     *
     * @param invoiceNumber invoice number, for example LH-000001
     * @param issueDate     issue date, written as yyyy-MM-dd in the hash
     * @return the 96-character hexadecimal SHA-384 hash
     * @throws IllegalStateException if the JVM does not provide SHA-384 (it always does in Java 17)
     */
    public static String generate(String invoiceNumber, LocalDate issueDate) {
        String source = invoiceNumber + SCHOOL_NIT + issueDate;
        try {
            byte[] hash = MessageDigest.getInstance(ALGORITHM).digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The " + ALGORITHM + " algorithm is not available.", e);
        }
    }
}