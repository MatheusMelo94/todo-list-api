# syntax=docker/dockerfile:1
#
# Dockerfile multi-stage para todo-list-api (Java 17 / Spring Boot 3.5.x).
# Per devops-conventions.md § Dockerfile Baseline:
#   - base eclipse-temurin 17 (jdk para build, jre para runtime)
#   - multi-stage (build compila; runtime minimo)
#   - usuario nao-root
#   - HEALTHCHECK no endpoint /actuator/health
#   - porta via env var, default 8080
#   - sem segredos embutidos (tudo via env em runtime)
#
# Spring Boot fixado em 3.5.x (nao 4.x) por docs/adr/0002 — base image 17 OK.

# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# Camada de dependencias: copia wrapper + pom primeiro para cache eficiente.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Codigo-fonte e empacotamento. Testes rodam no CI (ci.yml), nao na imagem
# (-DskipTests mantem o build da imagem rapido e deterministico). A gate de
# testes e do pipeline, per devops-conventions.md § CI Pipeline Shape.
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# wget para o HEALTHCHECK (presente na base alpine; garantido aqui).
RUN apk add --no-cache wget \
    && addgroup -S app && adduser -S app -G app

# Copia apenas o jar do estagio de build.
COPY --from=build /workspace/target/*.jar app.jar

# Profile prod ativo por default (Render sobrescreve via SPRING_PROFILES_ACTIVE
# no render.yaml). PORT default 8080 — Render injeta o valor real em runtime.
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080

USER app
EXPOSE 8080

# Health check no endpoint do Actuator (exposto so 'health' no application.yml).
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider "http://localhost:${PORT}/actuator/health" || exit 1

# JAVA_OPTS opcional (heap em planos free e limitado); shell form para expandir
# as env vars. server.port=${PORT:8080} ja vem do application.yml (profile prod).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
