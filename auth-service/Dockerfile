# build environment (Gradle)
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# Source code copy & build
COPY . .
RUN gradle clean build -x test --no-daemon

# run environment 
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# non-root USER
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# jar file copy
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]