package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.in.rest.dto.ActiveLearnersCountResponse;
import co.sena.actors.application.port.in.CountActiveLearnersByFichaUseCase;
import io.micrometer.observation.annotation.Observed;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/fichas")
@Observed(name = "actors.internal.ficha-learners.controller")
public class InternalFichaLearnerController {

    private final CountActiveLearnersByFichaUseCase useCase;

    public InternalFichaLearnerController(CountActiveLearnersByFichaUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{fichaId}/active-learners/count")
    @PreAuthorize("hasAuthority('ACT_INTERNAL_FICHA_LEARNER_COUNT')")
    public ResponseEntity<ActiveLearnersCountResponse> countActiveLearners(@PathVariable UUID fichaId) {
        return ResponseEntity.ok(new ActiveLearnersCountResponse(fichaId, useCase.countActiveLearners(fichaId)));
    }
}
