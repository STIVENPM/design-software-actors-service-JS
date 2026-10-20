package co.sena.actors.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public record LearnerTrackingReportQuery(
        int page,
        int pageSize,
        String sort,
        UUID fichaId,
        String enrollmentStatus,
        LocalDate from,
        LocalDate to) {
}