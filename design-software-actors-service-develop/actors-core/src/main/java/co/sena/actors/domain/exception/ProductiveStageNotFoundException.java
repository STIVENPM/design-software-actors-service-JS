package co.sena.actors.domain.exception;

public class ProductiveStageNotFoundException extends RuntimeException {
    public ProductiveStageNotFoundException() {
        super("Productive stage not found");
    }
}
