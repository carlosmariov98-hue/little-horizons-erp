package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.MoneyFormat;

import java.math.BigDecimal;
import java.util.List;

/** Console submenu to manage the catalog of payment concepts (conceptos de pago). */
public class PaymentConceptMenu extends BaseMenu {

    private final PaymentConceptController concepts;

    public PaymentConceptMenu(InputReader input, PaymentConceptController concepts) {
        super(input);
        this.concepts = concepts;
    }

    /** Reads a money amount typed with a dot as decimal separator (e.g. 350000 or 350000.50). */
    static BigDecimal readAmount(InputReader input, String prompt) throws ErpException {
        return input.readValid(prompt, text -> {
            try {
                return PaymentConceptController.validateAmount(new BigDecimal(text), "Monto");
            } catch (NumberFormatException e) {
                throw new ValidationException("Ingrese un monto válido, sin separador de miles (ejemplo: 350000).");
            }
        });
    }

    public void run() {
        boolean back = false;
        while (!back) {
            Console.title("CONCEPTOS DE PAGO");
            Console.info("1. Listar conceptos");
            Console.info("2. Crear un concepto");
            Console.info("3. Cambiar el valor base de un concepto");
            Console.info("4. Activar o desactivar un concepto");
            Console.info("0. Volver");
            switch (input.readMenuOption("Seleccione una opción: ", 0, 4)) {
                case 1:
                    runSafely(this::list);
                    break;
                case 2:
                    runSafely(this::create);
                    break;
                case 3:
                    runSafely(this::changeAmount);
                    break;
                case 4:
                    runSafely(this::toggleActive);
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private void list() throws ErpException {
        List<PaymentConcept> all = concepts.listAll();
        Console.title("CONCEPTOS DE PAGO (" + all.size() + ")");
        for (PaymentConcept c : all) {
            Console.info(String.format("%3d | %-14s | %-28s | %18s | %-10s | %s", c.getConceptId(), c.getCode(),
                    c.getName(), MoneyFormat.format(c.getBaseAmount()), c.isRecurring() ? "Recurrente" : "Único",
                    c.isActive() ? "Activo" : "Inactivo"));
        }
    }

    private void create() throws ErpException {
        Console.title("CREAR UN CONCEPTO");
        String code = input.readValid("Código (ejemplo: EXCURSION): ", text -> text);
        String name = input.readValid("Nombre: ", text -> text);
        String description = input.readLine("Descripción (Enter = ninguna): ");
        BigDecimal amount = readAmount(input, "Valor base: ");
        boolean recurring = input.readYesNo("¿Es un cobro recurrente por período (como la pensión)? (s/n): ");
        int id = concepts.createConcept(code, name, description, amount, recurring);
        Console.success("Concepto creado con ID " + id + ".");
    }

    private PaymentConcept chooseConcept() throws ErpException {
        return input.readChoice("Concepto:", concepts.listAll(),
                c -> c.getName() + " (" + c.getCode() + ") - " + MoneyFormat.format(c.getBaseAmount())
                        + (c.isActive() ? "" : " [Inactivo]"));
    }

    private void changeAmount() throws ErpException {
        PaymentConcept concept = chooseConcept();
        Console.info("Valor actual: " + MoneyFormat.format(concept.getBaseAmount()));
        BigDecimal amount = readAmount(input, "Nuevo valor base: ");
        concepts.changeBaseAmount(concept.getConceptId(), amount);
        Console.success("Valor base actualizado. Los pagos ya registrados no cambian.");
    }

    private void toggleActive() throws ErpException {
        PaymentConcept concept = chooseConcept();
        boolean target = !concept.isActive();
        if (input.readYesNo((target ? "¿Activar" : "¿Desactivar") + " el concepto " + concept.getName() + "? (s/n): ")) {
            concepts.changeActive(concept.getConceptId(), target);
            Console.success("Concepto " + (target ? "activado" : "desactivado") + ".");
        } else {
            Console.info("No se realizó ningún cambio.");
        }
    }
}