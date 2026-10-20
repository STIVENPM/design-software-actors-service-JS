package co.sena.actors.application.port.in;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InstructorLoadReportQuery(
        int page,
        int pageSize,
        String sort,
        UUID trainingCenterId,
        String area,
        OffsetDateTime from,
        OffsetDateTime to) {
}