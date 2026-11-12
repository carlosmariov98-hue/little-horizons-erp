package com.littlehorizons.erp.exception;

/**
 * Thrown when a database operation fails. Wraps the original SQLException.
 */
public class DatabaseException extends ErpException {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}