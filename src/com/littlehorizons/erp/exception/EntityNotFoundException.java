package com.littlehorizons.erp.exception;

/**
 * Thrown when a requested record does not exist in the database.
 */
public class EntityNotFoundException extends ErpException {

    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
