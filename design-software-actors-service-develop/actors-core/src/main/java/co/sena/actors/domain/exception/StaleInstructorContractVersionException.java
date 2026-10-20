package co.sena.actors.domain.exception;

import java.util.UUID;

/**
 * 409 CONFLICT: optimistic locking conflict on instructor contract.
 */
public class StaleInstructorContractVersionException extends RuntimeException {

    private final UUID contractId;

    public StaleInstructorContractVersionException(UUID contractId) {
        super("Concurrency conflict: stale row_version for instructor contract " + contractId);
        this.contractId = contractId;
    }

    public UUID getContractId() {
        return contractId;
    }
}