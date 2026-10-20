package co.sena.actors.domain.exception;

/**
 * 422 UNPROCESSABLE_ENTITY: business rule violation on contract (e.g. end_date < start_date).
 */
public class InvalidInstructorContractException extends RuntimeException {

    private final String field;

    public InvalidInstructorContractException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}