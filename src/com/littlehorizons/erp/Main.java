package com.littlehorizons.erp;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.EntityNotFoundException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * TEMPORARY class used to test StudentDao. It will be replaced by the real entry point.
 * Every record it creates is deleted at the end.
 */
public class Main {

    private static final String TEST_DOCUMENT = "9990000001";

    public static void main(String[] args) {
        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            StudentDao dao = new StudentDao(database);
            try {
                runTests(dao);
            } finally {
                cleanUp(database);
            }
        } catch (ErpException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static void runTests(StudentDao dao) throws ErpException {
        check("findAll returns the 50 students of the dataset", dao.findAll().size() == 50);
        Student first = dao.findById(1);
        System.out.println("      student 1 -> " + first);
        check("findById(1) loads the enums and the dates", first.getDocumentType() != null
                && first.getStatus() != null && first.getBirthDate() != null);
        check("search 'Londo' finds the siblings", dao.search("Londo").size() >= 2);
        check("search is safe against SQL injection", dao.search("' OR '1'='1").isEmpty());
        check("findByStatus(GRADUATED) returns 2 students", dao.findByStatus(StudentStatus.GRADUATED).size() == 2);

        Student student = new Student();
        student.setDocumentType(DocumentType.IDENTITY_CARD);
        student.setDocumentNumber(TEST_DOCUMENT);
        student.setFirstName("Prueba");
        student.setMiddleName("");
        student.setFirstLastName("Dao");
        student.setSecondLastName("Temporal");
        student.setBirthDate(LocalDate.of(2012, 5, 20));
        student.setGender(Gender.OTHER);
        student.setAddress("Calle 1 # 2-3");
        student.setPhone("3001234567");
        student.setAdmissionDate(LocalDate.of(2026, 1, 19));
        student.setStatus(StudentStatus.ACTIVE);
        int newId = dao.insert(student);
        check("insert returns the generated id", newId > 50 && student.getStudentId() == newId);

        Student loaded = dao.findById(newId);
        check("empty middle name was saved as NULL", loaded.getMiddleName() == null);
        check("full name has no double spaces", "Prueba Dao Temporal".equals(loaded.getFullName()));

        loaded.setAddress("Carrera 9 # 8-7");
        dao.update(loaded);
        check("update changes the address", "Carrera 9 # 8-7".equals(dao.findById(newId).getAddress()));
        dao.update(loaded);
        check("update with identical data does not fail", true);

        dao.changeStatus(newId, StudentStatus.SUSPENDED);
        check("changeStatus works", dao.findById(newId).getStatus() == StudentStatus.SUSPENDED);

        check("existsByDocumentNumber detects the document", dao.existsByDocumentNumber(TEST_DOCUMENT));
        check("existsByDocumentNumber ignores the student itself", !dao.existsByDocumentNumber(TEST_DOCUMENT, newId));
        check("existsByDocumentNumber is false for unknown documents", !dao.existsByDocumentNumber("0000000000"));

        Student duplicate = dao.findById(newId);
        duplicate.setStudentId(0);
        try {
            dao.insert(duplicate);
            check("duplicate document is rejected", false);
        } catch (BusinessRuleException e) {
            System.out.println("      message -> " + e.getMessage());
            check("duplicate document is rejected with a business rule error", true);
        }

        try {
            dao.findById(999999);
            check("unknown id raises EntityNotFoundException", false);
        } catch (EntityNotFoundException e) {
            System.out.println("      message -> " + e.getMessage());
            check("unknown id raises EntityNotFoundException", true);
        }

        try {
            dao.changeStatus(999999, StudentStatus.ACTIVE);
            check("changeStatus on unknown id raises EntityNotFoundException", false);
        } catch (EntityNotFoundException e) {
            check("changeStatus on unknown id raises EntityNotFoundException", true);
        }

        Student incomplete = new Student();
        incomplete.setDocumentType(DocumentType.IDENTITY_CARD);
        incomplete.setDocumentNumber("9990000002");
        incomplete.setFirstName("Incompleto");
        incomplete.setFirstLastName("Sin Fecha");
        incomplete.setGender(Gender.MALE);
        incomplete.setStatus(StudentStatus.ACTIVE);
        try {
            dao.insert(incomplete);
            check("student without birth date is rejected", false);
        } catch (DatabaseException e) {
            System.out.println("      message -> " + e.getMessage());
            check("student without birth date raises DatabaseException (no crash)", true);
        }
    }

    private static void check(String description, boolean condition) {
        System.out.println((condition ? "[OK]   " : "[FAIL] ") + description);
    }

    private static void cleanUp(DatabaseConnection database) {
        String sql = "DELETE FROM estudiantes WHERE numero_documento IN ('9990000001', '9990000002')";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int deleted = statement.executeUpdate();
            System.out.println("Cleanup: " + deleted + " test record(s) deleted.");
        } catch (SQLException | DatabaseException e) {
            System.out.println("Cleanup failed. Delete the test student manually: " + e.getMessage());
        }
    }
}
