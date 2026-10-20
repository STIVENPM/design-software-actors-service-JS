package co.sena.actors.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ActiveLearnersCountResponse(UUID fichaId, long activeLearners) {}
