# syntax=docker/dockerfile:1

# =========================================================
# Build stage: compile with the project's Maven wrapper
# (Maven 3.9.16, Java 17 as set in pom.xml)
# =========================================================
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /build

# Download Maven and dependencies first, so they are cached
# while only source files change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/*.jar app.jar


# =========================================================
# Runtime stage: JRE only, non-root user
# =========================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring \
    && mkdir -p /app/data /app/uploads \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /build/app.jar app.jar

USER spring

# Render sets PORT at runtime; 8081 is used when running locally.
ENV PORT=8081 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8081

# Shell form so $PORT and $JAVA_OPTS are expanded; exec makes
# Java PID 1 so it receives Render's shutdown signal.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -Dserver.address=0.0.0.0 -jar /app/app.jar"]
