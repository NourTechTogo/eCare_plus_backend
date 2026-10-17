# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Support checkouts created on Windows as well as Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build --chown=app:app /workspace/target/ecare-plus-backend-*.jar /app/app.jar
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    SPRING_DOCKER_COMPOSE_ENABLED=false
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
