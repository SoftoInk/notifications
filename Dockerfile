FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw ./
RUN chmod +x mvnw

# Download dependencies and build (skip tests — they run in CI)
RUN ./mvnw clean package -DskipTests -q

# -------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/notifications-1.0.0.jar ./notifications.jar
COPY --from=build /app/target/classes/ ./classes/

# Run the examples demo
CMD ["java", "-cp", "notifications.jar", "com.novacomp.notifications.examples.NotificationExamples"]
