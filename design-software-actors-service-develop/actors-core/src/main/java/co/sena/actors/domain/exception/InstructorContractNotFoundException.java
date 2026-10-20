package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 404 NOT_FOUND: instructor contract not found or soft-deleted.
 */
public class InstructorContractNotFoundException extends RuntimeException {

    private final UUID contractId;

    public InstructorContractNotFoundException(UUID contractId) {
        super("Instructor contract not found: " + contractId);
        this.contractId = contractId;
    }

    public UUID getContractId() {
        return contractId;
    }
}