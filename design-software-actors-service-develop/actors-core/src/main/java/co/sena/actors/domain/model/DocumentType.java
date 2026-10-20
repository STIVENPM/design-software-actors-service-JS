package co.sena.actors.domain.model;

/**
 * Identity document type (CHECK ck_instructor_document_type).
 */
public enum DocumentType {
    CC, CE, PA, PE;

    public static DocumentType of(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("document_type is invalid: " + value);
    }
}
