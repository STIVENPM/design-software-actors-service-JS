package co.sena.actors.adapter.config;

import co.sena.actors.adapter.in.rest.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalMtlsAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final String trustedSubject;

    public InternalMtlsAuthenticationFilter(
            ObjectMapper objectMapper,
            @Value("${actors.internal.mtls.trusted-subject:CN=iam-service}") String trustedSubject) {
        this.objectMapper = objectMapper;
        this.trustedSubject = trustedSubject;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/learners/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        X509Certificate[] certificates =
                (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certificates == null) {
            certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        }

        if (certificates == null || certificates.length == 0) {
            writeUnauthorized(response, "MTLS_CERTIFICATE_REQUIRED", "Client certificate required");
            return;
        }

        X500Principal certificateSubject = certificates[0].getSubjectX500Principal();
        final X500Principal configuredSubject;
        try {
            configuredSubject = new X500Principal(trustedSubject);
        } catch (IllegalArgumentException ex) {
            writeUnauthorized(response, "MTLS_TRUSTED_SUBJECT_INVALID", "Configured client identity is invalid");
            return;
        }
        if (!isTrustedSubject(certificateSubject, configuredSubject)) {
            writeUnauthorized(response, "MTLS_CERTIFICATE_UNTRUSTED", "Client certificate is not trusted");
            return;
        }

        String subject = certificateSubject.getName();

        var authentication = new UsernamePasswordAuthenticationToken(
                subject,
                "N/A",
                List.of(new SimpleGrantedAuthority("ACTORS_INTERNAL_IAM")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    static boolean isTrustedSubject(X500Principal certificateSubject, X500Principal configuredSubject) {
        return certificateSubject != null && configuredSubject != null && certificateSubject.equals(configuredSubject);
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message));
    }
}
