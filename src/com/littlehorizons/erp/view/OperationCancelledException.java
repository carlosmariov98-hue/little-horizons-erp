package com.littlehorizons.erp.view;

/**
 * Thrown when the user types "cancelar" while a form is being filled in.
 * It is unchecked because it can happen at any prompt; the menus catch it and go back.
 */
public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Operation cancelled by the user.");
    }
}
