package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.*;
import co.sena.actors.application.port.in.ProductiveStageUseCase;
import co.sena.actors.domain.exception.InvalidProductiveStageException;
import co.sena.actors.domain.exception.ProductiveStageNotFoundException;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/productive-stages")
@Observed(name = "productive-stage.controller")
public class ProductiveStageController {
    private final ProductiveStageUseCase stages;
    private final CurrentUserProvider users;

    public ProductiveStageController(ProductiveStageUseCase stages, CurrentUserProvider users) {
        this.stages = stages;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACT_PRODUCTIVE_STAGE_MANAGE')")
    public ResponseEntity<ProductiveStageResponse> create(
            @Valid @RequestBody ProductiveStageCreateRequest request) {
        var created = stages.create(request.toCommand(), users.currentUserId());
        return ResponseEntity.created(URI.create("/api/v1/productive-stages/" + created.id()))
                .body(ProductiveStageResponse.from(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_ACTIVITY_LOG_VIEW')")
    public ProductiveStageResponse get(@PathVariable("id") UUID id) {
        return ProductiveStageResponse.from(stages.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACT_ACTIVITY_LOG_VIEW')")
    public ProductiveStageListResponse list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "learner_id", required = false) UUID learnerId,
            @RequestParam(value = "company_id", required = false) UUID companyId,
            @RequestParam(value = "status_id", required = false) UUID statusId) {
        return ProductiveStageListResponse.from(stages.list(
                new ProductiveStageUseCase.Query(page, pageSize, learnerId, companyId, statusId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_PRODUCTIVE_STAGE_MANAGE')")
    public ProductiveStageResponse update(@PathVariable("id") UUID id,
            @Valid @RequestBody ProductiveStageUpdateRequest request) {
        return ProductiveStageResponse.from(stages.update(id, request.toCommand(), users.currentUserId()));
    }

    @PostMapping("/{id}/visits")
    @PreAuthorize("hasAuthority('ACT_PRODUCTIVE_STAGE_MANAGE')")
    public ResponseEntity<CompanyVisitResponse> addVisit(@PathVariable("id") UUID id,
            @Valid @RequestBody CompanyVisitCreateRequest request) {
        var created = stages.addVisit(id, request.toCommand(), users.currentUserId());
        return ResponseEntity.created(URI.create("/api/v1/productive-stages/" + id + "/visits/" + created.id()))
                .body(CompanyVisitResponse.from(created));
    }

    @ExceptionHandler(ProductiveStageNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ProductiveStageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProductiveStageException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidProductiveStageException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("UNPROCESSABLE_ENTITY", ex.getMessage(),
                        List.of(new ErrorResponse.ErrorDetail("productive_stage", "INVALID_VALUE"))));
    }
}
