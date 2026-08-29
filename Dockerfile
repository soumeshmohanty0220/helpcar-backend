# Multi-stage build.
#
# Stage 1 resolves dependencies in its own layer so that source-only changes do not
# re-download the world. Stage 2 ships a JRE, not a JDK, and runs as a non-root user.

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Wrapper and build scripts first — this layer is cached until the build files change.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Never run the service as root.
RUN groupadd --system helpcar && useradd --system --gid helpcar --create-home helpcar
USER helpcar

COPY --from=build --chown=helpcar:helpcar /workspace/build/libs/*.jar app.jar

EXPOSE 8080

# Container-aware heap sizing; the JVM reads the cgroup limit rather than the host's RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
