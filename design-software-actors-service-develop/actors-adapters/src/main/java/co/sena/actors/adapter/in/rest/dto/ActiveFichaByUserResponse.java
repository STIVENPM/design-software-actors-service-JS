package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ActiveFichaByUserResponse(
        UUID learnerId,
        UUID fichaId) {

    public static ActiveFichaByUserResponse from(
            GetActiveFichaByUserUseCase.ActiveFichaResult result) {

        return new ActiveFichaByUserResponse(
                result.learnerId(),
                result.fichaId());
    }
}