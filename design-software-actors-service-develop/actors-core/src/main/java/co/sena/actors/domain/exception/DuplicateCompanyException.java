package co.sena.actors.domain.exception;

public class DuplicateCompanyException extends RuntimeException {
    public DuplicateCompanyException() {
        super("Company NIT already exists");
    }
}
