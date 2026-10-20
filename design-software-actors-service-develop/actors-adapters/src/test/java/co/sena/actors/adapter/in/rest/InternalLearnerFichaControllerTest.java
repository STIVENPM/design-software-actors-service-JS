package co.sena.actors.adapter.in.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase;
import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase.ActiveFichaResult;
import co.sena.actors.application.usecase.ActiveFichaInconsistencyException;
import co.sena.actors.application.usecase.LearnerNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalLearnerFichaControllerTest {

    private final GetActiveFichaByUserUseCase useCase =
            mock(GetActiveFichaByUserUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalLearnerFichaController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsLearnerWithActiveFicha() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        UUID fichaId = UUID.randomUUID();
        when(useCase.findActiveFichaByUserId(userId))
                .thenReturn(new ActiveFichaResult(learnerId, fichaId));

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learner_id").value(learnerId.toString()))
                .andExpect(jsonPath("$.ficha_id").value(fichaId.toString()));
    }

    @Test
    void returnsNoContentWhenLearnerHasNoActiveFicha() throws Exception {
        UUID userId = UUID.randomUUID();
        when(useCase.findActiveFichaByUserId(userId))
                .thenReturn(new ActiveFichaResult(UUID.randomUUID(), null));

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenLearnerDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(useCase.findActiveFichaByUserId(userId))
                .thenThrow(new LearnerNotFoundException("Learner not found"));

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void returnsConflictWhenLearnerHasMultipleActiveFichas() throws Exception {
        UUID userId = UUID.randomUUID();
        when(useCase.findActiveFichaByUserId(userId))
                .thenThrow(new ActiveFichaInconsistencyException("Multiple active fichas"));

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("Multiple active fichas"));
    }

    @Test
    void returnsInternalServerErrorForTechnicalFailure() throws Exception {
        UUID userId = UUID.randomUUID();
        when(useCase.findActiveFichaByUserId(userId))
                .thenThrow(new IllegalStateException("Database unavailable"));

        mockMvc.perform(get("/internal/learners/by-user/{user_id}/active-ficha", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }
}
