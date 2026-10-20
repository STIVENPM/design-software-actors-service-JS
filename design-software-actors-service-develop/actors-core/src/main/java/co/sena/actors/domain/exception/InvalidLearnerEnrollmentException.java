package co.sena.actors.domain.exception;

public class InvalidLearnerEnrollmentException extends RuntimeException {

    private final String field;

    public InvalidLearnerEnrollmentException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
