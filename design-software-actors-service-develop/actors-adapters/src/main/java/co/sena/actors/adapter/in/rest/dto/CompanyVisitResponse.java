package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.CompanyVisit;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyVisitResponse(
        UUID id,
        @JsonProperty("productive_stage_id") UUID productiveStageId,
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("visit_number") Integer visitNumber,
        @JsonProperty("visit_date") LocalDate visitDate,
        @JsonProperty("visit_type") String visitType,
        @JsonProperty("learner_performance") String learnerPerformance,
        @JsonProperty("company_satisfaction") String companySatisfaction,
        @JsonProperty("has_improvement_plan") Boolean hasImprovementPlan,
        @JsonProperty("first_visit_late") Boolean firstVisitLate,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
    public static CompanyVisitResponse from(CompanyVisit visit) {
        return new CompanyVisitResponse(visit.id(), visit.productiveStageId(),
                visit.instructorId(), visit.visitNumber(), visit.visitDate(), visit.visitType(),
                visit.learnerPerformance(), visit.companySatisfaction(),
                visit.hasImprovementPlan(), visit.firstVisitLate(), visit.createdAt());
    }
}
