package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.InstructorContractCreateRequest;
import co.sena.actors.adapter.in.rest.dto.InstructorContractResponse;
import co.sena.actors.adapter.in.rest.dto.InstructorContractUpdateRequest;
import co.sena.actors.application.port.in.BrowseInstructorContractsUseCase;
import co.sena.actors.application.port.in.CreateInstructorContractCommand;
import co.sena.actors.application.port.in.ManageInstructorContractUseCase;
import co.sena.actors.application.port.in.UpdateInstructorContractCommand;
import co.sena.actors.domain.model.ContractType;
import co.sena.actors.domain.model.InstructorContract;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instructor contracts CRUD (HU-ACT-002, E2-E3).
 * Base /api/v1 as per servers.url in actors.yaml.
 */
@RestController
@RequestMapping("/api/v1/instructors/{instructorId}/contracts")
@Observed(name = "instructor_contract.controller")
@SuppressWarnings("null") // ResponseEntity builders carry no null contract; editor-only hints, javac is clean
public class InstructorContractController {

    private final ManageInstructorContractUseCase commands;
    private final BrowseInstructorContractsUseCase queries;
    private final InstructorAuth auth;
    private final CurrentUserProvider users;

    public InstructorContractController(
            ManageInstructorContractUseCase commands,
            BrowseInstructorContractsUseCase queries,
            InstructorAuth auth,
            CurrentUserProvider users) {
        this.commands = commands;
        this.queries = queries;
        this.auth = auth;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<InstructorContractResponse> create(
            @PathVariable("instructorId") UUID instructorId,
            @Valid @RequestBody InstructorContractCreateRequest request) {
        InstructorContract created = commands.create(new CreateInstructorContractCommand(
                instructorId,
                ContractType.of(request.contractType()),
                request.startDate(),
                request.endDate(),
                request.trainingCenterId(),
                request.weeklyHourLimit(),
                request.notes(),
                request.isCurrent()), users.currentUserId(), auth.viewer());
        return ResponseEntity.created(URI.create("/api/v1/instructors/" + instructorId + "/contracts/" + created.getId()))
                .body(InstructorContractResponse.from(created));
    }

    @GetMapping
    @PreAuthorize("@instructorAuth.canView()")
    public ResponseEntity<List<InstructorContractResponse>> list(
            @PathVariable("instructorId") UUID instructorId) {
        List<InstructorContractResponse> data = queries.listByInstructor(instructorId, auth.viewer()).stream()
                .map(InstructorContractResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{contractId}")
    @PreAuthorize("@instructorAuth.canView()")
    public ResponseEntity<InstructorContractResponse> get(
            @PathVariable("instructorId") UUID instructorId,
            @PathVariable("contractId") UUID contractId) {
        InstructorContract contract = queries.get(instructorId, contractId, auth.viewer());
        return ResponseEntity.ok(InstructorContractResponse.from(contract));
    }

    @PutMapping("/{contractId}")
    @PreAuthorize("@instructorAuth.canManage()")
    public ResponseEntity<InstructorContractResponse> update(
            @PathVariable("instructorId") UUID instructorId,
            @PathVariable("contractId") UUID contractId,
            @Valid @RequestBody InstructorContractUpdateRequest request) {
        InstructorContract updated = commands.update(instructorId, contractId, new UpdateInstructorContractCommand(
                request.startDate(),
                request.endDate(),
                request.weeklyHourLimit(),
                request.isCurrent(),
                request.notes(),
                request.rowVersion()), users.currentUserId(), auth.viewer());
        return ResponseEntity.ok(InstructorContractResponse.from(updated));
    }
}
