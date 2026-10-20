package co.sena.actors.adapter.in.event;

public class InvalidIamEventException extends RuntimeException {
    public InvalidIamEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
