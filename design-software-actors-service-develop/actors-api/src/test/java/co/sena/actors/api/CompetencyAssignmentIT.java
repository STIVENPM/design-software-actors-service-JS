package co.sena.actors.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-ACT-003 ITs against a real Postgres (no persistence mocks).
 * Covers competency assignment (E1-E2). Every test is @Transactional
 * and rolls its rows back.
 */
@SpringBootTest(classes = ActorsApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null") // MockMvc/Jackson builders carry no null contract; editor-only hints, javac is clean
class CompetencyAssignmentIT extends ActorsApiPostgresIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void stubDecoder() {
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

    private JwtRequestPostProcessor centerViewJwt(UUID centerId) {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:TRAINING_CENTER"))
                .jwt(j -> j.subject(UUID.randomUUID().toString())
                        .claim("training_center_id", centerId.toString()));
    }

    private JwtRequestPostProcessor centerViewJwtNoClaim() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:TRAINING_CENTER"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
    }

    private JwtRequestPostProcessor centerManageJwt(UUID centerId) {
        return jwt()
                .authorities(
                        new SimpleGrantedAuthority("ACT_INSTRUCTOR_MANAGE:TRAINING_CENTER"),
                        new SimpleGrantedAuthority("ACT_INSTRUCTOR_VIEW:TRAINING_CENTER"))
                .jwt(j -> j.subject(UUID.randomUUID().toString())
                        .claim("training_center_id", centerId.toString()));
    }

    private String doc() {
        return "K" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    private String createInstructor() throws Exception {
        String documentNumber = doc();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", UUID.randomUUID().toString());
        body.put("document_type", "CC");
        body.put("document_number", documentNumber);
        body.put("first_name", "Camilo");
        body.put("last_name", "Rojas");
        body.put("email", "camilo." + documentNumber.toLowerCase() + "@sena.edu.co");
        body.put("phone", "3001234567");
        body.put("default_max_hours_per_week", new BigDecimal("40.0"));
        MvcResult result = mockMvc.perform(post("/api/v1/instructors")
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createCurrentContract(String instructorId, UUID centerId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contract_type", "STAFF");
        body.put("start_date", LocalDate.now().minusDays(10).toString());
        body.put("end_date", null);
        body.put("training_center_id", centerId.toString());
        body.put("weekly_hour_limit", new BigDecimal("40.0"));
        body.put("notes", "Contrato de centro");
        body.put("is_current", true);
        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> assignmentPayload(UUID competencyId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("competency_id", competencyId == null ? null : competencyId.toString());
        body.put("certified_at", LocalDate.now().minusDays(30).toString());
        body.put("certification_entity", "SENA");
        return body;
    }

    private String createAssignment(String instructorId, UUID competencyId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/instructors/{id}/competencies", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(competencyId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ------------------------------------------------------------------ E1 assign

    @Test
    void assignReturns201() throws Exception {
        String instructorId = createInstructor();
        UUID competencyId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/instructors/{id}/competencies", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(competencyId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructor_id").value(instructorId))
                .andExpect(jsonPath("$.competency_id").value(competencyId.toString()))
                .andExpect(jsonPath("$.certification_entity").value("SENA"))
                .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    void duplicateActiveReturns409() throws Exception {
        String instructorId = createInstructor();
        UUID competencyId = UUID.randomUUID();
        createAssignment(instructorId, competencyId);

        mockMvc.perform(post("/api/v1/instructors/{id}/competencies", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(competencyId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void missingCompetencyIdReturns400() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/competencies", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void assignWithUnknownInstructorReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/instructors/{id}/competencies", UUID.randomUUID())
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void viewOnlyCannotAssign403() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/competencies", instructorId)
                        .with(viewJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(UUID.randomUUID()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    // ------------------------------------------------------------------ E2 list/revoke

    @Test
    void listOnlyActive() throws Exception {
        String instructorId = createInstructor();
        createAssignment(instructorId, UUID.randomUUID());
        String revoked = createAssignment(instructorId, UUID.randomUUID());

        mockMvc.perform(delete("/api/v1/instructors/{id}/competencies/{assignmentId}",
                                instructorId, revoked)
                        .with(manageJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/instructors/{id}/competencies", instructorId).with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteTwiceReturns404() throws Exception {
        String instructorId = createInstructor();
        String assignmentId = createAssignment(instructorId, UUID.randomUUID());

        mockMvc.perform(delete("/api/v1/instructors/{id}/competencies/{assignmentId}",
                                instructorId, assignmentId)
                        .with(manageJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/instructors/{id}/competencies/{assignmentId}",
                                instructorId, assignmentId)
                        .with(manageJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void deleteAssignmentOfOtherInstructorReturns404() throws Exception {
        String instructorA = createInstructor();
        String instructorB = createInstructor();
        String assignmentId = createAssignment(instructorA, UUID.randomUUID());

        mockMvc.perform(delete("/api/v1/instructors/{id}/competencies/{assignmentId}",
                                instructorB, assignmentId)
                        .with(manageJwt()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ RBAC scopes

    @Test
    void ownProfileSeesOwnAssignments() throws Exception {
        String ownId = createInstructor();
        String assignmentId = createAssignment(ownId, UUID.randomUUID());

        mockMvc.perform(get("/api/v1/instructors/{id}/competencies", ownId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(assignmentId));
    }

    @Test
    void ownProfileCannotReadOtherAssignments() throws Exception {
        String ownId = createInstructor();
        String otherId = createInstructor();
        createAssignment(otherId, UUID.randomUUID());

        mockMvc.perform(get("/api/v1/instructors/{id}/competencies", otherId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainingCenterSeesOwnCenterAssignments() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorA = createInstructor();
        createCurrentContract(instructorA, centerA);
        String assignmentId = createAssignment(instructorA, UUID.randomUUID());
        String instructorB = createInstructor();
        createCurrentContract(instructorB, UUID.randomUUID());
        createAssignment(instructorB, UUID.randomUUID());

        mockMvc.perform(get("/api/v1/instructors/{id}/competencies", instructorA)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(assignmentId));

        mockMvc.perform(get("/api/v1/instructors/{id}/competencies", instructorB)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainingCenterWithoutClaimCannotReadAssignments() throws Exception {
        String instructorId = createInstructor();
        createAssignment(instructorId, UUID.randomUUID());

        mockMvc.perform(get("/api/v1/instructors/{id}/competencies", instructorId)
                        .with(centerViewJwtNoClaim()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCOPE_VIOLATION"));
    }

    @Test
    void trainingCenterCannotManageOutsideCenter() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorB = createInstructor();
        createCurrentContract(instructorB, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/instructors/{id}/competencies", instructorB)
                        .with(centerManageJwt(centerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentPayload(UUID.randomUUID()))))
                .andExpect(status().isNotFound());
    }
}
