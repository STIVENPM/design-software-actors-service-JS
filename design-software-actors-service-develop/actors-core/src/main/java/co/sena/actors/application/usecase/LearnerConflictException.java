package co.sena.actors.application.usecase;

public class LearnerConflictException extends RuntimeException {
    public LearnerConflictException(String message) {
        super(message);
    }
}
