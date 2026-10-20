package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.in.rest.dto.ErrorResponse;
import co.sena.actors.adapter.in.rest.dto.ErrorResponse.ErrorDetail;
import co.sena.actors.domain.exception.CompetencyAssignmentNotFoundException;
import co.sena.actors.domain.exception.DuplicateCompetencyAssignmentException;
import co.sena.actors.domain.exception.DuplicateInstructorAreaException;
import co.sena.actors.domain.exception.DuplicateInstructorException;
import co.sena.actors.domain.exception.InstructorAreaNotFoundException;
import co.sena.actors.domain.exception.InstructorAvailabilityExceptionNotFoundException;
import co.sena.actors.domain.exception.InstructorContractNotFoundException;
import co.sena.actors.domain.exception.InstructorNotFoundException;
import co.sena.actors.domain.exception.InvalidInstructorContractException;
import co.sena.actors.domain.exception.InvalidInstructorException;
import co.sena.actors.domain.exception.InvalidLearnerEnrollmentException;
import co.sena.actors.domain.exception.InvalidImprovementPlanException;
import co.sena.actors.domain.exception.ImprovementPlanNotFoundException;
import co.sena.actors.domain.exception.StaleInstructorAreaVersionException;
import co.sena.actors.domain.exception.StaleInstructorContractVersionException;
import co.sena.actors.domain.exception.StaleInstructorVersionException;
import co.sena.actors.domain.exception.UnsupportedScopeException;
import co.sena.actors.application.usecase.LearnerNotFoundException;
import co.sena.actors.application.usecase.LearnerConflictException;
import co.sena.actors.application.usecase.FichaNotFoundException;
import co.sena.actors.application.usecase.FichaLookupUnavailableException;
import co.sena.actors.application.usecase.ActiveFichaInconsistencyException;
import co.sena.actors.adapter.config.ReportRateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Mapping to the HU codes: 400 VALIDATION_ERROR, 404 NOT_FOUND,
 * 409 CONFLICT, 422 UNPROCESSABLE_ENTITY.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ReportRateLimitExceededException.class)
        public ResponseEntity<ErrorResponse> handleReportRateLimit(ReportRateLimitExceededException ex) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ErrorResponse.of("TOO_MANY_REQUESTS", "Report rate limit exceeded"));
        }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.add(new ErrorDetail(
                        error.getField(),
                        messageToIssue(error.getDefaultMessage())
                ))
        );

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", "Invalid payload", details));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        String message = ex instanceof IllegalArgumentException && ex.getMessage() != null
                ? ex.getMessage()
                : "Invalid payload";

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(InstructorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(InstructorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "Instructor not found"));
    }

    @ExceptionHandler(LearnerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLearnerNotFound(LearnerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ActiveFichaInconsistencyException.class)
    public ResponseEntity<ErrorResponse> handleActiveFichaInconsistency(
            ActiveFichaInconsistencyException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "INSUFFICIENT_PERMISSIONS",
                        "Feature no presente en JWT"));
    }

    @ExceptionHandler(FichaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFichaNotFound(FichaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(FichaLookupUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleFichaLookupUnavailable(FichaLookupUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("DEPENDENCY_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(ImprovementPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImprovementPlanNotFound(ImprovementPlanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidImprovementPlanException.class)
    public ResponseEntity<ErrorResponse> handleImprovementPlanUnprocessable(InvalidImprovementPlanException ex) {
        String code = "EXCLUSIVE_ARC_VIOLATION".equals(ex.getMessage())
                ? "EXCLUSIVE_ARC_VIOLATION" : "UNPROCESSABLE_ENTITY";
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(code, ex.getMessage()));
    }

    @ExceptionHandler(InstructorAvailabilityExceptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAvailabilityExceptionNotFound(
            InstructorAvailabilityExceptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateInstructorException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateInstructorException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        ex.getMessage(),
                        List.of(new ErrorDetail(ex.getField(), "DUPLICATE"))
                ));
    }

    @ExceptionHandler(LearnerConflictException.class)
    public ResponseEntity<ErrorResponse> handleLearnerConflict(LearnerConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        ex.getMessage(),
                        List.of(new ErrorDetail("documentNumber", "DUPLICATE"))
                ));
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        "Integrity conflict (unique or check constraint violated)"
                ));

    }

    @ExceptionHandler({
            StaleInstructorVersionException.class,
            ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponse> handleStaleVersion(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        "Concurrency conflict: stale row_version"
                ));
    }


    @ExceptionHandler(InvalidInstructorException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessable(InvalidInstructorException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(
                        "UNPROCESSABLE_ENTITY",
                        ex.getMessage(),
                        List.of(new ErrorDetail(ex.getField(), "INVALID_VALUE"))
                ));
    }

    @ExceptionHandler(UnsupportedScopeException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScope(UnsupportedScopeException ex) {
        String issue = ex.getScopes().contains("TRAINING_CENTER")
                || ex.getScopes().isEmpty()
                ? "TRAINING_CENTER_UNSUPPORTED"
                : "UNSUPPORTED_FILTER";

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "SCOPE_VIOLATION",
                        ex.getMessage(),
                        List.of(new ErrorDetail("scope", issue))
                ));
    }

    @ExceptionHandler(InstructorAreaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAreaNotFound(InstructorAreaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "Instructor area not found"));
    }

    @ExceptionHandler(DuplicateInstructorAreaException.class)
    public ResponseEntity<ErrorResponse> handleAreaConflict(DuplicateInstructorAreaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        ex.getMessage(),
                        List.of(new ErrorDetail(ex.getField(), "DUPLICATE"))
                ));
    }

    @ExceptionHandler(StaleInstructorAreaVersionException.class)
    public ResponseEntity<ErrorResponse> handleAreaStaleVersion(
            StaleInstructorAreaVersionException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        "Concurrency conflict: stale row_version"
                ));
    }

    @ExceptionHandler(InstructorContractNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContractNotFound(
            InstructorContractNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        "Instructor contract not found"
                ));
    }

    @ExceptionHandler(InvalidInstructorContractException.class)
    public ResponseEntity<ErrorResponse> handleContractUnprocessable(
            InvalidInstructorContractException ex) {

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(
                        "UNPROCESSABLE_ENTITY",
                        ex.getMessage(),
                        List.of(new ErrorDetail(ex.getField(), "INVALID_VALUE"))
                ));
    }

    @ExceptionHandler(InvalidLearnerEnrollmentException.class)
    public ResponseEntity<ErrorResponse> handleLearnerEnrollmentUnprocessable(
            InvalidLearnerEnrollmentException ex) {

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(
                        "UNPROCESSABLE_ENTITY",
                        ex.getMessage(),
                        List.of(new ErrorDetail(ex.getField(), "INVALID_VALUE"))
                ));
    }

    @ExceptionHandler(StaleInstructorContractVersionException.class)
    public ResponseEntity<ErrorResponse> handleContractStaleVersion(
            StaleInstructorContractVersionException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        "Concurrency conflict: stale row_version"
                ));
    }

    @ExceptionHandler(CompetencyAssignmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompetencyNotFound(
            CompetencyAssignmentNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        "Competency assignment not found"
                ));
    }

    @ExceptionHandler(DuplicateCompetencyAssignmentException.class)
    public ResponseEntity<ErrorResponse> handleCompetencyConflict(
            DuplicateCompetencyAssignmentException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        ex.getMessage(),
                        List.of(new ErrorDetail(ex.getField(), "DUPLICATE"))
                ));

    }



    private String messageToIssue(String message) {
        if (message == null || message.isBlank()) {
            return "INVALID";
        }

        return message.length() > 120
                ? message.substring(0, 120)
                : message;

    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(
                        "METHOD_NOT_ALLOWED",
                        "HTTP method not supported"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred"));
    }

}
