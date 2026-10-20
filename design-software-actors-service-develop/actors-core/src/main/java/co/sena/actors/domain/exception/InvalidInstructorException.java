package co.sena.actors.domain.exception;

/**
 * 422 UNPROCESSABLE_ENTITY: syntactically valid payload that violates
 * business rules on PUT/PATCH (e.g. negative default_max_hours_per_week).
 */
public class InvalidInstructorException extends RuntimeException {

    private final String field;

    public InvalidInstructorException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
