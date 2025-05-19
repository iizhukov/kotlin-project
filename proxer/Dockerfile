FROM gradle:8.14.0-jdk21 AS builder

COPY . /app

WORKDIR /app

RUN gradle clean shadowJar --no-daemon

FROM eclipse-temurin:21

WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar app.jar

COPY docker_config.json config.json

EXPOSE 8080

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
