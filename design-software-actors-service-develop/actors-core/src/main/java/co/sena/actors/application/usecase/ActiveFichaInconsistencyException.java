package co.sena.actors.application.usecase;

public class ActiveFichaInconsistencyException extends RuntimeException {

    public ActiveFichaInconsistencyException(String message) {
        super(message);
    }
}