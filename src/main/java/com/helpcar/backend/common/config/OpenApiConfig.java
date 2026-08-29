package com.helpcar.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Serves the generated OpenAPI document at {@code /v3/api-docs} and Swagger UI at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI helpcarOpenApi(@Value("${helpcar.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("HelpCAR API")
                        .version(version)
                        .description("Volunteer medical-transport matching service. "
                                + "See docs/ARCHITECTURE.md for the design this API implements.")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
