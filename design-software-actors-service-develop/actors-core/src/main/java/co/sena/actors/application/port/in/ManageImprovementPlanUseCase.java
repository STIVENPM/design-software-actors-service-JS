package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.ActorImprovementPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageImprovementPlanUseCase {
    ActorImprovementPlan create(CreateCommand command, UUID actorId);

    ActorImprovementPlan get(UUID id);

    Page list(Query query);

    ActorImprovementPlan update(UUID id, UpdateCommand command, UUID actorId);

    record CreateCommand(UUID instructorId, UUID learnerId, String origin,
            UUID originReferenceId, String description, String specificGoals,
            LocalDate dueDate, UUID statusId) {}

    record UpdateCommand(String description, String specificGoals, LocalDate dueDate,
            UUID statusId) {}

    record Query(int page, int pageSize, UUID instructorId, UUID learnerId, UUID statusId) {}

    record Page(List<ActorImprovementPlan> content, int page, int pageSize,
            long totalItems, int totalPages) {}
}