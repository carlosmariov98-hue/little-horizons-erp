package com.littlehorizons.erp.exception;

/**
 * Base class for all checked exceptions of the ERP application.
 * Catching this type handles any application-specific error.
 */
public class ErpException extends Exception {

    private static final long serialVersionUID = 1L;

    public ErpException(String message) {
        super(message);
    }

    public ErpException(String message, Throwable cause) {
        super(message, cause);
    }
}