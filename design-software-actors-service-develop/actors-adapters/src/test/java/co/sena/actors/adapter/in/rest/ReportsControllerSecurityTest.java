package co.sena.actors.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.adapter.config.ReportRateLimiter;
import co.sena.actors.adapter.config.SecurityConfig;
import co.sena.actors.application.port.in.ReportPage;
import co.sena.actors.application.port.in.ReportsUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = ReportsControllerSecurityTest.TestApplication.class,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "actors.security.dev-jwt-decoder=true",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportsControllerSecurityTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
            org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
    })
    @Import({
            ReportsController.class,
            SecurityConfig.class,
            GlobalExceptionHandler.class,
            ReportRateLimiter.class
    })
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportsUseCase reportsUseCase;

    @Test
    @WithMockUser(authorities = "ACT_REPORT_VIEW")
    void authorizedUserCanAccessReport() throws Exception {
        when(reportsUseCase.learnerTracking(any())).thenReturn(new ReportPage<>(List.of(), 1, 20, 0, 0));

        mockMvc.perform(get("/api/v1/reports/learner-tracking")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "OTHER_ROLE")
    void userWithoutFeatureReceivesForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/learner-tracking")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/reports/instructor-load")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/learner-tracking")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ACT_REPORT_VIEW", username = "rate-limit-user")
    void returns429TooManyRequestsWhenRateLimitExceeded() throws Exception {
        when(reportsUseCase.learnerTracking(any())).thenReturn(new ReportPage<>(List.of(), 1, 20, 0, 0));
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/v1/reports/learner-tracking"));
        }
        mockMvc.perform(get("/api/v1/reports/learner-tracking"))
                .andExpect(status().isTooManyRequests());
    }
}