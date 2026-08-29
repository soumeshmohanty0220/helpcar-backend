package com.helpcar.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full context against a real PostGIS container.
 *
 * <p>A mocked datasource would prove nothing here: the schema depends on the PostGIS
 * extension and on GiST indexes over geography columns, so the migration has to run
 * against the real engine to be meaningful.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HelpcarBackendApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("application context loads and Flyway applies the initial schema")
    void contextLoads() {
        // Failure here means the context or the V1 migration is broken.
    }

    @Test
    @DisplayName("GET /api/v1/ping is public and reports the service as up")
    void pingIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("helpcar-backend"));
    }

    @Test
    @DisplayName("unknown endpoints are denied by default rather than exposed")
    void unknownEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }
}
