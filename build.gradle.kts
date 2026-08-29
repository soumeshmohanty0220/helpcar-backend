plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.helpcar"
version = "0.1.0-SNAPSHOT"
description = "HelpCAR backend — volunteer medical-transport matching service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // --- Web / API ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation(libs.springdoc.openapi.webmvc)

    // --- Security ---
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- Persistence (PostgreSQL + PostGIS) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(libs.hibernate.spatial)
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- Realtime location index / pub-sub ---
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // --- Operations ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Keeps parameter names in the bytecode — required for clean JSON binding
    // and for Spring's parameter-name discovery without @Param/@RequestParam("x").
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
