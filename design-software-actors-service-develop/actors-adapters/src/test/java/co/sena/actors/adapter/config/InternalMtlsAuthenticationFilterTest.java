package co.sena.actors.adapter.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;

class InternalMtlsAuthenticationFilterTest {

    private static final X500Principal IAM = new X500Principal("CN=iam-service");

    @Test
    void acceptsTheConfiguredIdentityExactly() {
        assertTrue(InternalMtlsAuthenticationFilter.isTrustedSubject(
                new X500Principal("CN=iam-service"), IAM));
    }

    @Test
    void rejectsAnIdentityThatOnlyContainsTheConfiguredName() {
        assertFalse(InternalMtlsAuthenticationFilter.isTrustedSubject(
                new X500Principal("CN=not-iam-service"), IAM));
    }

    @Test
    void rejectsASimilarOrDifferentIdentity() {
        assertFalse(InternalMtlsAuthenticationFilter.isTrustedSubject(
                new X500Principal("CN=iam-service-client"), IAM));
        assertFalse(InternalMtlsAuthenticationFilter.isTrustedSubject(
                new X500Principal("CN=actors-service"), IAM));
    }
}
