package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.DocumentType;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LearnerCreateRequest(
        UUID userId,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate) {}
