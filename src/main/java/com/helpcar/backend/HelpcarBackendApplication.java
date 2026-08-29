package com.helpcar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the HelpCAR backend.
 *
 * <p>This service owns identity, helper availability, ride requests, matching and
 * live location for the HelpCAR platform. The Flutter client is a thin consumer of
 * the REST + WebSocket API exposed here and holds no business logic or secrets.
 *
 * @see <a href="../../../../../docs/ARCHITECTURE.md">docs/ARCHITECTURE.md</a>
 */
@SpringBootApplication
public class HelpcarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpcarBackendApplication.class, args);
    }
}
