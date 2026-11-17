package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.util.AppLogger;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Behaviour shared by all menus. Its main job is running an action safely: whatever goes wrong,
 * the user sees a clear message and returns to the menu instead of the program crashing.
 */
public abstract class BaseMenu {

    /** One action a menu option can execute. */
    @FunctionalInterface
    protected interface MenuAction {
        void run() throws ErpException;
    }

    protected static final List<AcademicPeriod> REGULAR_PERIODS = Arrays.asList(AcademicPeriod.FIRST,
            AcademicPeriod.SECOND, AcademicPeriod.THIRD, AcademicPeriod.FOURTH);

    protected final InputReader input;

    protected BaseMenu(InputReader input) {
        this.input = input;
    }

    protected void runSafely(MenuAction action) {
        try {
            action.run();
        } catch (OperationCancelledException e) {
            AppLogger.getInstance().info("Operation cancelled by the user.");
            Console.info("Operación cancelada. No se guardó ningún cambio.");
        } catch (ErpException e) {
            // database errors were already logged where they happened; the rest are rejected actions
            if (!(e instanceof DatabaseException)) {
                AppLogger.getInstance().warn("Operation rejected: " + e.getClass().getSimpleName() + ".");
            }
            Console.error(e.getMessage());
        } catch (NoSuchElementException e) {
            throw e; // the input stream was closed: let the application end
        } catch (RuntimeException e) {
            AppLogger.getInstance().error("Unexpected error while running a menu option.", e);
            Console.error("Ocurrió un error inesperado. Consulte error_log.txt para más detalles.");
        }
    }

    protected int readId(String prompt) throws ErpException {
        return input.readInt(prompt, 1, Integer.MAX_VALUE);
    }

    protected int readYear() throws ErpException {
        int current = LocalDate.now().getYear();
        return input.readValid("Año académico (Enter = " + current + "): ", text -> {
            if (text.isEmpty()) {
                return current;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new ValidationException("Ingrese un año válido, por ejemplo " + current + ".");
            }
        });
    }

    protected AcademicPeriod readPeriod() throws ErpException {
        return input.readChoice("Período:", REGULAR_PERIODS, AcademicPeriod::getLabel);
    }

    // asks for a text value showing the current one; Enter keeps it and '-' clears an optional value
    protected String readKeepText(String label, String current, InputReader.Parser<String> parser, boolean optional)
            throws ErpException {
        return input.readValid(label + " [" + dash(current) + "]: ", text -> {
            if (text.isEmpty()) {
                return current;
            }
            if (optional && text.equals("-")) {
                return null;
            }
            return parser.parse(text);
        });
    }

    protected static String dash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}