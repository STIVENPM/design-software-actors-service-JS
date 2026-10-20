package co.sena.actors.domain.model;

/**
 * Instructor contract type (CHECK ck_instructor_contract_contract_type).
 */
public enum ContractType {
    STAFF, CONTRACTOR, HOURLY;

    public static ContractType of(String value) {
        if (value == null) {
            return null;
        }
        for (ContractType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("contract_type is invalid: " + value);
    }
}