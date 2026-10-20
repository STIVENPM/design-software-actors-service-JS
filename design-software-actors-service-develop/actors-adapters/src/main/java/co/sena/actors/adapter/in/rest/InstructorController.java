package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.InstructorCreateRequest;
import co.sena.actors.adapter.in.rest.dto.InstructorListResponse;
import co.sena.actors.adapter.in.rest.dto.InstructorPatchRequest;
import co.sena.actors.adapter.in.rest.dto.InstructorReplaceRequest;
import co.sena.actors.adapter.in.rest.dto.InstructorResponse;
import co.sena.actors.adapter.in.rest.dto.ActivityLogCursorPageDto;
import co.sena.actors.application.port.in.BrowseInstructorsUseCase;
import co.sena.actors.application.port.in.GetInstructorActivityLogUseCase;
import co.sena.actors.application.port.in.CreateInstructorCommand;
import co.sena.actors.application.port.in.InstructorPage;
import co.sena.actors.application.port.in.InstructorQuery;
import co.sena.actors.application.port.in.ManageInstructorUseCase;
import co.sena.actors.application.port.in.PatchInstructorCommand;
import co.sena.actors.application.port.in.ReplaceInstructorCommand;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Instructor;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
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

/**
 * Base instructor profile CRUD (HU-ACT-001, E1-E4).
 * Base /api/v1 as per servers.url in actors.yaml.
 * Thin adapter: it only deserializes HTTP and delegates to use cases.
 * Reads go through BrowseInstructorsUseCase (pagination, filters and
 * visibility live in the application layer).
 */
@RestController
@RequestMapping("/api/v1/instructors")
@Observed(name = "instructor.controller")
@SuppressWarnings("null") // ResponseEntity builders carry no null contract; editor-only hints, javac is clean
public class InstructorController {

    private final ManageInstructorUseCase commands;
    private final BrowseInstructorsUseCase queries;
    private final GetInstructorActivityLogUseCase activityLogUseCase;
    private final InstructorAuth auth;
    private final CurrentUserProvider users;

    public InstructorController(
            ManageInstructorUseCase commands,
            BrowseInstructorsUseCase queries,
            GetInstructorActivityLogUseCase activityLogUseCase,
            InstructorAuth auth,
            CurrentUserProvider users) {
        this.commands = commands;
        this.queries = queries;
        this.activityLogUseCase = activityLogUseCase;
        this.auth = auth;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<InstructorResponse> create(@Valid @RequestBody InstructorCreateRequest request) {
        Instructor created = commands.create(new CreateInstructorCommand(
                request.userId(),
                DocumentType.of(request.documentType()),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.defaultMaxHoursPerWeek()), users.currentUserId());
        return ResponseEntity.created(URI.create("/api/v1/instructors/" + created.getId()))
                .body(InstructorResponse.from(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@instructorAuth.canView()")
    public ResponseEntity<InstructorResponse> getById(@PathVariable("id") UUID id) {
        Instructor instructor = queries.get(id, auth.viewer());
        return ResponseEntity.ok(InstructorResponse.from(instructor));
    }

    @GetMapping
    @PreAuthorize("@instructorAuth.canView()")
    public ResponseEntity<InstructorListResponse> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "document_number", required = false) String documentNumber,
            @RequestParam(value = "is_active", required = false) Boolean active,
            @RequestParam(value = "area", required = false) String area,
            @RequestParam(value = "training_center_id", required = false) String trainingCenterId) {
        InstructorQuery query = InstructorQueryMapper.toQuery(
                page, pageSize, sort, documentNumber, active, area, trainingCenterId);
        InstructorPage result = queries.list(query, auth.viewer());
        return ResponseEntity.ok(InstructorListResponse.from(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<InstructorResponse> replace(
            @PathVariable("id") UUID id, @Valid @RequestBody InstructorReplaceRequest request) {
        Instructor updated = commands.replace(id, new ReplaceInstructorCommand(
                DocumentType.of(request.documentType()),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.defaultMaxHoursPerWeek(),
                request.rowVersion()), users.currentUserId());
        return ResponseEntity.ok(InstructorResponse.from(updated));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<InstructorResponse> patch(
            @PathVariable("id") UUID id, @Valid @RequestBody InstructorPatchRequest request) {
        Instructor updated = commands.patch(id, new PatchInstructorCommand(
                request.documentType() == null ? null : DocumentType.of(request.documentType()),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.defaultMaxHoursPerWeek(),
                request.rowVersion()), users.currentUserId());
        return ResponseEntity.ok(InstructorResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        commands.deactivate(id, users.currentUserId());
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/activity-log")
    @PreAuthorize("hasAuthority('ACT_ACTIVITY_LOG_VIEW')")
    public ResponseEntity<ActivityLogCursorPageDto> getActivityLog(
            @PathVariable("id") UUID id,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        var page = activityLogUseCase.getInstructorActivityLog(id, cursor, limit, auth.viewer());
        return ResponseEntity.ok(ActivityLogCursorPageDto.from(page));
    }
}
