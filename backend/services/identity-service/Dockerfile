FROM gradle:8.7-jdk21 AS build
WORKDIR /workspace

COPY . .

RUN gradle :services:identity-service:test :services:identity-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/identity-service/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
