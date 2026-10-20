package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.*;
import co.sena.actors.application.port.in.CompanyUseCase;
import co.sena.actors.domain.exception.CompanyNotFoundException;
import co.sena.actors.domain.exception.DuplicateCompanyException;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
@Observed(name = "company.controller")
public class CompanyController {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "created_at", "createdAt", "updated_at", "updatedAt",
            "nit", "nit", "business_name", "businessName");
    private final CompanyUseCase companies;
    private final CurrentUserProvider users;

    public CompanyController(CompanyUseCase companies, CurrentUserProvider users) {
        this.companies = companies;
        this.users = users;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACT_COMPANY_MANAGE')")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyCreateRequest request) {
        var created = companies.create(request.nit(), request.toDetails(), users.currentUserId());
        return ResponseEntity.created(URI.create("/api/v1/companies/" + created.id()))
                .body(CompanyResponse.from(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_COMPANY_VIEW')")
    public CompanyResponse get(@PathVariable("id") UUID id) {
        return CompanyResponse.from(companies.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACT_COMPANY_VIEW')")
    public CompanyListResponse list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", defaultValue = "created_at:desc") String sort,
            @RequestParam(value = "nit", required = false) String nit,
            @RequestParam(value = "city_id", required = false) UUID cityId,
            @RequestParam(value = "is_active", defaultValue = "true") boolean active) {
        String[] parts = sort.split(":", -1);
        String field = SORT_FIELDS.get(parts[0]);
        if (field == null || parts.length > 2 ||
                (parts.length == 2 && !List.of("asc", "desc").contains(parts[1]))) {
            throw new IllegalArgumentException("Invalid company sort");
        }
        var query = new CompanyUseCase.Query(page, pageSize, field,
                parts.length == 2 && "asc".equals(parts[1]), nit, cityId, active);
        return CompanyListResponse.from(companies.list(query));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_COMPANY_MANAGE')")
    public CompanyResponse replace(@PathVariable("id") UUID id,
            @Valid @RequestBody CompanyUpdateRequest request) {
        return CompanyResponse.from(companies.update(
                id, request.toDetails(), request.isActive(), false, users.currentUserId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_COMPANY_MANAGE')")
    public CompanyResponse patch(@PathVariable("id") UUID id,
            @Valid @RequestBody CompanyUpdateRequest request) {
        return CompanyResponse.from(companies.update(
                id, request.toDetails(), request.isActive(), true, users.currentUserId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_COMPANY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        companies.deactivate(id, users.currentUserId());
        return ResponseEntity.noContent().build();
    }

    // Local handlers avoid altering the error contracts of previous HUs.
    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(CompanyNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateCompanyException.class)
    public ResponseEntity<ErrorResponse> conflict(DuplicateCompanyException ex) {
        return ResponseEntity.status(409).body(ErrorResponse.of("CONFLICT", ex.getMessage(),
                List.of(new ErrorResponse.ErrorDetail("nit", "DUPLICATE"))));
    }
}
