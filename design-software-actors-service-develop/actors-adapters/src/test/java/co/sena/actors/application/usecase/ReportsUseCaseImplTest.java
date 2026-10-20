package co.sena.actors.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.sena.actors.adapter.in.rest.dto.InstructorLoadReportResponse;
import co.sena.actors.adapter.in.rest.dto.LearnerTrackingReportResponse;
import co.sena.actors.application.port.in.InstructorLoadReportEntry;
import co.sena.actors.application.port.in.InstructorLoadReportQuery;
import co.sena.actors.application.port.in.LearnerTrackingReportEntry;
import co.sena.actors.application.port.in.LearnerTrackingReportQuery;
import co.sena.actors.application.port.in.ReportPage;
import co.sena.actors.application.port.out.ReportsRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportsUseCaseImplTest {

    private final StubReportsRepository repository = new StubReportsRepository();
    private final ReportsUseCaseImpl useCase = new ReportsUseCaseImpl(repository);

    @Test
    void learnerTrackingBuildsOffsetPagination() {
        ReportPage<LearnerTrackingReportEntry> page = useCase.learnerTracking(
                new LearnerTrackingReportQuery(
                        2, 2, null, null, "ACTIVE", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)));

        assertEquals(2, page.page());
        assertEquals(2, page.pageSize());
        assertEquals(3, page.totalItems());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.data().size());
        assertEquals("ACTIVE", repository.learnerQuery.enrollmentStatus());
    }

    @Test
    void instructorLoadAcceptsNullContractLimit() {
        ReportPage<InstructorLoadReportEntry> page = useCase.instructorLoad(
                new InstructorLoadReportQuery(1, 20, null, null, "Software", null, null));

        assertEquals(1, page.totalItems());
        assertEquals(null, page.data().get(0).currentContractWeeklyHourLimit());
        assertEquals("Software", repository.instructorQuery.area());
    }

    @Test
    void rejectsUnsupportedEnrollmentStatus() {
        assertThrows(IllegalArgumentException.class, () -> useCase.learnerTracking(
                new LearnerTrackingReportQuery(1, 20, null, null, "UNKNOWN", null, null)));
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () -> useCase.instructorLoad(
                new InstructorLoadReportQuery(
                        1, 20, null, null, null,
                        java.time.OffsetDateTime.parse("2026-02-01T00:00:00Z"),
                        java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"))));
    }

    @Test
    void rejectsPageSizeAboveContractMaximum() {
        assertThrows(IllegalArgumentException.class, () -> useCase.instructorLoad(
                new InstructorLoadReportQuery(1, 101, null, null, null, null, null)));
    }

    @Test
    void learnerTrackingPaginationSerializesSnakeCase() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new LearnerTrackingReportResponse(
                List.of(),
                new LearnerTrackingReportResponse.Pagination(1, 20, 42, 3)));

        assertEquals(true, json.contains("\"page_size\":20"));
        assertEquals(true, json.contains("\"total_items\":42"));
        assertEquals(true, json.contains("\"total_pages\":3"));
    }

    @Test
    void instructorLoadPaginationSerializesSnakeCase() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new InstructorLoadReportResponse(
                List.of(),
                new InstructorLoadReportResponse.Pagination(1, 20, 42, 3)));

        assertEquals(true, json.contains("\"page_size\":20"));
        assertEquals(true, json.contains("\"total_items\":42"));
        assertEquals(true, json.contains("\"total_pages\":3"));
    }

    private static final class StubReportsRepository implements ReportsRepositoryPort {

        private LearnerTrackingReportQuery learnerQuery;
        private InstructorLoadReportQuery instructorQuery;

        @Override
        public List<LearnerTrackingReportEntry> findLearnerTracking(LearnerTrackingReportQuery query) {
            learnerQuery = query;
            return List.of(new LearnerTrackingReportEntry(
                    UUID.randomUUID(), "100", "Ana Torres", "ACTIVE", UUID.randomUUID(),
                    "LECTURE", LocalDate.of(2026, 1, 15), null, null));
        }

        @Override
        public long countLearnerTracking(LearnerTrackingReportQuery query) {
            return 3;
        }

        @Override
        public List<InstructorLoadReportEntry> findInstructorLoad(InstructorLoadReportQuery query) {
            instructorQuery = query;
            return List.of(new InstructorLoadReportEntry(
                    UUID.randomUUID(), "200", "Luis Perez", BigDecimal.valueOf(40), null,
                    null, 1, 2, 0));
        }

        @Override
        public long countInstructorLoad(InstructorLoadReportQuery query) {
            return 1;
        }
    }
}