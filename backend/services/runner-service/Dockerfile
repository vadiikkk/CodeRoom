FROM gradle:8.7-jdk21 AS build
WORKDIR /workspace

COPY . .

RUN gradle :services:runner-service:test :services:runner-service:bootJar --no-daemon

FROM eclipse-temurin:21-jdk
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    golang-go \
    python3 \
    python3-pip \
    python3-venv \
    unzip \
    && python3 -m venv /opt/runner-venv \
    && /opt/runner-venv/bin/pip install --no-cache-dir pytest \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/opt/runner-venv/bin:${PATH}"

COPY --from=build /workspace/services/runner-service/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
