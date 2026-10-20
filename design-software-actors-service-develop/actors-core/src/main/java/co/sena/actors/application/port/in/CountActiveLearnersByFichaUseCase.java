package co.sena.actors.application.port.in;

import java.util.UUID;

public interface CountActiveLearnersByFichaUseCase {

    long countActiveLearners(UUID fichaId);
}
