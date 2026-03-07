FROM gradle:8.7-jdk21 AS build
WORKDIR /workspace

COPY . .

RUN gradle :services:autograding-service:test :services:autograding-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/autograding-service/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
