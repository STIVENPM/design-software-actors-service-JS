package co.sena.actors.application.usecase;

public class FichaNotFoundException extends RuntimeException {

    public FichaNotFoundException() {
        super("Ficha not found");
    }
}
