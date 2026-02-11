# ============================================================
# Stage 1: Build — Gradle wrapper build with Eclipse Temurin JDK 17
# ============================================================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and build config first (layer caching)
COPY gradle/wrapper/ gradle/wrapper/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle .
COPY settings.gradle .

# Ensure gradlew is executable
RUN chmod +x gradlew

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application (skip tests — tests run in CI)
RUN ./gradlew bootJar --no-daemon -x test

# ============================================================
# Stage 2: Run — Minimal JRE 17 runtime
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
