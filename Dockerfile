# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY . .

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=builder --chown=app:app /app/build/libs/*.jar app.jar

EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
