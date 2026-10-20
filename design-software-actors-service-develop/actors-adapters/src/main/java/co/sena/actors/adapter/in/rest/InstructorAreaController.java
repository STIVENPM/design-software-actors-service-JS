package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.InstructorAreaCreateRequest;
import co.sena.actors.adapter.in.rest.dto.InstructorAreaResponse;
import co.sena.actors.application.port.in.BrowseInstructorAreasUseCase;
import co.sena.actors.application.port.in.CreateInstructorAreaCommand;
import co.sena.actors.application.port.in.ManageInstructorAreaUseCase;
import co.sena.actors.domain.model.InstructorArea;
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
 * Instructor areas CRUD (HU-ACT-002, E1).
 * Base /api/v1 as per servers.url in actors.yaml.
 */
@RestController
@RequestMapping("/api/v1/instructors/{instructorId}/areas")
@Observed(name = "instructor_area.controller")
@SuppressWarnings("null") // ResponseEntity builders carry no null contract; editor-only hints, javac is clean
public class InstructorAreaController {

    private final ManageInstructorAreaUseCase commands;
    private final BrowseInstructorAreasUseCase queries;
    private final InstructorAuth auth;
    private final CurrentUserProvider users;

    public InstructorAreaController(
            ManageInstructorAreaUseCase commands,
            BrowseInstructorAreasUseCase queries,
            InstructorAuth auth,
            CurrentUserProvider users) {
        this.commands = commands;
        this.queries = queries;
        this.auth = auth;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<InstructorAreaResponse> create(
            @PathVariable("instructorId") UUID instructorId,
            @Valid @RequestBody InstructorAreaCreateRequest request) {
        InstructorArea created = commands.create(new CreateInstructorAreaCommand(
                instructorId, request.areaName(), request.isPrimary()), users.currentUserId(), auth.viewer());
        return ResponseEntity.created(URI.create("/api/v1/instructors/" + instructorId + "/areas/" + created.getId()))
                .body(InstructorAreaResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("@instructorAuth.canView()")
    public ResponseEntity<List<InstructorAreaResponse>> list(
            @PathVariable("instructorId") UUID instructorId) {
        List<InstructorAreaResponse> data = queries.listActiveByInstructor(instructorId, auth.viewer()).stream()
                .map(InstructorAreaResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/{areaId}")
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<Void> deactivate(
            @PathVariable("instructorId") UUID instructorId,
            @PathVariable("areaId") UUID areaId) {
        commands.deactivate(instructorId, areaId, users.currentUserId(), auth.viewer());
        return ResponseEntity.noContent().build();
    }
}
