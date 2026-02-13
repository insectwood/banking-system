# build environment (Gradle)
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# 의존성 캐싱
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# run environment 
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# non-root 유저
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# jar 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]