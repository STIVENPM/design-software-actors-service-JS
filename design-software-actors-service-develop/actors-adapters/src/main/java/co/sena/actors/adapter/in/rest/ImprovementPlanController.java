package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.ImprovementPlanCreateRequest;
import co.sena.actors.adapter.in.rest.dto.ImprovementPlanListResponse;
import co.sena.actors.adapter.in.rest.dto.ImprovementPlanResponse;
import co.sena.actors.adapter.in.rest.dto.ImprovementPlanUpdateRequest;
import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/improvement-plans")
@Observed(name = "improvement-plan.controller")
public class ImprovementPlanController {
    private final ManageImprovementPlanUseCase plans;
    private final CurrentUserProvider users;

    public ImprovementPlanController(ManageImprovementPlanUseCase plans, CurrentUserProvider users) {
        this.plans = plans;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACT_PRODUCTIVE_STAGE_MANAGE')")
    public ResponseEntity<ImprovementPlanResponse> create(
            @Valid @RequestBody ImprovementPlanCreateRequest request) {
        var created = plans.create(request.toCommand(), users.currentUserId());
        return ResponseEntity.created(URI.create("/api/v1/improvement-plans/" + created.id()))
                .body(ImprovementPlanResponse.from(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_ACTIVITY_LOG_VIEW')")
    public ImprovementPlanResponse get(@PathVariable UUID id) {
        return ImprovementPlanResponse.from(plans.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACT_ACTIVITY_LOG_VIEW')")
    public ImprovementPlanListResponse list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "instructor_id", required = false) UUID instructorId,
            @RequestParam(value = "learner_id", required = false) UUID learnerId,
            @RequestParam(value = "status_id", required = false) UUID statusId) {
        return ImprovementPlanListResponse.from(plans.list(
                new ManageImprovementPlanUseCase.Query(page, pageSize, instructorId, learnerId, statusId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_PRODUCTIVE_STAGE_MANAGE')")
    public ImprovementPlanResponse update(@PathVariable UUID id,
            @RequestBody ImprovementPlanUpdateRequest request) {
        return ImprovementPlanResponse.from(plans.update(id, request.toCommand(), users.currentUserId()));
    }
}