package co.sena.actors.adapter.in.event;

import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamUserDeactivated;
import co.sena.actors.application.usecase.IamUserSyncUseCase;
import org.springframework.transaction.annotation.Transactional;

/** One PostgreSQL transaction includes inbox, profile, dependents and activity_log. */
public class IamEventApplicationService {
    private final IamUserSyncUseCase useCase;

    public IamEventApplicationService(IamUserSyncUseCase useCase) {
        this.useCase = useCase;
    }

    @Transactional
    public IamUserSyncUseCase.Result process(Object event) {
        if (event instanceof IamUserCreated created) {
            return useCase.created(created);
        }
        if (event instanceof IamUserDeactivated deactivated) {
            return useCase.deactivated(deactivated);
        }
        throw new IllegalArgumentException("Unsupported IAM event");
    }
}
