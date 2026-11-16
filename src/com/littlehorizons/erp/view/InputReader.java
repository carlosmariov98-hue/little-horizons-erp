package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.util.DateUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Reads and validates what the user types. Invalid input shows a clear message and asks again.
 * Typing "cancelar" at any data prompt throws OperationCancelledException so the menu can go back.
 */
public class InputReader {

    private static final String CANCEL_WORD = "cancelar";

    /**
     * Converts the text typed by the user into a value, or rejects it with an exception.
     *
     * @param <T> type of the converted value
     */
    @FunctionalInterface
    public interface Parser<T> {
        T parse(String input) throws ErpException;
    }

    /**
     * Checks an option chosen from a list and rejects it with an exception when it is not acceptable.
     *
     * @param <E> type of the option
     */
    @FunctionalInterface
    public interface ChoiceValidator<E> {
        void validate(E choice) throws ErpException;
    }

    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    private String readRawLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Reads a line of text.
     *
     * @param prompt text shown to the user
     * @return the trimmed text
     * @throws OperationCancelledException if the user typed "cancelar"
     */
    public String readLine(String prompt) {
        String text = readRawLine(prompt);
        if (text.equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return text;
    }

    /**
     * Keeps asking until the parser accepts the typed text. Validation and business rule errors are
     * shown and the question is repeated; technical errors (such as a database failure) are passed on.
     *
     * @param prompt text shown to the user
     * @param parser converts and validates the typed text
     * @param <T>    type of the converted value
     * @return the accepted value
     * @throws ErpException if a technical error happens while parsing
     */
    public <T> T readValid(String prompt, Parser<T> parser) throws ErpException {
        while (true) {
            String text = readLine(prompt);
            try {
                return parser.parse(text);
            } catch (ValidationException | BusinessRuleException e) {
                Console.error(e.getMessage());
            }
        }
    }

    /**
     * Reads a menu option. Invalid entries are rejected and asked again.
     *
     * @param prompt text shown to the user
     * @param min    lowest valid option
     * @param max    highest valid option
     * @return the chosen option
     */
    public int readMenuOption(String prompt, int min, int max) {
        while (true) {
            String text = readRawLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // handled below: the same message is shown for any invalid entry
            }
            Console.error("Opción inválida. Ingrese un número entre " + min + " y " + max + ".");
        }
    }

    /**
     * Reads a whole number inside a range, asking again until it is valid.
     *
     * @param prompt text shown to the user
     * @param min    lowest valid value
     * @param max    highest valid value
     * @return the number typed by the user
     * @throws ErpException never in practice; required by the parser signature
     */
    public int readInt(String prompt, int min, int max) throws ErpException {
        return readValid(prompt, text -> {
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // handled below: the same message is shown for any invalid entry
            }
            throw new ValidationException("Ingrese un número entero entre " + min + " y " + max + ".");
        });
    }

    /**
     * Reads a date in dd/mm/yyyy format. An empty answer returns the default date.
     *
     * @param prompt      text shown to the user
     * @param defaultDate date used when the user just presses Enter (null to make the date mandatory)
     * @return the date
     * @throws ErpException never in practice; required by the parser signature
     */
    public LocalDate readDate(String prompt, LocalDate defaultDate) throws ErpException {
        return readValid(prompt, text -> {
            if (text.isEmpty() && defaultDate != null) {
                return defaultDate;
            }
            return DateUtils.parse(text);
        });
    }

    /**
     * Asks a yes/no question.
     *
     * @param prompt text shown to the user
     * @return true for yes (s, si, sí), false for no (n, no)
     */
    public boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt).toLowerCase();
            if (text.equals("s") || text.equals("si") || text.equals("sí")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            Console.error("Responda con 's' (sí) o 'n' (no).");
        }
    }

    /**
     * Shows a numbered list and returns the option the user picks.
     *
     * @param title   heading of the list
     * @param options the available options
     * @param label   converts an option into the text shown to the user
     * @param <E>     type of the options
     * @return the chosen option
     * @throws ErpException never in practice; required by the parser signature
     */
    public <E> E readChoice(String title, List<E> options, Function<E, String> label) throws ErpException {
        Console.info(title);
        for (int i = 0; i < options.size(); i++) {
            Console.info("  " + (i + 1) + ". " + label.apply(options.get(i)));
        }
        return readValid("Seleccione (1-" + options.size() + "): ", text -> {
            try {
                int index = Integer.parseInt(text);
                if (index >= 1 && index <= options.size()) {
                    return options.get(index - 1);
                }
            } catch (NumberFormatException e) {
                // handled below: the same message is shown for any invalid entry
            }
            throw new ValidationException("Opción inválida. Ingrese un número entre 1 y " + options.size() + ".");
        });
    }

    /**
     * Same as {@link #readChoice(String, List, Function)}, but each chosen option is also checked by a validator;
     * when it is rejected the message is shown and the list is offered again.
     *
     * @param title     heading of the list
     * @param options   the available options
     * @param label     converts an option into the text shown to the user
     * @param validator accepts the option or throws an exception explaining why not
     * @param <E>       type of the options
     * @return the chosen option
     * @throws ErpException if a technical error happens
     */
    public <E> E readChoice(String title, List<E> options, Function<E, String> label,
                            ChoiceValidator<E> validator)
            throws ErpException {
        while (true) {
            E choice = readChoice(title, options, label);
            try {
                validator.validate(choice);
                return choice;
            } catch (ValidationException | BusinessRuleException e) {
                Console.error(e.getMessage());
            }
        }
    }

    public void pause() {
        readRawLine("Presione Enter para continuar...");
    }
}