package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** A concept the school charges for, such as tuition or uniforms (table "conceptos_pago"). */
public class PaymentConcept {

    private int conceptId;
    private String code;
    private String name;
    private String description;
    private BigDecimal baseAmount;
    private boolean recurring;
    private boolean active = true;

    public int getConceptId() { return conceptId; }
    public void setConceptId(int conceptId) { this.conceptId = conceptId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}