package com.littlehorizons.erp.exception;

/** A file could not be created, written or read (for example the TXT invoice). */
public class FileOperationException extends ErpException {

    private static final long serialVersionUID = 1L;

    public FileOperationException(String message) {
        super(message);
    }

    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}