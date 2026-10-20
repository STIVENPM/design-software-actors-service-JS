package co.sena.actors.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.actors.adapter.out.persistence.InstructorJpaEntity;
import co.sena.actors.adapter.out.persistence.InstructorJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * ITs against a real Postgres (no persistence mocks): they use the development
 * Postgres (schema actors_instructors deployed by Liquibase from
 * design-software-actors-db, untouched). Every test is @Transactional and rolls
 * its rows back. In CI point it with ACTORS_DB_URL/ACTORS_DB_USER/ACTORS_DB_PASSWORD.
 * Covers E1-E4 without persistence mocks. TDD: these tests define the contract.
 */
@SpringBootTest(classes = ActorsApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null") // MockMvc/Jackson builders carry no null contract; editor-only hints, javac is clean
class InstructorIT extends ActorsApiPostgresIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    InstructorJpaRepository repository;

    @MockBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void stubDecoder() {
        // Without a stub the mock returns null and a real Authorization header would yield 500.
        // The decoder is never used with jwt() (it injects authentication directly).
        org.mockito.Mockito.lenient().when(jwtDecoder.decode(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new BadJwtException("Invalid bearer token"));
    }

    // ------------------------------------------------------------------ helpers

    private JwtRequestPostProcessor manageJwt() {
        return jwt()
                .authorities(
                        new SimpleGrantedAuthority("ACT_INSTRUCTOR_MANAGE:GLOBAL"),
                        new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:GLOBAL"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
    }

    private JwtRequestPostProcessor viewJwt() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:GLOBAL"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
    }

    private JwtRequestPostProcessor ownProfileJwt(UUID actorId) {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:OWN_PROFILE"))
                .jwt(j -> j.subject(UUID.randomUUID().toString())
                        .claim("actor_id", actorId.toString()));
    }

    private JwtRequestPostProcessor trainingCenterViewJwt() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:TRAINING_CENTER"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
    }

    private JwtRequestPostProcessor trainingCenterViewJwt(UUID centerId) {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:TRAINING_CENTER"))
                .jwt(j -> j.subject(UUID.randomUUID().toString())
                        .claim("training_center_id", centerId.toString()));
    }

    private JwtRequestPostProcessor trainingCenterManageJwt() {
        return jwt()
                .authorities(
                        new SimpleGrantedAuthority("ACT_INSTRUCTOR_MANAGE:TRAINING_CENTER"),
                        new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:TRAINING_CENTER"))
                .jwt(j -> j.subject(UUID.randomUUID().toString())
                        .claim("training_center_id", UUID.randomUUID().toString()));
    }

    private Map<String, Object> createPayload(String documentNumber, UUID userId, BigDecimal hours) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", userId.toString());
        body.put("document_type", "CC");
        body.put("document_number", documentNumber);
        body.put("first_name", "Ana");
        body.put("last_name", "Torres");
        body.put("email", "ana." + documentNumber.toLowerCase() + "@sena.edu.co");
        body.put("phone", "3001234567");
        body.put("default_max_hours_per_week", hours);
        return body;
    }

    private String doc() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    private String createInstructor(String documentNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(documentNumber, UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ------------------------------------------------------------------ E1 creation

    @Test
    void createReturns201WithDerivedFullName() throws Exception {
        String documentNumber = doc();
        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(documentNumber, UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.full_name").value("Ana Torres"))
                .andExpect(jsonPath("$.document_type").value("CC"))
                .andExpect(jsonPath("$.document_number").value(documentNumber))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.row_version").value(0));
    }

    @Test
    void duplicateDocumentNumberReturns409() throws Exception {
        String documentNumber = doc();
        createInstructor(documentNumber);

        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(documentNumber, UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void duplicateUserIdReturns409() throws Exception {
        String first = doc();
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload(first, userId, new BigDecimal("40.0")))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload(doc(), userId, new BigDecimal("40.0")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void negativeHoursOnCreateReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(doc(), UUID.randomUUID(), new BigDecimal("-1.0")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidDocumentTypeReturns400() throws Exception {
        Map<String, Object> body = createPayload(doc(), UUID.randomUUID(), new BigDecimal("40.0"));
        body.put("document_type", "XX");

        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingEmailReturns400() throws Exception {
        Map<String, Object> body = createPayload(doc(), UUID.randomUUID(), new BigDecimal("40.0"));
        body.remove("email");

        mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(doc(), UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_TOKEN"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/instructors")
                        .header("Authorization", "Bearer token-forjado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(doc(), UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        org.mockito.Mockito.doThrow(new JwtValidationException("Unable to validate token",
                        List.of(new OAuth2Error("invalid_token", "Jwt expired at 2026-01-01", null))))
                .when(jwtDecoder).decode(org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(get("/api/v1/instructors").header("Authorization", "Bearer expirado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void viewOnlyCannotCreate403() throws Exception {
        mockMvc.perform(post("/api/v1/instructors")
                        .with(viewJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(doc(), UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    // ------------------------------------------------------------------ E2 query

    @Test
    void getReturns200() throws Exception {
        String id = createInstructor(doc());

        mockMvc.perform(get("/api/v1/instructors/{id}", id).with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.full_name").value("Ana Torres"));
    }

    @Test
    void getMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/instructors/{id}", UUID.randomUUID()).with(viewJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void getSoftDeletedReturns404() throws Exception {
        String id = createInstructor(doc());
        mockMvc.perform(delete("/api/v1/instructors/{id}", id).with(manageJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/instructors/{id}", id).with(viewJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void listOnlyActiveByDefault() throws Exception {
        String activeId = createInstructor(doc());
        String deletedId = createInstructor(doc());
        mockMvc.perform(delete("/api/v1/instructors/{id}", deletedId).with(manageJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/instructors").with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeId));
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/instructors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_TOKEN"));
    }

    // ------------------------------------------------------------------ E3 update

    @Test
    void putReplacesFullyAndBumpsRowVersion() throws Exception {
        String id = createInstructor(doc());
        Map<String, Object> put = new LinkedHashMap<>();
        put.put("document_type", "CE");
        put.put("document_number", doc());
        put.put("first_name", "Luis");
        put.put("last_name", "Pardo");
        put.put("email", "luis.pardo@sena.edu.co");
        put.put("phone", "3109876543");
        put.put("default_max_hours_per_week", new BigDecimal("48.0"));

        mockMvc.perform(put("/api/v1/instructors/{id}", id)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(put)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Luis Pardo"))
                .andExpect(jsonPath("$.document_type").value("CE"))
                .andExpect(jsonPath("$.row_version").value(1));
    }

    @Test
    void putNegativeHoursReturns422() throws Exception {
        String id = createInstructor(doc());
        Map<String, Object> put = new LinkedHashMap<>();
        put.put("document_type", "CC");
        put.put("document_number", doc());
        put.put("first_name", "Ana");
        put.put("last_name", "Torres");
        put.put("email", "ana.torres@sena.edu.co");
        put.put("default_max_hours_per_week", new BigDecimal("-5.0"));

        mockMvc.perform(put("/api/v1/instructors/{id}", id)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(put)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void putMissingReturns404() throws Exception {
        Map<String, Object> put = new LinkedHashMap<>();
        put.put("document_type", "CC");
        put.put("document_number", doc());
        put.put("first_name", "Ana");
        put.put("last_name", "Torres");
        put.put("email", "ana.torres@sena.edu.co");
        put.put("default_max_hours_per_week", new BigDecimal("40.0"));

        mockMvc.perform(put("/api/v1/instructors/{id}", UUID.randomUUID())
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(put)))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchAppliesOnlySentFields() throws Exception {
        String id = createInstructor(doc());
        Map<String, Object> patch = Map.of("phone", "3111111111", "first_name", "Mariana");

        mockMvc.perform(patch("/api/v1/instructors/{id}", id)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("3111111111"))
                .andExpect(jsonPath("$.first_name").value("Mariana"))
                .andExpect(jsonPath("$.last_name").value("Torres"))
                .andExpect(jsonPath("$.full_name").value("Mariana Torres"))
                .andExpect(jsonPath("$.row_version").value(1));
    }

    @Test
    void patchNegativeHoursReturns422() throws Exception {
        String id = createInstructor(doc());

        mockMvc.perform(patch("/api/v1/instructors/{id}", id)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"default_max_hours_per_week\": -2.0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void staleRowVersionReturns409() throws Exception {
        String id = createInstructor(doc());

        mockMvc.perform(patch("/api/v1/instructors/{id}", id)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"3122222222\", \"row_version\": 99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void putRequiresManage() throws Exception {
        String id = createInstructor(doc());
        Map<String, Object> put = new LinkedHashMap<>();
        put.put("document_type", "CC");
        put.put("document_number", doc());
        put.put("first_name", "Ana");
        put.put("last_name", "Torres");
        put.put("email", "ana.torres@sena.edu.co");
        put.put("default_max_hours_per_week", new BigDecimal("40.0"));

        mockMvc.perform(put("/api/v1/instructors/{id}", id)
                        .with(viewJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(put)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    // ------------------------------------------------------------------ E4 deactivation

    @Test
    void deleteIsSoftNeverPhysical() throws Exception {
        String id = createInstructor(doc());

        mockMvc.perform(delete("/api/v1/instructors/{id}", id).with(manageJwt()))
                .andExpect(status().isNoContent());

        InstructorJpaEntity row = repository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(row.isActive()).isFalse();
        assertThat(row.getDeletedAt()).isNotNull();
        assertThat(row.getDeletedBy()).isNotNull();
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/instructors/{id}", UUID.randomUUID()).with(manageJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------ RBAC OWN_PROFILE

    @Test
    void ownProfileScopeOnlySeesOwnRow() throws Exception {
        String ownId = createInstructor(doc());
        String otherId = createInstructor(doc());

        mockMvc.perform(get("/api/v1/instructors/{id}", ownId).with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownId));

        mockMvc.perform(get("/api/v1/instructors/{id}", otherId).with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/instructors").with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ownId));
    }

    private String createContract(String instructorId, UUID centerId, boolean current) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contract_type", "STAFF");
        body.put("start_date", LocalDate.now().minusDays(10).toString());
        body.put("end_date", null);
        body.put("training_center_id", centerId.toString());
        body.put("weekly_hour_limit", new BigDecimal("40.0"));
        body.put("notes", "Contrato de centro");
        body.put("is_current", current);
        MvcResult result = mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void trainingCenterListsOnlyOwnCenter() throws Exception {
        UUID centerA = UUID.randomUUID();
        UUID centerB = UUID.randomUUID();
        String instructorA = createInstructor(doc());
        createContract(instructorA, centerA, true);
        String instructorB = createInstructor(doc());
        createContract(instructorB, centerB, true);
        createInstructor(doc());

        mockMvc.perform(get("/api/v1/instructors").with(trainingCenterViewJwt(centerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(instructorA));
    }

    @Test
    void trainingCenterGetOwnCenter200OtherCenter404() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorA = createInstructor(doc());
        createContract(instructorA, centerA, true);
        String instructorB = createInstructor(doc());
        createContract(instructorB, UUID.randomUUID(), true);

        mockMvc.perform(get("/api/v1/instructors/{id}", instructorA)
                        .with(trainingCenterViewJwt(centerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructorA));

        mockMvc.perform(get("/api/v1/instructors/{id}", instructorB)
                        .with(trainingCenterViewJwt(centerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainingCenterMismatchedQueryParam403() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorA = createInstructor(doc());
        createContract(instructorA, centerA, true);

        mockMvc.perform(get("/api/v1/instructors")
                        .queryParam("training_center_id", UUID.randomUUID().toString())
                        .with(trainingCenterViewJwt(centerA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCOPE_VIOLATION"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void trainingCenterWithoutClaim403() throws Exception {
        createInstructor(doc());

        mockMvc.perform(get("/api/v1/instructors").with(trainingCenterViewJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCOPE_VIOLATION"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void globalCanFilterByTrainingCenter() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorA = createInstructor(doc());
        createContract(instructorA, centerA, true);
        String instructorB = createInstructor(doc());
        createContract(instructorB, UUID.randomUUID(), true);

        mockMvc.perform(get("/api/v1/instructors")
                        .queryParam("training_center_id", centerA.toString())
                        .with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(instructorA));
    }

    @Test
    void areaFilterStillRejected() throws Exception {
        createInstructor(doc());

        mockMvc.perform(get("/api/v1/instructors")
                        .queryParam("area", "Sistemas")
                        .with(viewJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCOPE_VIOLATION"));
    }

    @Test
    void trainingCenterManageCanCreate() throws Exception {
        // Writes carry no row filter in HU-ACT-001, so CENTER_DIRECTOR
        // administrative creates stay allowed; scoping arrives with HU-ACT-002.
        mockMvc.perform(post("/api/v1/instructors")
                        .with(trainingCenterManageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createPayload(doc(), UUID.randomUUID(), new BigDecimal("40.0")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.full_name").value("Ana Torres"));
    }
}
