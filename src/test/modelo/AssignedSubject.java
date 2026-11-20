package test.modelo;

/**
 * A subject assigned to a teacher, with the grade it belongs to.
 */
public class AssignedSubject {

    private final String code;
    private final String name;
    private final String gradeName;

    public AssignedSubject(String code, String name, String gradeName) {
        this.code = code;
        this.name = name;
        this.gradeName = gradeName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getGradeName() {
        return gradeName;
    }
}