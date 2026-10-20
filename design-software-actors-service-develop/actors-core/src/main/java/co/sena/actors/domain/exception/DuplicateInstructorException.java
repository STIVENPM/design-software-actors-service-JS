package co.sena.actors.domain.exception;

/** 409 CONFLICT: duplicated document_number (or user_id). */
public class DuplicateInstructorException extends RuntimeException {

    private final String field;

    public DuplicateInstructorException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
