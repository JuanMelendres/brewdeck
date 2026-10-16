# syntax=docker/dockerfile:1
# BrewDeck API image for hosted environments (ADR-017). Local development keeps running the API with
# ./mvnw and Postgres from docker-compose.yml.
#
#   docker build -t brewdeck-api brewdeck-api
#   docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=staging -e DB_URL=... -e DB_USER=... \
#     -e DB_PASSWORD=... -e BREWDECK_JWT_SECRET=... -e BREWDECK_MAIL_ENABLED=true \
#     -e SPRING_MAIL_HOST=... brewdeck-api

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# Tests and coverage run in api-ci before any deploy; the image build only packages.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests -Djacoco.skip=true package \
    && cp target/brewdeck-api-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system --gid 10001 brewdeck \
    && useradd --system --uid 10001 --gid brewdeck --no-create-home brewdeck

# Least-changing layers first, so a code-only change re-ships only the application layer.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER brewdeck

# Heap sized from the container limit (the API uses ~365 MiB after start under 512 MiB, see the
# free hosting spike). Exit on OOM so the platform restarts the container instead of limping on.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
