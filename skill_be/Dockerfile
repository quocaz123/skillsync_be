# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper + pom trước để tận dụng Docker layer cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cache layer riêng)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source và build
COPY src src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Chạy với profile 'pro' — Koyeb sẽ inject env vars
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=pro", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
