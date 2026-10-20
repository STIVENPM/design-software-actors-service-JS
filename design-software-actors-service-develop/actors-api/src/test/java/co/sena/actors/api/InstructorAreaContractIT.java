package co.sena.actors.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * HU-ACT-002 ITs against a real Postgres (no persistence mocks).
 * Covers instructor areas (E1) and contracts (E2-E4 + RN-ACT-01).
 * Every test is @Transactional and rolls its rows back.
 */
@SpringBootTest(classes = ActorsApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null") // MockMvc/Jackson builders carry no null contract; editor-only hints, javac is clean
class InstructorAreaContractIT extends ActorsApiPostgresIT {

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
        return "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    private String areaName() {
        return "AREA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String createInstructor() throws Exception {
        String documentNumber = doc();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", UUID.randomUUID().toString());
        body.put("document_type", "CC");
        body.put("document_number", documentNumber);
        body.put("first_name", "Bruno");
        body.put("last_name", "Diaz");
        body.put("email", "bruno." + documentNumber.toLowerCase() + "@sena.edu.co");
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

    private Map<String, Object> areaPayload(String name, boolean primary) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("area_name", name);
        body.put("is_primary", primary);
        return body;
    }

    private String createArea(String instructorId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/instructors/{id}/areas", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaPayload(name, false))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private Map<String, Object> contractPayload(
            String type, LocalDate start, LocalDate end, BigDecimal hours, boolean current) {
        return contractPayload(type, start, end, hours, current, UUID.randomUUID());
    }

    private Map<String, Object> contractPayload(
            String type, LocalDate start, LocalDate end, BigDecimal hours, boolean current, UUID centerId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contract_type", type);
        body.put("start_date", start == null ? null : start.toString());
        body.put("end_date", end == null ? null : end.toString());
        body.put("training_center_id", centerId.toString());
        body.put("weekly_hour_limit", hours);
        body.put("notes", "Contrato de prueba");
        body.put("is_current", current);
        return body;
    }

    private String createContract(String instructorId, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ------------------------------------------------------------------ E1 areas

    @Test
    void createAreaReturns201() throws Exception {
        String instructorId = createInstructor();
        String name = areaName();

        mockMvc.perform(post("/api/v1/instructors/{id}/areas", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaPayload(name, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructor_id").value(instructorId))
                .andExpect(jsonPath("$.area_name").value(name))
                .andExpect(jsonPath("$.is_primary").value(true))
                .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    void duplicateAreaReturns409() throws Exception {
        String instructorId = createInstructor();
        String name = areaName();
        createArea(instructorId, name);

        mockMvc.perform(post("/api/v1/instructors/{id}/areas", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaPayload(name, false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void listAreasOnlyActive() throws Exception {
        String instructorId = createInstructor();
        createArea(instructorId, areaName());
        String deleted = createArea(instructorId, areaName());

        mockMvc.perform(delete("/api/v1/instructors/{id}/areas/{areaId}", instructorId, deleted)
                        .with(manageJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/instructors/{id}/areas", instructorId).with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteAreaTwiceReturns404() throws Exception {
        String instructorId = createInstructor();
        String areaId = createArea(instructorId, areaName());

        mockMvc.perform(delete("/api/v1/instructors/{id}/areas/{areaId}", instructorId, areaId)
                        .with(manageJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/instructors/{id}/areas/{areaId}", instructorId, areaId)
                        .with(manageJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void deleteAreaOfOtherInstructorReturns404() throws Exception {
        String instructorA = createInstructor();
        String instructorB = createInstructor();
        String areaId = createArea(instructorA, areaName());

        mockMvc.perform(delete("/api/v1/instructors/{id}/areas/{areaId}", instructorB, areaId)
                        .with(manageJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void createAreaWithUnknownInstructorReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/instructors/{id}/areas", UUID.randomUUID())
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaPayload(areaName(), false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void viewOnlyCannotCreateArea403() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/areas", instructorId)
                        .with(viewJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaPayload(areaName(), false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    // ------------------------------------------------------------------ E2 contracts

    @Test
    void createContractReturns201Current() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "STAFF", LocalDate.now().minusDays(10), null,
                                new BigDecimal("40.0"), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructor_id").value(instructorId))
                .andExpect(jsonPath("$.contract_type").value("STAFF"))
                .andExpect(jsonPath("$.is_current").value(true))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.row_version").value(0));
    }

    @Test
    void secondCurrentContractClosesPrevious() throws Exception {
        String instructorId = createInstructor();
        String first = createContract(instructorId, contractPayload(
                "STAFF", LocalDate.now().minusDays(30), null, new BigDecimal("40.0"), true));
        String second = createContract(instructorId, contractPayload(
                "CONTRACTOR", LocalDate.now(), null, new BigDecimal("48.0"), true));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, first)
                        .with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_current").value(false));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, second)
                        .with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_current").value(true));
    }

    @Test
    void listAndGetContract() throws Exception {
        String instructorId = createInstructor();
        String contractId = createContract(instructorId, contractPayload(
                "HOURLY", LocalDate.now().minusDays(5), null, new BigDecimal("60.0"), true));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts", instructorId).with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(contractId));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, contractId)
                        .with(viewJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId));
    }

    @Test
    void getContractOfOtherInstructorReturns404() throws Exception {
        String instructorA = createInstructor();
        String instructorB = createInstructor();
        String contractId = createContract(instructorA, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", instructorB, contractId)
                        .with(viewJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------ E3 update

    @Test
    void updateContractAppliesSentFields() throws Exception {
        String instructorId = createInstructor();
        String contractId = createContract(instructorId, contractPayload(
                "CONTRACTOR", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("weekly_hour_limit", new BigDecimal("48.0"));
        update.put("notes", "Ampliación de horas");

        mockMvc.perform(put("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, contractId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekly_hour_limit").value(48.0))
                .andExpect(jsonPath("$.notes").value("Ampliación de horas"))
                .andExpect(jsonPath("$.row_version").value(1));
    }

    @Test
    void updateEndDateBeforeStartReturns422() throws Exception {
        String instructorId = createInstructor();
        String contractId = createContract(instructorId, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("end_date", LocalDate.now().minusDays(10).toString());

        mockMvc.perform(put("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, contractId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void updateContractOfOtherInstructorReturns404() throws Exception {
        String instructorA = createInstructor();
        String instructorB = createInstructor();
        String contractId = createContract(instructorA, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("notes", "Intento cruzado");

        mockMvc.perform(put("/api/v1/instructors/{id}/contracts/{contractId}", instructorB, contractId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ E2/E4 validation

    @Test
    void createEndDateBeforeStartReturns422() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "STAFF", LocalDate.now(), LocalDate.now().minusDays(1),
                                new BigDecimal("40.0"), true))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void createNegativeHoursReturns422() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "STAFF", LocalDate.now().minusDays(5), null,
                                new BigDecimal("-1.0"), true))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void staffAbove40Returns422() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "STAFF", LocalDate.now().minusDays(5), null,
                                new BigDecimal("41.0"), false))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void contractorAbove48Returns422() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "CONTRACTOR", LocalDate.now().minusDays(5), null,
                                new BigDecimal("49.0"), false))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void hourlyWithoutFixedLimitIsAllowed() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "HOURLY", LocalDate.now().minusDays(5), null,
                                new BigDecimal("60.0"), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contract_type").value("HOURLY"));
    }

    @Test
    void updateNegativeHoursReturns422() throws Exception {
        String instructorId = createInstructor();
        String contractId = createContract(instructorId, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("weekly_hour_limit", new BigDecimal("-2.0"));

        mockMvc.perform(put("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, contractId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void updateCanCloseCurrentContract() throws Exception {
        String instructorId = createInstructor();
        String contractId = createContract(instructorId, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("end_date", LocalDate.now().toString());
        update.put("is_current", false);

        mockMvc.perform(put("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, contractId)
                        .with(manageJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_current").value(false))
                .andExpect(jsonPath("$.end_date").value(LocalDate.now().toString()));
    }

    @Test
    void deleteContractsNotExposed405() throws Exception {
        String instructorId = createInstructor();
        String contractId = createContract(instructorId, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        mockMvc.perform(delete("/api/v1/instructors/{id}/contracts/{contractId}", instructorId, contractId)
                        .with(manageJwt()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void viewOnlyCannotCreateContract403() throws Exception {
        String instructorId = createInstructor();

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorId)
                        .with(viewJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "STAFF", LocalDate.now().minusDays(5), null,
                                new BigDecimal("40.0"), false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    // ------------------------------------------------------------------ RBAC scopes on child resources

    @Test
    void ownProfileSeesOwnAreasAndContracts() throws Exception {
        String ownId = createInstructor();
        String areaId = createArea(ownId, areaName());
        UUID centerId = UUID.randomUUID();
        String contractId = createContract(ownId, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true, centerId));

        mockMvc.perform(get("/api/v1/instructors/{id}/areas", ownId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(areaId));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts", ownId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(contractId));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", ownId, contractId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId));
    }

    @Test
    void ownProfileCannotReadOtherAreasAndContracts() throws Exception {
        String ownId = createInstructor();
        String otherId = createInstructor();
        createArea(otherId, areaName());
        String contractId = createContract(otherId, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        mockMvc.perform(get("/api/v1/instructors/{id}/areas", otherId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts", otherId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", otherId, contractId)
                        .with(ownProfileJwt(UUID.fromString(ownId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainingCenterSeesOwnCenterAreasAndContracts() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorA = createInstructor();
        String areaId = createArea(instructorA, areaName());
        String contractId = createContract(instructorA, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true, centerA));
        String instructorB = createInstructor();
        createContract(instructorB, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        mockMvc.perform(get("/api/v1/instructors/{id}/areas", instructorA)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(areaId));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts", instructorA)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(contractId));

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts/{contractId}", instructorA, contractId)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId));

        mockMvc.perform(get("/api/v1/instructors/{id}/areas", instructorB)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/instructors/{id}/contracts", instructorB)
                        .with(centerViewJwt(centerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainingCenterWithoutClaimCannotReadAreas() throws Exception {
        String instructorId = createInstructor();
        createArea(instructorId, areaName());

        mockMvc.perform(get("/api/v1/instructors/{id}/areas", instructorId)
                        .with(centerViewJwtNoClaim()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCOPE_VIOLATION"));
    }

    @Test
    void trainingCenterCannotManageOutsideCenter() throws Exception {
        UUID centerA = UUID.randomUUID();
        String instructorB = createInstructor();
        createContract(instructorB, contractPayload(
                "STAFF", LocalDate.now().minusDays(5), null, new BigDecimal("40.0"), true));

        mockMvc.perform(post("/api/v1/instructors/{id}/areas", instructorB)
                        .with(centerManageJwt(centerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaPayload(areaName(), false))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/instructors/{id}/contracts", instructorB)
                        .with(centerManageJwt(centerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractPayload(
                                "STAFF", LocalDate.now().minusDays(5), null,
                                new BigDecimal("40.0"), false, centerA))))
                .andExpect(status().isNotFound());
    }
}
