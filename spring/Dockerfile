# 1단계: 빌드 (Gradle 캐시를 활용하려고 의존성 먼저 받아둔다)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# 2단계: 실행 (JDK가 아닌 JRE만 담아 이미지 크기를 줄인다)
FROM eclipse-temurin:17-jre
WORKDIR /app

# root로 띄우지 않는다
RUN useradd -r -u 1001 dortfolio
USER dortfolio

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너 메모리 한도에 맞춰 힙이 잡히도록 한다
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
