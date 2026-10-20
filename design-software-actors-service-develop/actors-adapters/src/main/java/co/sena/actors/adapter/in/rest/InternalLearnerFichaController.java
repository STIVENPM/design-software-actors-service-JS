package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.in.rest.dto.ActiveFichaByUserResponse;
import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase;
import io.micrometer.observation.annotation.Observed;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/learners")
@Observed(name = "actors.internal.learner-active-ficha.controller")
public class InternalLearnerFichaController {

    private final GetActiveFichaByUserUseCase useCase;

    public InternalLearnerFichaController(GetActiveFichaByUserUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/by-user/{user_id}/active-ficha")
    public ResponseEntity<ActiveFichaByUserResponse> getActiveFichaByUser(
            @PathVariable("user_id") UUID userId) {

        var result = useCase.findActiveFichaByUserId(userId);

        if (result.fichaId() == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(ActiveFichaByUserResponse.from(result));
    }
}