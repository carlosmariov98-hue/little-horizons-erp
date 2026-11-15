package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.util.AppLogger;

import java.util.NoSuchElementException;

/**
 * Behaviour shared by all menus. Its main job is running an action safely: whatever goes wrong,
 * the user sees a clear message and returns to the menu instead of the program crashing.
 */
public abstract class BaseMenu {

    /**
     * One action a menu option can execute.
     */
    @FunctionalInterface
    protected interface MenuAction {
        void run() throws ErpException;
    }

    protected final InputReader input;

    protected BaseMenu(InputReader input) {
        this.input = input;
    }

    protected void runSafely(MenuAction action) {
        try {
            action.run();
        } catch (OperationCancelledException e) {
            Console.info("Operación cancelada. No se guardó ningún cambio.");
        } catch (ErpException e) {
            Console.error(e.getMessage());
        } catch (NoSuchElementException e) {
            throw e; // the input stream was closed: let the application end
        } catch (RuntimeException e) {
            AppLogger.getInstance().error("Unexpected error while running a menu option.", e);
            Console.error("Ocurrió un error inesperado. Consulte error_log.txt para más detalles.");
        }
    }
}
