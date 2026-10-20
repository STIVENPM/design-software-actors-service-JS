package co.sena.actors.application.port.in;

public interface ReportsUseCase {

    ReportPage<LearnerTrackingReportEntry> learnerTracking(LearnerTrackingReportQuery query);

    ReportPage<InstructorLoadReportEntry> instructorLoad(InstructorLoadReportQuery query);
}