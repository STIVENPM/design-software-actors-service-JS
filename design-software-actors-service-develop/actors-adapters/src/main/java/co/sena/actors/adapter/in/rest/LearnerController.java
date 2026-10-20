package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.in.rest.dto.ActivityLogCursorPageDto;
import co.sena.actors.adapter.in.rest.dto.LearnerCreateRequest;
import co.sena.actors.adapter.in.rest.dto.LearnerDto;
import co.sena.actors.adapter.in.rest.dto.LearnerFichaEnrollmentCreateRequest;
import co.sena.actors.adapter.in.rest.dto.LearnerFichaEnrollmentResponse;
import co.sena.actors.adapter.in.rest.dto.LearnerUpdateRequest;
import co.sena.actors.application.port.in.GetLearnerActivityLogUseCase;
import co.sena.actors.application.port.in.ManageLearnerFichaEnrollmentUseCase;
import co.sena.actors.application.port.in.ManageLearnerUseCase;
import co.sena.actors.application.port.in.QueryLearnerFichaEnrollmentUseCase;
import co.sena.actors.application.port.in.QueryLearnerUseCase;
import co.sena.actors.application.port.in.QueryLearnerUseCase.BrowseQuery;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learners")
@Observed(name = "learner.controller")
public class LearnerController {

    private final GetLearnerActivityLogUseCase activityLogUseCase;
    private final ManageLearnerUseCase manageLearnerUseCase;
    private final QueryLearnerUseCase queryLearnerUseCase;
    private final ManageLearnerFichaEnrollmentUseCase manageEnrollmentUseCase;
    private final QueryLearnerFichaEnrollmentUseCase queryEnrollmentUseCase;
    private final InstructorAuth auth;

    public LearnerController(
            GetLearnerActivityLogUseCase activityLogUseCase,
            ManageLearnerUseCase manageLearnerUseCase,
            QueryLearnerUseCase queryLearnerUseCase,
            ManageLearnerFichaEnrollmentUseCase manageEnrollmentUseCase,
            QueryLearnerFichaEnrollmentUseCase queryEnrollmentUseCase,
            InstructorAuth auth) {
        this.activityLogUseCase = activityLogUseCase;
        this.manageLearnerUseCase = manageLearnerUseCase;
        this.queryLearnerUseCase = queryLearnerUseCase;
        this.manageEnrollmentUseCase = manageEnrollmentUseCase;
        this.queryEnrollmentUseCase = queryEnrollmentUseCase;
        this.auth = auth;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACT_LEARNER_MANAGE')")
    public ResponseEntity<LearnerDto> createLearner(@RequestBody LearnerCreateRequest request) {
        var command = new ManageLearnerUseCase.CreateCommand(
                request.userId(),
                request.documentType(),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.birthDate());
        var learner = manageLearnerUseCase.createLearner(command);
        return ResponseEntity.created(URI.create("/api/v1/learners/" + learner.id()))
                .body(LearnerDto.from(learner));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_LEARNER_VIEW')")
    public ResponseEntity<LearnerDto> getLearnerById(@PathVariable("id") UUID id) {
        var learner = queryLearnerUseCase.getLearnerById(id);
        return ResponseEntity.ok(LearnerDto.from(learner));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACT_LEARNER_VIEW')")
    public ResponseEntity<co.sena.actors.adapter.in.rest.dto.LearnerPageEnvelope> browseLearners(
            @RequestParam(value = "document_number", required = false) String documentNumber,
            @RequestParam(value = "ficha_id", required = false) UUID fichaId,
            @RequestParam(value = "enrollment_status_id", required = false) UUID enrollmentStatusId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        var query = new BrowseQuery(documentNumber, fichaId, enrollmentStatusId, page, limit);
        var result = queryLearnerUseCase.browseLearners(query);
        
        var dtos = result.data().stream().map(LearnerDto::from).toList();
        var pagination = new co.sena.actors.adapter.in.rest.dto.LearnerPageEnvelope.PaginationInfo(
                result.totalItems(), result.currentPage(), result.totalPages());
        
        return ResponseEntity.ok(new co.sena.actors.adapter.in.rest.dto.LearnerPageEnvelope(dtos, pagination));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_LEARNER_MANAGE')")
    public ResponseEntity<LearnerDto> updateLearner(
            @PathVariable("id") UUID id,
            @RequestBody LearnerUpdateRequest request) {
        var command = new ManageLearnerUseCase.UpdateCommand(
                request.documentType(),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.birthDate());
        var learner = manageLearnerUseCase.updateLearner(id, command);
        return ResponseEntity.ok(LearnerDto.from(learner));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_LEARNER_MANAGE')")
    public ResponseEntity<LearnerDto> patchLearner(
            @PathVariable("id") UUID id,
            @RequestBody LearnerUpdateRequest request) {
        // Similar to put for now
        var command = new ManageLearnerUseCase.UpdateCommand(
                request.documentType(),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.birthDate());
        var learner = manageLearnerUseCase.updateLearner(id, command);
        return ResponseEntity.ok(LearnerDto.from(learner));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_LEARNER_MANAGE')")
    public ResponseEntity<Void> cancelLearnerEnrollment(@PathVariable("id") UUID id) {
        manageLearnerUseCase.cancelLearnerEnrollment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ficha-enrollments")
    @PreAuthorize("hasAuthority('ACT_LEARNER_MANAGE')")
    public ResponseEntity<LearnerFichaEnrollmentResponse> createFichaEnrollment(
            @PathVariable("id") UUID learnerId,
            @Valid @RequestBody LearnerFichaEnrollmentCreateRequest request) {
        var created = manageEnrollmentUseCase.enroll(
                learnerId,
                new ManageLearnerFichaEnrollmentUseCase.CreateCommand(
                        request.fichaId(),
                        request.enrollmentStatusId(),
                        request.stage(),
                        request.enrollmentDate(),
                        request.transferReason()));

        return ResponseEntity
                .created(URI.create("/api/v1/learners/" + learnerId + "/ficha-enrollments/" + created.id()))
                .body(LearnerFichaEnrollmentResponse.from(created));
    }

    @GetMapping("/{id}/ficha-enrollments")
    @PreAuthorize("hasAuthority('ACT_LEARNER_VIEW')")
    public ResponseEntity<List<LearnerFichaEnrollmentResponse>> getFichaEnrollmentHistory(
            @PathVariable("id") UUID learnerId) {
        var data = queryEnrollmentUseCase.listHistory(learnerId).stream()
                .map(LearnerFichaEnrollmentResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}/activity-log")
    @PreAuthorize("hasAuthority('ACT_ACTIVITY_LOG_VIEW')")
    public ResponseEntity<ActivityLogCursorPageDto> getActivityLog(
            @PathVariable("id") UUID id,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        var page = activityLogUseCase.getLearnerActivityLog(id, cursor, limit, auth.viewer());
        return ResponseEntity.ok(ActivityLogCursorPageDto.from(page));
    }
}

