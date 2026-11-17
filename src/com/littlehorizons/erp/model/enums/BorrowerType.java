package com.littlehorizons.erp.model.enums;

/**
 * Who borrows a resource. Students and teachers have separate loan tables, so each constant also knows the
 * names of its tables and columns. These names are constants written in this file, never user input,
 * which is why the DAO can safely put them inside its SQL text.
 */
public enum BorrowerType {

    STUDENT("Estudiante", "prestamos_estudiante", "estudiantes", "id_estudiante"),
    TEACHER("Profesor", "prestamos_profesor", "profesores", "id_profesor");

    private final String label;
    private final String loanTable;
    private final String borrowerTable;
    private final String idColumn;

    BorrowerType(String label, String loanTable, String borrowerTable, String idColumn) {
        this.label = label;
        this.loanTable = loanTable;
        this.borrowerTable = borrowerTable;
        this.idColumn = idColumn;
    }

    public String getLabel() {
        return label;
    }

    public String getLoanTable() {
        return loanTable;
    }

    public String getBorrowerTable() {
        return borrowerTable;
    }

    public String getIdColumn() {
        return idColumn;
    }
}