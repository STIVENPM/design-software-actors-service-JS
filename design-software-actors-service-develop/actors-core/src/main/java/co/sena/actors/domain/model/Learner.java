package co.sena.actors.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Learner(
        UUID id,
        UUID userId,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        boolean isActive,
        LocalDateTime createdAt) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    public static Learner create(
            UUID id,
            UUID userId,
            DocumentType documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate birthDate) {
        if (userId == null) {
            throw new IllegalArgumentException("user_id is required");
        }
        if (documentType == null) {
            throw new IllegalArgumentException("document_type is required");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("document_number is required");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("first_name is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("last_name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        return new Learner(
                id,
                userId,
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                birthDate,
                true,
                LocalDateTime.now());
    }

    public Learner update(
            DocumentType documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate birthDate) {
        
        return new Learner(
                this.id,
                this.userId, // immutable
                documentType != null ? documentType : this.documentType,
                documentNumber != null ? documentNumber : this.documentNumber,
                firstName != null ? firstName : this.firstName,
                lastName != null ? lastName : this.lastName,
                email != null ? email : this.email,
                phone, // can be null
                birthDate, // can be null
                this.isActive,
                this.createdAt); // immutable
    }

    public Learner deactivate() {
        return new Learner(id, userId, documentType, documentNumber, firstName, lastName,
                email, phone, birthDate, false, createdAt);
    }
}
