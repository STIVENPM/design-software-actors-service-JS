package co.sena.actors.domain.exception;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException() {
        super("Company not found");
    }
}
