package com.littlehorizons.erp.exception;

/**
 * Thrown when an operation violates a business rule (e.g. club is full).
 */
public class BusinessRuleException extends ErpException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}