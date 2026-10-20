package co.sena.actors.application.port.out;

import co.sena.actors.application.port.in.InstructorLoadReportEntry;
import co.sena.actors.application.port.in.InstructorLoadReportQuery;
import co.sena.actors.application.port.in.LearnerTrackingReportEntry;
import co.sena.actors.application.port.in.LearnerTrackingReportQuery;
import java.util.List;

public interface ReportsRepositoryPort {

    List<LearnerTrackingReportEntry> findLearnerTracking(LearnerTrackingReportQuery query);

    long countLearnerTracking(LearnerTrackingReportQuery query);

    List<InstructorLoadReportEntry> findInstructorLoad(InstructorLoadReportQuery query);

    long countInstructorLoad(InstructorLoadReportQuery query);
}