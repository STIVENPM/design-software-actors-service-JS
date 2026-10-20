package co.sena.actors.application.usecase;

public class FichaLookupUnavailableException extends RuntimeException {

    public FichaLookupUnavailableException() {
        super("Ficha lookup unavailable");
    }

    public FichaLookupUnavailableException(Throwable cause) {
        super("Ficha lookup unavailable", cause);
    }
}
