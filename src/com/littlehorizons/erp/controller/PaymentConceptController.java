package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business logic for the catalog of payment concepts (tuition, uniforms, transport...).
 */
public class PaymentConceptController {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final PaymentConceptDao conceptDao;

    public PaymentConceptController(PaymentConceptDao conceptDao) {
        this.conceptDao = conceptDao;
    }

    /**
     * Validates a money amount: greater than zero, at most two decimals and at most 99.999.999,99.
     *
     * @param amount the amount to check
     * @param label  name of the field, used in the error message
     * @return the amount with exactly two decimals
     * @throws ValidationException if the amount is not valid
     */
    public static BigDecimal validateAmount(BigDecimal amount, String label) throws ValidationException {
        Validator.requireNotNull(amount, label);
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("El campo '" + label + "' debe ser mayor que cero, con máximo dos decimales "
                    + "(ejemplo: 350000 o 350000.50).");
        }
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    /**
     * Creates a new concept.
     *
     * @return the generated id
     * @throws ErpException if the data is invalid, the code is already used, or the database fails
     */
    public int createConcept(String code, String name, String description, BigDecimal baseAmount, boolean recurring)
            throws ErpException {
        PaymentConcept concept = new PaymentConcept();
        String cleanCode = Validator.requireText(code, "Código", 20).toUpperCase();
        if (!cleanCode.matches("^[A-Z0-9_]+$")) {
            throw new ValidationException("El código solo admite letras, números y guion bajo (ejemplo: EXCURSION).");
        }
        concept.setCode(cleanCode);
        concept.setName(Validator.requireText(name, "Nombre", 100));
        concept.setDescription(Validator.optionalText(description, "Descripción", 500));
        concept.setBaseAmount(validateAmount(baseAmount, "Valor base"));
        concept.setRecurring(recurring);
        concept.setActive(true);
        return conceptDao.insert(concept);
    }

    /**
     * Changes the base amount of a concept. Payments already registered keep the amount they were paid with.
     *
     * @throws ErpException if the amount is invalid, the concept does not exist, or the database fails
     */
    public void changeBaseAmount(int conceptId, BigDecimal newAmount) throws ErpException {
        BigDecimal amount = validateAmount(newAmount, "Valor base");
        PaymentConcept concept = conceptDao.findById(conceptId);
        if (concept.getBaseAmount().compareTo(amount) == 0) {
            throw new BusinessRuleException("El concepto ya tiene ese valor base.");
        }
        conceptDao.updateBaseAmount(conceptId, amount);
    }

    /**
     * Activates or deactivates a concept. An inactive concept can no longer be charged.
     *
     * @throws ErpException if the concept does not exist, already has that status, or the database fails
     */
    public void changeActive(int conceptId, boolean active) throws ErpException {
        PaymentConcept concept = conceptDao.findById(conceptId);
        if (concept.isActive() == active) {
            throw new BusinessRuleException("El concepto ya se encuentra " + (active ? "activo" : "inactivo") + ".");
        }
        conceptDao.changeActive(conceptId, active);
    }

    public PaymentConcept getById(int conceptId) throws DatabaseException, EntityNotFoundException {
        return conceptDao.findById(conceptId);
    }

    public List<PaymentConcept> listAll() throws DatabaseException {
        return conceptDao.findAll();
    }

    public List<PaymentConcept> listActive() throws DatabaseException {
        return conceptDao.findActive();
    }
}