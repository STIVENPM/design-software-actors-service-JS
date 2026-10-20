package co.sena.actors.adapter.in.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.application.port.in.CountActiveLearnersByFichaUseCase;
import co.sena.actors.application.usecase.FichaNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalFichaLearnerControllerTest {

    private final CountActiveLearnersByFichaUseCase useCase = mock(CountActiveLearnersByFichaUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalFichaLearnerController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void existingEmptyFichaReturnsZero() throws Exception {
        UUID fichaId = UUID.randomUUID();

        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", fichaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficha_id").value(fichaId.toString()))
                .andExpect(jsonPath("$.active_learners").value(0));
    }

    @Test
    void unknownFichaReturnsStandardError() throws Exception {
        UUID fichaId = UUID.randomUUID();
        when(useCase.countActiveLearners(fichaId)).thenThrow(new FichaNotFoundException());

        mockMvc.perform(get("/internal/fichas/{fichaId}/active-learners/count", fichaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }
}
