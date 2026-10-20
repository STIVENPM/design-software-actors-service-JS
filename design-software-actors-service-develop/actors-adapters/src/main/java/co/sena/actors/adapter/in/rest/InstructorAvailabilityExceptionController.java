package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.rest.dto.AvailabilityExceptionCreateRequest;
import co.sena.actors.adapter.in.rest.dto.AvailabilityExceptionResponse;
import co.sena.actors.core.domain.InstructorAvailabilityException;
import co.sena.actors.core.port.in.InstructorAvailabilityExceptionUseCase;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/instructors")
public class InstructorAvailabilityExceptionController {

    private final InstructorAvailabilityExceptionUseCase useCase;
    private final CurrentUserProvider currentUserProvider;

    public InstructorAvailabilityExceptionController(
            InstructorAvailabilityExceptionUseCase useCase,
            CurrentUserProvider currentUserProvider) {

        this.useCase = useCase;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/{id}/availability-exceptions")
    @PreAuthorize("hasAnyAuthority('ACT_INSTRUCTOR_MANAGE')")
    public ResponseEntity<AvailabilityExceptionResponse> createException(
            @PathVariable("id") UUID instructorId,
            @Valid @RequestBody AvailabilityExceptionCreateRequest request) {

        UUID actorId = currentUserProvider.currentUserId();

        InstructorAvailabilityException created =
                useCase.createException(
                        instructorId,
                        request.getExceptionType(),
                        request.getStartDatetime(),
                        request.getEndDatetime(),
                        request.getDescription(),
                        request.getApprovedBy(),
                        actorId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToResponse(created));
    }

    @GetMapping("/{id}/availability-exceptions")
    @PreAuthorize("hasAnyAuthority('ACT_INSTRUCTOR_VIEW', 'ACT_INSTRUCTOR_MANAGE')")
    public ResponseEntity<List<AvailabilityExceptionResponse>> getExceptions(
            @PathVariable("id") UUID instructorId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        List<AvailabilityExceptionResponse> responses =
                useCase.getOverlappingExceptions(
                                instructorId,
                                from,
                                to
                        )
                        .stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}/availability-exceptions/{exception_id}")
    @PreAuthorize("hasAnyAuthority('ACT_INSTRUCTOR_MANAGE')")
    public ResponseEntity<Void> deleteException(
            @PathVariable("id") UUID instructorId,
            @PathVariable("exception_id") UUID exceptionId) {

        useCase.deleteException(
                instructorId,
                exceptionId,
                currentUserProvider.currentUserId()
        );

        return ResponseEntity.noContent().build();
    }

    private AvailabilityExceptionResponse mapToResponse(
            InstructorAvailabilityException domain) {

        AvailabilityExceptionResponse response =
                new AvailabilityExceptionResponse();

        response.setId(domain.getId());
        response.setInstructorId(domain.getInstructorId());
        response.setExceptionType(domain.getExceptionType());
        response.setStartDatetime(domain.getStartDatetime());
        response.setEndDatetime(domain.getEndDatetime());
        response.setDescription(domain.getDescription());
        response.setApprovedBy(domain.getApprovedBy());
        response.setCreatedAt(domain.getCreatedAt());

        return response;
    }
}

