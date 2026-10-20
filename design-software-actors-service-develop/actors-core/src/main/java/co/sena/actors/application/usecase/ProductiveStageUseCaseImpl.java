package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import co.sena.actors.application.port.out.ProductiveStageRepositoryPort;
import co.sena.actors.domain.exception.InvalidProductiveStageException;
import co.sena.actors.domain.exception.ProductiveStageNotFoundException;
import co.sena.actors.domain.model.CompanyVisit;
import co.sena.actors.domain.model.ProductiveStage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

public class ProductiveStageUseCaseImpl implements ProductiveStageUseCase {
    private static final Set<String> VISIT_TYPES = Set.of("IN_PERSON", "VIRTUAL", "PHONE");
    private static final Set<String> PERFORMANCE =
            Set.of("EXCELLENT", "GOOD", "ACCEPTABLE", "DEFICIENT");
    private static final Set<String> SATISFACTION = Set.of("HIGH", "MEDIUM", "LOW");

    private final ProductiveStageRepositoryPort repository;

    public ProductiveStageUseCaseImpl(ProductiveStageRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public ProductiveStage create(CreateCommand command, UUID actor) {
        validateCreate(command);
        requireReferences(command.learnerId(), command.companyId(), command.supervisorInstructorId());
        repository.statusInfo(command.statusId()).orElseThrow(
                () -> new InvalidProductiveStageException("status_id not found"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(new ProductiveStage(UUID.randomUUID(), command.learnerId(),
                command.companyId(), command.supervisorInstructorId(), command.statusId(),
                command.startDate(), command.plannedEndDate(), command.totalHoursRequired(),
                0, null, now, actor, now, actor, 0));
    }

    @Override
    public ProductiveStage get(UUID id) {
        return repository.findById(id).orElseThrow(ProductiveStageNotFoundException::new);
    }

    @Override
    public Page list(Query query) {
        if (query.page() < 1 || query.pageSize() < 1 || query.pageSize() > 100) {
            throw new IllegalArgumentException("page must be >= 1 and page_size between 1 and 100");
        }
        return repository.findAll(query);
    }

    @Override
    public ProductiveStage update(UUID id, UpdateCommand command, UUID actor) {
        ProductiveStage current = get(id);
        UUID statusId = command.statusId() == null ? current.statusId() : command.statusId();
        var status = repository.statusInfo(statusId).orElseThrow(
                () -> new InvalidProductiveStageException("status_id not found"));
        int completed = command.totalHoursCompleted() == null
                ? current.totalHoursCompleted() : command.totalHoursCompleted();
        if (completed < 0) {
            throw new InvalidProductiveStageException("total_hours_completed must be >= 0");
        }
        if (status.terminal() && repository.countVisits(id) < 2) {
            throw new InvalidProductiveStageException(
                    "terminal status requires at least 2 company visits");
        }
        String interruptionReason = command.interruptionReason();
        if (!"INTERRUPTED".equals(status.code())) {
            interruptionReason = null;
        } else if (interruptionReason == null || interruptionReason.isBlank()) {
            throw new InvalidProductiveStageException(
                    "interruption_reason is required for INTERRUPTED status");
        }
        var plannedEnd = command.plannedEndDate() == null
                ? current.plannedEndDate() : command.plannedEndDate();
        if (plannedEnd != null && plannedEnd.isBefore(current.startDate())) {
            throw new InvalidProductiveStageException("planned_end_date must be >= start_date");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(new ProductiveStage(current.id(), current.learnerId(),
                current.companyId(), current.supervisorInstructorId(), statusId,
                current.startDate(), plannedEnd, current.totalHoursRequired(), completed,
                interruptionReason, current.createdAt(), current.createdBy(), now, actor,
                current.rowVersion()));
    }

    @Override
    public CompanyVisit addVisit(UUID productiveStageId, VisitCommand command, UUID actor) {
        ProductiveStage stage = get(productiveStageId);
        validateVisit(command);
        UUID instructorId = command.instructorId() == null ? stage.supervisorInstructorId() : command.instructorId();
        if (!repository.existsActiveInstructor(instructorId)) {
            throw new InvalidProductiveStageException("instructor_id not found");
        }
        boolean late = repository.countVisits(productiveStageId) == 0
                && command.visitDate().isAfter(stage.startDate().plusDays(30));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.saveVisit(new CompanyVisit(UUID.randomUUID(), productiveStageId,
                instructorId, command.visitNumber(), command.visitDate(), command.visitType(),
                command.learnerPerformance(), command.companySatisfaction(),
                command.hasImprovementPlan() == null ? false : command.hasImprovementPlan(),
                late, now, actor));
    }

    private void validateCreate(CreateCommand command) {
        if (command.learnerId() == null || command.companyId() == null
                || command.supervisorInstructorId() == null || command.statusId() == null) {
            throw new InvalidProductiveStageException("learner_id, company_id, supervisor_instructor_id and status_id are required");
        }
        if (command.startDate() == null) {
            throw new InvalidProductiveStageException("start_date is required");
        }
        if (command.plannedEndDate() != null && command.plannedEndDate().isBefore(command.startDate())) {
            throw new InvalidProductiveStageException("planned_end_date must be >= start_date");
        }
        if (command.totalHoursRequired() == null || command.totalHoursRequired() <= 0) {
            throw new InvalidProductiveStageException("total_hours_required must be > 0");
        }
    }

    private void requireReferences(UUID learnerId, UUID companyId, UUID instructorId) {
        if (!repository.existsLearner(learnerId)) {
            throw new InvalidProductiveStageException("learner_id not found");
        }
        if (!repository.existsActiveCompany(companyId)) {
            throw new InvalidProductiveStageException("company_id not found");
        }
        if (!repository.existsActiveInstructor(instructorId)) {
            throw new InvalidProductiveStageException("supervisor_instructor_id not found");
        }
    }

    private void validateVisit(VisitCommand command) {
        if (command.visitNumber() == null || command.visitNumber() < 1) {
            throw new InvalidProductiveStageException("visit_number must be >= 1");
        }
        if (command.visitDate() == null) {
            throw new InvalidProductiveStageException("visit_date is required");
        }
        if (!VISIT_TYPES.contains(command.visitType())) {
            throw new InvalidProductiveStageException("invalid visit_type");
        }
        if (!PERFORMANCE.contains(command.learnerPerformance())) {
            throw new InvalidProductiveStageException("invalid learner_performance");
        }
        if (command.companySatisfaction() != null && !SATISFACTION.contains(command.companySatisfaction())) {
            throw new InvalidProductiveStageException("invalid company_satisfaction");
        }
    }
}
