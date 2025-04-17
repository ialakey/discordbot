# Stage 1: Build the application
FROM gradle:7.6.0-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

COPY src/main/resources/audio/ audio/

ENTRYPOINT ["java", "-jar", "app.jar"]
