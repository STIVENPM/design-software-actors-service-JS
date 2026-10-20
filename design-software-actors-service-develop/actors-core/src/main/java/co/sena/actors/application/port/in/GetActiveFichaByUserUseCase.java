package co.sena.actors.application.port.in;

import java.util.UUID;

public interface GetActiveFichaByUserUseCase {

    ActiveFichaResult findActiveFichaByUserId(UUID userId);

    record ActiveFichaResult(UUID learnerId, UUID fichaId) {
    }
}