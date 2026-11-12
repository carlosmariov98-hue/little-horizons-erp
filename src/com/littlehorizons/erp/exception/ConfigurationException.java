package com.littlehorizons.erp.exception;

/**
 * Thrown when the application configuration file is missing or invalid.
 */
public class ConfigurationException extends ErpException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}