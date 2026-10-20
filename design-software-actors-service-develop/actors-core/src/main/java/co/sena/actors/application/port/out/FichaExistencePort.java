package co.sena.actors.application.port.out;

import java.util.UUID;

public interface FichaExistencePort {

    boolean existsById(UUID fichaId);
}
