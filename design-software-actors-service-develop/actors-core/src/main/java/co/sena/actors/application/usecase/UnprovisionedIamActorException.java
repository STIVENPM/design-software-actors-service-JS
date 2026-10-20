package co.sena.actors.application.usecase;

import java.util.UUID;

/** No actor profile can be materialized without the required profile facts. */
public class UnprovisionedIamActorException extends RuntimeException {
    public UnprovisionedIamActorException(UUID userId) {
        super("No provisioned actor profile for IAM user_id " + userId);
    }
}
