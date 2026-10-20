package co.sena.actors.adapter.config;

import co.sena.actors.adapter.in.rest.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * iam-service RS256 JWT verified locally (authentication.md).
 * The features[] claim carries FEATURE:SCOPE; it is exposed as a
 * FEATURE authority (any scope) and as FEATURE:SCOPE (exact scope).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@SuppressWarnings("null") // Spring DSL builders carry no null contract; editor-only hints, javac is clean
public class SecurityConfig {

    @Bean
    InternalMtlsAuthenticationFilter internalMtlsAuthenticationFilter(
            ObjectMapper objectMapper,
            @Value("${actors.internal.mtls.trusted-subject:CN=iam-service}") String trustedSubject) {
        return new InternalMtlsAuthenticationFilter(objectMapper, trustedSubject);
    }

    @Bean
    @Order(1)
    SecurityFilterChain internalLearnerMtlsSecurityFilterChain(
            HttpSecurity http,
            InternalMtlsAuthenticationFilter internalMtlsAuthenticationFilter,
            org.springframework.security.web.AuthenticationEntryPoint actorsEntryPoint,
            org.springframework.security.web.access.AccessDeniedHandler actorsDeniedHandler) throws Exception {

        return http
                .securityMatcher("/internal/learners/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalMtlsAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(actorsEntryPoint)
                        .accessDeniedHandler(actorsDeniedHandler))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            org.springframework.security.web.AuthenticationEntryPoint actorsEntryPoint,
            org.springframework.security.web.access.AccessDeniedHandler actorsDeniedHandler) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();
            Object features = jwt.getClaim("features");
            if (features instanceof Collection<?> items) {
                for (Object item : items) {
                    if (item instanceof String feature && !feature.isBlank()) {
                        String value = feature.trim();
                        authorities.add(new SimpleGrantedAuthority(value));
                        int separator = value.indexOf(':');
                        if (separator > 0) {
                            authorities.add(new SimpleGrantedAuthority(value.substring(0, separator)));
                        }
                    }
                }
            }
            return authorities;
        });
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/internal/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(actorsEntryPoint))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(actorsEntryPoint)
                        .accessDeniedHandler(actorsDeniedHandler))
                .build();
    }

    /**
     * Same entry point for both 401 paths: missing token (via
     * ExceptionTranslationFilter) and invalid/expired token (via
     * BearerTokenAuthenticationFilter, which IGNORES the exceptionHandling
     * one and falls back to its own default unless wired here).
     */
    @Bean
    org.springframework.security.web.AuthenticationEntryPoint actorsEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), unauthorizedBody(request, ex));
        };
    }

    @Bean
    org.springframework.security.web.access.AccessDeniedHandler actorsDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ErrorResponse.of("INSUFFICIENT_PERMISSIONS", "Feature no presente en JWT"));
        };
    }
    /**
     * Codes from 07-api/authentication.md § Standard errors.
     * TOKEN_REVOKED cannot be verified locally (the denylist lives in
     * iam-service, V2 roadmap) and is therefore never emitted here.
     */
    private static ErrorResponse unauthorizedBody(
            jakarta.servlet.http.HttpServletRequest request, Exception ex) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return ErrorResponse.of("MISSING_TOKEN", "Falta el header Authorization con Bearer token");
        }
        if (isExpired(ex)) {
            return ErrorResponse.of("TOKEN_EXPIRED", "Token expirado");
        }
        return ErrorResponse.of("TOKEN_INVALID", "Token malformado o firma inválida");
    }

    private static boolean isExpired(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof org.springframework.security.oauth2.jwt.JwtValidationException validation) {
                boolean expired = validation.getErrors().stream().anyMatch(error ->
                        (error.getDescription() != null
                                && error.getDescription().toLowerCase(java.util.Locale.ROOT).contains("expired"))
                        || (error.getErrorCode() != null
                                && error.getErrorCode().toLowerCase(java.util.Locale.ROOT).contains("expired")));
                if (expired) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("expired")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fallback JwtDecoder for local development and tests ONLY.
     * Double lock: dev/test profiles AND explicit
     * actors.security.dev-jwt-decoder=true property (enabled in
     * application-dev.yml / application-test.yml, never in the base
     * application.yml). In QA/staging/prod this bean does not exist:
     * only the real decoder against the IAM JWKS is valid; if the IAM
     * configuration is missing there, startup fails (loudly, safely).
     * ITs override it with @MockBean.
     */
    @Bean
    @org.springframework.context.annotation.Profile({"dev", "test"})
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "actors.security.dev-jwt-decoder", havingValue = "true")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder devJwtDecoder() {
        com.nimbusds.jose.jwk.RSAKey rsaKey;
        try {
            rsaKey = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048)
                    .keyID("actors-dev-key")
                    .generate();
        } catch (com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException("No se pudo generar clave JWT de desarrollo", ex);
        }
        try {
            return org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                    .withPublicKey(rsaKey.toRSAPublicKey())
                    .build();
        } catch (com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException("Clave JWT de desarrollo inválida", ex);
        }
    }
}
