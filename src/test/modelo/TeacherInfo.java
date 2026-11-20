package test.modelo;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic data of a teacher that the exporter needs: the identity document and the names.
 */
public class TeacherInfo {

    private final int teacherId;
    private final String documentType;
    private final String documentNumber;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String secondLastName;

    public TeacherInfo(int teacherId, String documentType, String documentNumber, String firstName,
                       String middleName, String lastName, String secondLastName) {
        this.teacherId = teacherId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.secondLastName = secondLastName;
    }

    public int getTeacherId() {
        return teacherId;
    }

    /**
     * Document type and number, for example "CC 80123456".
     */
    public String getDocument() {
        return documentType + " " + documentNumber;
    }

    /**
     * Full name built only with the names that exist, separated by one space. A teacher without
     * middle name or second last name does not leave a double space.
     */
    public String getFullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}