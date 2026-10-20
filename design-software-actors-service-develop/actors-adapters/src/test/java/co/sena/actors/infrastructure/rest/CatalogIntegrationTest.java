package co.sena.actors.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CatalogIntegrationTest.TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled
class CatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "SCOPE_REF_CATALOG_VIEW")
    void shouldReturnCatalogsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/catalogs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_REF_CATALOG_VIEW")
    void shouldReturnNotFoundWhenCatalogDetailsDoNotExist() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get("/api/v1/catalogs/" + nonExistentId + "/details")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

	@org.springframework.boot.SpringBootConfiguration
	@org.springframework.boot.autoconfigure.EnableAutoConfiguration
	@org.springframework.context.annotation.ComponentScan(basePackages = "co.sena.actors")
	static class TestConfig {}
}



