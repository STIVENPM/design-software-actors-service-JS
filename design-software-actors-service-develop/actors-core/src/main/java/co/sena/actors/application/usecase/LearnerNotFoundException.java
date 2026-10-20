package co.sena.actors.application.usecase;

public class LearnerNotFoundException extends RuntimeException {
    public LearnerNotFoundException(String message) {
        super(message);
    }
}
