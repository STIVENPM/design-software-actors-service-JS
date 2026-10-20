package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.Learner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LearnerDto(
        UUID id,
        UUID userId,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        LocalDateTime createdAt) {

    public static LearnerDto from(Learner learner) {
        return new LearnerDto(
                learner.id(),
                learner.userId(),
                learner.documentType().name(),
                learner.documentNumber(),
                learner.firstName(),
                learner.lastName(),
                learner.email(),
                learner.phone(),
                learner.birthDate(),
                learner.createdAt());
    }
}
