package co.sena.actors.adapter.config;

import co.sena.actors.application.port.in.BrowseCompetencyAssignmentsUseCase;
import co.sena.actors.application.port.in.BrowseInstructorsUseCase;
import co.sena.actors.application.port.in.BrowseInstructorAreasUseCase;
import co.sena.actors.application.port.in.BrowseInstructorContractsUseCase;
import co.sena.actors.application.port.in.CountActiveLearnersByFichaUseCase;
import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase;
import co.sena.actors.application.port.in.ManageCompetencyAssignmentUseCase;
import co.sena.actors.application.port.in.ManageInstructorUseCase;
import co.sena.actors.application.port.in.ManageInstructorAreaUseCase;
import co.sena.actors.application.port.in.ManageInstructorContractUseCase;
import co.sena.actors.application.port.in.ManageLearnerFichaEnrollmentUseCase;
import co.sena.actors.application.port.in.QueryLearnerFichaEnrollmentUseCase;
import co.sena.actors.application.port.in.ManageLearnerUseCase;
import co.sena.actors.application.port.in.QueryLearnerUseCase;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.application.port.out.FichaExistencePort;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.application.port.out.InstructorContractRepositoryPort;
import co.sena.actors.application.port.out.LearnerEnrollmentPort;
import co.sena.actors.application.port.out.LearnerEnrollmentEventPublisherPort;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.application.port.out.ReportsRepositoryPort;
import co.sena.actors.application.port.out.ImprovementPlanRepositoryPort;
import co.sena.actors.application.usecase.BrowseCompetencyAssignmentsUseCaseImpl;
import co.sena.actors.application.usecase.BrowseInstructorsUseCaseImpl;
import co.sena.actors.application.usecase.BrowseInstructorAreasUseCaseImpl;
import co.sena.actors.application.usecase.BrowseInstructorContractsUseCaseImpl;
import co.sena.actors.application.usecase.CountActiveLearnersByFichaUseCaseImpl;
import co.sena.actors.application.usecase.GetActiveFichaByUserUseCaseImpl;
import co.sena.actors.application.usecase.InstructorViewerAccess;
import co.sena.actors.application.usecase.ManageCompetencyAssignmentUseCaseImpl;
import co.sena.actors.application.usecase.ManageInstructorUseCaseImpl;
import co.sena.actors.application.usecase.ManageInstructorAreaUseCaseImpl;
import co.sena.actors.application.usecase.ManageInstructorContractUseCaseImpl;
import co.sena.actors.application.usecase.GetInstructorActivityLogUseCaseImpl;
import co.sena.actors.application.usecase.GetLearnerActivityLogUseCaseImpl;
import co.sena.actors.application.usecase.ManageLearnerFichaEnrollmentUseCaseImpl;
import co.sena.actors.application.usecase.ManageLearnerUseCaseImpl;
import co.sena.actors.application.usecase.QueryLearnerFichaEnrollmentUseCaseImpl;
import co.sena.actors.application.usecase.QueryLearnerUseCaseImpl;
import co.sena.actors.application.port.in.ReportsUseCase;
import co.sena.actors.application.usecase.ReportsUseCaseImpl;
import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import co.sena.actors.application.usecase.ManageImprovementPlanUseCaseImpl;
import co.sena.actors.application.port.in.GetInstructorActivityLogUseCase;
import co.sena.actors.application.port.in.GetLearnerActivityLogUseCase;
import co.sena.actors.core.port.in.InstructorAvailabilityExceptionUseCase;
import co.sena.actors.core.port.out.AvailabilityEventPublisherPort;
import co.sena.actors.core.port.out.InstructorAvailabilityExceptionRepositoryPort;
import co.sena.actors.core.service.InstructorAvailabilityExceptionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Hexagonal wiring: the core knows nothing about Spring. */
@Configuration
public class UseCaseConfig {

    @Bean
    ManageInstructorUseCase manageInstructorUseCase(InstructorRepositoryPort repository) {
        return new ManageInstructorUseCaseImpl(repository);
    }

    @Bean
    BrowseInstructorsUseCase browseInstructorsUseCase(InstructorRepositoryPort repository) {
        return new BrowseInstructorsUseCaseImpl(repository);
    }

    @Bean
    InstructorViewerAccess instructorViewerAccess(InstructorRepositoryPort instructors) {
        return new InstructorViewerAccess(instructors);
    }

    @Bean
    ManageInstructorAreaUseCase manageInstructorAreaUseCase(
            InstructorViewerAccess access, InstructorAreaRepositoryPort repository) {
        return new ManageInstructorAreaUseCaseImpl(access, repository);
    }

    @Bean
    BrowseInstructorAreasUseCase browseInstructorAreasUseCase(
            InstructorViewerAccess access, InstructorAreaRepositoryPort repository) {
        return new BrowseInstructorAreasUseCaseImpl(access, repository);
    }

    @Bean
    ManageInstructorContractUseCase manageInstructorContractUseCase(
            InstructorViewerAccess access, InstructorContractRepositoryPort repository) {
        return new ManageInstructorContractUseCaseImpl(access, repository);
    }

    @Bean
    BrowseInstructorContractsUseCase browseInstructorContractsUseCase(
            InstructorViewerAccess access, InstructorContractRepositoryPort repository) {
        return new BrowseInstructorContractsUseCaseImpl(access, repository);
    }

    @Bean
    ManageCompetencyAssignmentUseCase manageCompetencyAssignmentUseCase(
            InstructorViewerAccess access, CompetencyAssignmentRepositoryPort repository) {
        return new ManageCompetencyAssignmentUseCaseImpl(access, repository);
    }

    @Bean
    BrowseCompetencyAssignmentsUseCase browseCompetencyAssignmentsUseCase(
            InstructorViewerAccess access, CompetencyAssignmentRepositoryPort repository) {
        return new BrowseCompetencyAssignmentsUseCaseImpl(access, repository);
    }

    @Bean
    GetInstructorActivityLogUseCase getInstructorActivityLogUseCase(ActivityLogRepositoryPort repository) {
        return new GetInstructorActivityLogUseCaseImpl(repository);
    }

    @Bean
    GetLearnerActivityLogUseCase getLearnerActivityLogUseCase(ActivityLogRepositoryPort repository) {
        return new GetLearnerActivityLogUseCaseImpl(repository);
    }

    @Bean
    public ManageLearnerUseCase manageLearnerUseCase(
            LearnerRepositoryPort learnerRepositoryPort,
            LearnerEnrollmentPort learnerEnrollmentPort) {
        return new ManageLearnerUseCaseImpl(learnerRepositoryPort, learnerEnrollmentPort);
    }

    @Bean
    public QueryLearnerUseCase queryLearnerUseCase(LearnerRepositoryPort learnerRepositoryPort) {
        return new QueryLearnerUseCaseImpl(learnerRepositoryPort);
    }

    @Bean
    public GetActiveFichaByUserUseCase getActiveFichaByUserUseCase(
            LearnerRepositoryPort learnerRepositoryPort,
            LearnerFichaEnrollmentRepositoryPort enrollmentRepositoryPort) {
        return new GetActiveFichaByUserUseCaseImpl(
                learnerRepositoryPort,
                enrollmentRepositoryPort);
    }

    @Bean
    public ManageLearnerFichaEnrollmentUseCase manageLearnerFichaEnrollmentUseCase(
            LearnerRepositoryPort learnerRepositoryPort,
            LearnerFichaEnrollmentRepositoryPort learnerFichaEnrollmentRepositoryPort,
            LearnerEnrollmentEventPublisherPort learnerEnrollmentEventPublisherPort) {
        return new ManageLearnerFichaEnrollmentUseCaseImpl(
                learnerRepositoryPort,
                learnerFichaEnrollmentRepositoryPort,
                learnerEnrollmentEventPublisherPort);
    }

    @Bean
    public QueryLearnerFichaEnrollmentUseCase queryLearnerFichaEnrollmentUseCase(
            LearnerRepositoryPort learnerRepositoryPort,
            LearnerFichaEnrollmentRepositoryPort learnerFichaEnrollmentRepositoryPort) {
        return new QueryLearnerFichaEnrollmentUseCaseImpl(
                learnerRepositoryPort,
                learnerFichaEnrollmentRepositoryPort);
    }

    @Bean
    public CountActiveLearnersByFichaUseCase countActiveLearnersByFichaUseCase(
            LearnerFichaEnrollmentRepositoryPort repository, FichaExistencePort fichas) {
        return new CountActiveLearnersByFichaUseCaseImpl(repository, fichas);
    }

    @Bean
    @ConditionalOnBean(ReportsRepositoryPort.class)
    ReportsUseCase reportsUseCase(ReportsRepositoryPort repository) {
        return new ReportsUseCaseImpl(repository);
    }

    @Bean
    public ManageImprovementPlanUseCase manageImprovementPlanUseCase(
            ImprovementPlanRepositoryPort repository) {
        return new ManageImprovementPlanUseCaseImpl(repository);
    }

    @Bean
    InstructorAvailabilityExceptionUseCase instructorAvailabilityExceptionUseCase(
            InstructorAvailabilityExceptionRepositoryPort repositoryPort,
            InstructorRepositoryPort instructorRepositoryPort,
            AvailabilityEventPublisherPort eventPublisherPort) {
        return new InstructorAvailabilityExceptionService(
                repositoryPort,
                instructorRepositoryPort,
                eventPublisherPort);
    }
}
