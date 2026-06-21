FROM eclipse-temurin:25-jdk
WORKDIR /app

# Resolve dependencies first; this layer is cached until the build scripts change.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies

# Build the application.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test
RUN mv build/libs/backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
