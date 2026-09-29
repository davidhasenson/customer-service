# Multi-stage build for customer-service
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and pom.xml for dependency caching
COPY pom.xml ./
COPY mvnw ./
COPY .mvn/ .mvn/

# Download dependencies (cached layer)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src/

# Build application
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

# Copy JAR from builder
COPY --from=builder /build/target/customer-service-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD java -cp /app/app.jar org.springframework.boot.loader.PropertiesLauncher &>/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
