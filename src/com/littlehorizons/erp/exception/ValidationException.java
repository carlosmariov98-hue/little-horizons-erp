package com.littlehorizons.erp.exception;

/**
 * Thrown when user input does not meet the validation rules.
 */
public class ValidationException extends ErpException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}