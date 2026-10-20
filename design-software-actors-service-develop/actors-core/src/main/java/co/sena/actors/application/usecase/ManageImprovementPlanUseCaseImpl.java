package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import co.sena.actors.application.port.out.ImprovementPlanRepositoryPort;
import co.sena.actors.domain.exception.ImprovementPlanNotFoundException;
import co.sena.actors.domain.exception.InvalidImprovementPlanException;
import co.sena.actors.domain.model.ActorImprovementPlan;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

public class ManageImprovementPlanUseCaseImpl implements ManageImprovementPlanUseCase {
    private static final Set<String> ORIGINS = Set.of(
            "COMPANY_VISIT", "PERFORMANCE_REVIEW", "TRACKING_SESSION", "DISCIPLINARY");

    private final ImprovementPlanRepositoryPort repository;

    public ManageImprovementPlanUseCaseImpl(ImprovementPlanRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public ActorImprovementPlan create(CreateCommand command, UUID actorId) {
        validateArc(command.instructorId(), command.learnerId());
        validateCommon(command.origin(), command.description(), command.specificGoals(), command.dueDate());
        requireStatus(command.statusId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(new ActorImprovementPlan(
                UUID.randomUUID(), command.instructorId(), command.learnerId(), command.origin(),
                command.originReferenceId(), command.description(), command.specificGoals(),
                command.dueDate(), command.statusId(), actorId, now, null, now, actorId, 0,
                true, null, null));
    }

    @Override
    public ActorImprovementPlan get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ImprovementPlanNotFoundException(id));
    }

    @Override
    public Page list(Query query) {
        if (query.page() < 1 || query.pageSize() < 1 || query.pageSize() > 100) {
            throw new IllegalArgumentException("page must be >= 1 and page_size between 1 and 100");
        }
        return repository.findAll(query);
    }

    @Override
    public ActorImprovementPlan update(UUID id, UpdateCommand command, UUID actorId) {
        ActorImprovementPlan current = get(id);
        UUID statusId = command.statusId() == null ? current.statusId() : command.statusId();
        var status = requireStatus(statusId);
        String description = command.description() == null ? current.description() : command.description();
        String goals = command.specificGoals() == null ? current.specificGoals() : command.specificGoals();
        var dueDate = command.dueDate() == null ? current.dueDate() : command.dueDate();
        validateCommon(current.origin(), description, goals, dueDate);
        OffsetDateTime completedAt = status.terminal()
                ? (current.completedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : current.completedAt())
            : current.completedAt();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(new ActorImprovementPlan(current.id(), current.instructorId(), current.learnerId(),
                current.origin(), current.originReferenceId(), description, goals, dueDate, statusId,
                current.createdBy(), current.createdAt(), completedAt, now, actorId, current.rowVersion(),
                current.isActive(), current.deletedAt(), current.deletedBy()));
    }

    private ImprovementPlanRepositoryPort.StatusInfo requireStatus(UUID statusId) {
        if (statusId == null) {
            throw new InvalidImprovementPlanException("status_id is required");
        }
        return repository.statusInfo(statusId)
                .orElseThrow(() -> new InvalidImprovementPlanException("status_id not found"));
    }

    private void validateArc(UUID instructorId, UUID learnerId) {
        if ((instructorId == null) == (learnerId == null)) {
            throw new InvalidImprovementPlanException("EXCLUSIVE_ARC_VIOLATION");
        }
    }

    private void validateCommon(String origin, String description, String goals, java.time.LocalDate dueDate) {
        if (!ORIGINS.contains(origin)) {
            throw new InvalidImprovementPlanException("invalid origin");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidImprovementPlanException("description is required");
        }
        if (goals == null || goals.isBlank()) {
            throw new InvalidImprovementPlanException("specific_goals is required");
        }
        if (dueDate == null) {
            throw new InvalidImprovementPlanException("due_date is required");
        }
    }
}