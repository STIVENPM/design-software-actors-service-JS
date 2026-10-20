package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.CompanyVisit;
import co.sena.actors.domain.model.ProductiveStage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductiveStageUseCase {
    ProductiveStage create(CreateCommand command, UUID actor);
    ProductiveStage get(UUID id);
    Page list(Query query);
    ProductiveStage update(UUID id, UpdateCommand command, UUID actor);
    CompanyVisit addVisit(UUID productiveStageId, VisitCommand command, UUID actor);

    record CreateCommand(UUID learnerId, UUID companyId, UUID supervisorInstructorId,
                         LocalDate startDate, LocalDate plannedEndDate,
                         Integer totalHoursRequired, UUID statusId) {}

    record UpdateCommand(UUID statusId, Integer totalHoursCompleted,
                         String interruptionReason, LocalDate plannedEndDate) {}

    record VisitCommand(Integer visitNumber, LocalDate visitDate, String visitType,
                        String learnerPerformance, String companySatisfaction,
                        Boolean hasImprovementPlan, UUID instructorId) {}

    record Query(int page, int pageSize, UUID learnerId, UUID companyId, UUID statusId) {}
    record Page(List<ProductiveStage> content, int page, int pageSize,
                long totalItems, int totalPages) {}
}
