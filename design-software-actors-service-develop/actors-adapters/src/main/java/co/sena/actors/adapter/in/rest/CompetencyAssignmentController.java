package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.CompetencyAssignmentCreateRequest;
import co.sena.actors.adapter.in.rest.dto.CompetencyAssignmentResponse;
import co.sena.actors.application.port.in.BrowseCompetencyAssignmentsUseCase;
import co.sena.actors.application.port.in.CreateCompetencyAssignmentCommand;
import co.sena.actors.application.port.in.ManageCompetencyAssignmentUseCase;
import co.sena.actors.domain.model.CompetencyAssignment;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Competency assignments (HU-ACT-003, E1-E2).
 * Base /api/v1 as per servers.url in actors.yaml. Thin adapter: it only
 * deserializes HTTP and delegates to use cases (visibility lives in the
 * application layer via Viewer, same as areas/contracts).
 */
@RestController
@RequestMapping("/api/v1/instructors/{instructorId}/competencies")
@Observed(name = "competency_assignment.controller")
@SuppressWarnings("null") // ResponseEntity builders carry no null contract; editor-only hints, javac is clean
public class CompetencyAssignmentController {

    private final ManageCompetencyAssignmentUseCase commands;
    private final BrowseCompetencyAssignmentsUseCase queries;
    private final InstructorAuth auth;
    private final CurrentUserProvider users;

    public CompetencyAssignmentController(
            ManageCompetencyAssignmentUseCase commands,
            BrowseCompetencyAssignmentsUseCase queries,
            InstructorAuth auth,
            CurrentUserProvider users) {
        this.commands = commands;
        this.queries = queries;
        this.auth = auth;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<CompetencyAssignmentResponse> create(
            @PathVariable("instructorId") UUID instructorId,
            @Valid @RequestBody CompetencyAssignmentCreateRequest request) {
        CompetencyAssignment created = commands.create(new CreateCompetencyAssignmentCommand(
                instructorId,
                request.competencyId(),
                request.certifiedAt(),
                request.certificationEntity()), users.currentUserId(), auth.viewer());
        return ResponseEntity.created(URI.create(
                        "/api/v1/instructors/" + instructorId + "/competencies/" + created.getId()))
                .body(CompetencyAssignmentResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("@instructorAuth.canView()")
    public ResponseEntity<List<CompetencyAssignmentResponse>> list(
            @PathVariable("instructorId") UUID instructorId) {
        List<CompetencyAssignmentResponse> data =
                queries.listActiveByInstructor(instructorId, auth.viewer()).stream()
                        .map(CompetencyAssignmentResponse::from)
                        .toList();
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<Void> deactivate(
            @PathVariable("instructorId") UUID instructorId,
            @PathVariable("assignmentId") UUID assignmentId) {
        commands.deactivate(instructorId, assignmentId, users.currentUserId(), auth.viewer());
        return ResponseEntity.noContent().build();
    }
}
