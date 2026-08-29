package com.helpcar.backend.common.web;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated liveness endpoint for smoke tests and deployment verification.
 *
 * <p>Deliberately separate from {@code /actuator/health}: this one answers "is the
 * application serving traffic", the actuator probe answers "are its dependencies
 * healthy". A load balancer wants the former, an operator wants the latter.
 */
@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final String service;
    private final String version;

    public PingController(
            @Value("${spring.application.name}") String service, @Value("${helpcar.version}") String version) {
        this.service = service;
        this.version = version;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", service, version, Instant.now());
    }

    public record PingResponse(String status, String service, String version, Instant timestamp) {}
}
