package co.sena.actors.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public record LearnerTrackingReportEntry(
        UUID learnerId,
        String documentNumber,
        String fullName,
        String enrollmentStatus,
        UUID fichaId,
        String stage,
        LocalDate fichaEnrollmentDate,
        String productiveStageStatus,
        UUID productiveStageCompanyId) {
}